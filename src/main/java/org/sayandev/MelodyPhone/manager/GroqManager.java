package org.sayandev.MelodyPhone.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroqManager implements Listener {

    private static final String APPS_DIR_NAME = "apps";
    private static final String GROQ_FILE_NAME = "groq.yml";
    private static final String CFG_KEY_API = "general.api-key";
    private static final String CFG_KEY_MODEL = "general.model";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";

    private final JavaPlugin plugin;
    private final OkHttpClient http;

    private String apiKey;
    private String model;

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Deque<String>> history = new ConcurrentHashMap<>();
    private static final int MAX_TURNS = 8;

    public GroqManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.http = new OkHttpClient();
        loadGroqConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadGroqConfig() {
        try {
            File appsDir = new File(plugin.getDataFolder(), APPS_DIR_NAME);
            if (!appsDir.exists()) {
                boolean ok = appsDir.mkdirs();
                if (!ok) plugin.getLogger().warning("Failed to create apps directory.");
            }

            File groqFile = new File(appsDir, GROQ_FILE_NAME);
            if (!groqFile.exists()) {
                try {
                    plugin.saveResource(APPS_DIR_NAME + "/" + GROQ_FILE_NAME, false);
                } catch (IllegalArgumentException ignored) {
                    writeDefaultGroqFile(groqFile);
                }
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(groqFile);
            String fileApiKey = cfg.getString(CFG_KEY_API, "");
            String fileModel  = cfg.getString(CFG_KEY_MODEL, DEFAULT_MODEL);

            this.apiKey = !blank(fileApiKey) ? fileApiKey : System.getenv("GROQ_API_KEY");
            this.model  = !blank(fileModel)  ? fileModel  : DEFAULT_MODEL;

            if (blank(this.apiKey)) {
                plugin.getLogger().warning("API key is missing. Set " + CFG_KEY_API + " in apps/groq.yml or env GROQ_API_KEY.");
            } else {
                plugin.getLogger().info("Loaded model: " + this.model);
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load groq.yml: " + ex.getMessage());
            if (blank(this.model)) this.model = DEFAULT_MODEL;
        }
    }

    private void writeDefaultGroqFile(File groqFile) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(groqFile), StandardCharsets.UTF_8)) {
            w.write("general:\n");
            w.write("  api-key: \"\"\n");
            w.write("  model: \"" + DEFAULT_MODEL + "\"\n");
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not create default groq.yml: " + ex.getMessage());
        }
    }

    public void startChat(Player p) {
        active.add(p.getUniqueId());
        history.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
        p.sendMessage(Component.text("Type 'exit' to leave AI chat.").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("Ask your question.").color(NamedTextColor.GREEN));
    }

    public void stopChat(Player p) {
        active.remove(p.getUniqueId());
        history.remove(p.getUniqueId());
        p.sendMessage(Component.text("You left AI chat.").color(NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (!active.contains(id)) return;

        e.setCancelled(true);

        String userMsg = e.getMessage().trim();
        if (userMsg.equalsIgnoreCase("exit")) {
            Bukkit.getScheduler().runTask(plugin, () -> stopChat(p));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () ->
                p.sendMessage(Component.text("You: ").color(NamedTextColor.GRAY)
                        .append(Component.text(userMsg).color(NamedTextColor.WHITE)))
        );

        Deque<String> turns = history.computeIfAbsent(id, k -> new ArrayDeque<>());
        turns.addLast("User: " + userMsg);
        while (turns.size() > MAX_TURNS) turns.removeFirst();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String answer = callGroq(turns);
            if (answer == null) answer = "(no response)";
            final String finalAnswer = answer;

            Bukkit.getScheduler().runTask(plugin, () -> {
                turns.addLast("Assistant: " + finalAnswer);
                while (turns.size() > MAX_TURNS) turns.removeFirst();

                for (String line : splitForMinecraft(finalAnswer, 250)) {
                    p.sendMessage(Component.text("AI: ").color(NamedTextColor.AQUA)
                            .append(Component.text(line).color(NamedTextColor.WHITE)));
                }
            });
        });
    }

    private String callGroq(Deque<String> turns) {
        if (blank(apiKey)) return "GROQ_API_KEY is not set or general.api-key is empty.";
        try {
            final String endpoint = "https://api.groq.com/openai/v1/chat/completions";

            JsonObject root = new JsonObject();
            root.addProperty("model", model);
            root.addProperty("temperature", 0.6);
            root.addProperty("max_tokens", 512);

            JsonArray messages = new JsonArray();

            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content",
                    "You are an AI assistant inside a Minecraft server. " +
                            "Respond concisely. Default to English unless the user clearly writes another language.");
            messages.add(sys);

            for (String t : turns) {
                String role = t.startsWith("Assistant:") ? "assistant" : "user";
                String content = t.substring(t.indexOf(':') + 1).trim();

                JsonObject msg = new JsonObject();
                msg.addProperty("role", role);
                msg.addProperty("content", content);
                messages.add(msg);
            }
            root.add("messages", messages);

            String body = new Gson().toJson(root);

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            Request req = new Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    return extractGroqText(resp.body().string());
                } else {
                    String bodyStr = (resp.body() != null) ? resp.body().string() : "";
                    return "API error (" + resp.code() + "): " + bodyStr;
                }
            }
        } catch (Exception ex) {
            return "Request failed: " + ex.getMessage();
        }
    }

    private static String extractGroqText(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) return null;
            JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (msg == null || !msg.has("content")) return null;
            return msg.get("content").getAsString();
        } catch (Exception e) {
            return "Failed to parse response: " + e.getMessage();
        }
    }

    private static List<String> splitForMinecraft(String text, int chunk) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + chunk, text.length());
            out.add(text.substring(i, end));
            i = end;
        }
        return out;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
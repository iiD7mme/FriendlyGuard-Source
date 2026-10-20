package com.friendlyguard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FriendlyGuardConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("friendlyguard.json").toFile();
    private static FriendlyGuardConfig INSTANCE;

    @Expose
    private boolean enabled = true;

    @Expose
    private boolean showWarningMessage = true;

    @Expose
    private boolean friendGlow = true;

    @Expose
    private String defaultGlowColor = "GREEN";

    @Expose
    private Map<String, String> playerGlowColors = new LinkedHashMap<>();

    // Map of Player Name -> Enabled (true = protected, false = disabled)
    @Expose
    private Map<String, Boolean> playerStates = new LinkedHashMap<>();

    // Legacy list migration support
    @Expose
    private List<String> protectedPlayers = new ArrayList<>();

    // In-memory fast case-insensitive lookup set of ONLY ENABLED players
    private final transient Set<String> activeProtectedSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public static FriendlyGuardConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static FriendlyGuardConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                FriendlyGuardConfig config = GSON.fromJson(reader, FriendlyGuardConfig.class);
                if (config != null) {
                    config.migrateAndSync();
                    return config;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FriendlyGuardConfig fresh = new FriendlyGuardConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            if (CONFIG_FILE.getParentFile() != null && !CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            syncLookup();
            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void migrateAndSync() {
        if (playerStates == null) {
            playerStates = new LinkedHashMap<>();
        }
        // Migrate from old list if present
        if (protectedPlayers != null && !protectedPlayers.isEmpty()) {
            for (String p : protectedPlayers) {
                if (p != null && !p.trim().isEmpty() && !playerStates.containsKey(p.trim())) {
                    playerStates.put(p.trim(), true);
                }
            }
            protectedPlayers.clear();
        }
        syncLookup();
    }

    public void syncLookup() {
        activeProtectedSet.clear();
        if (playerStates != null) {
            for (Map.Entry<String, Boolean> entry : playerStates.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().trim().isEmpty() && Boolean.TRUE.equals(entry.getValue())) {
                    activeProtectedSet.add(entry.getKey().trim());
                }
            }
        }
    }

    public boolean isProtectionEnabled() {
        return enabled;
    }

    public void setProtectionEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public void setProtectionEnabledNoSave(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowWarningMessage() {
        return showWarningMessage;
    }

    public void setShowWarningMessage(boolean showWarningMessage) {
        this.showWarningMessage = showWarningMessage;
        save();
    }

    public void setShowWarningMessageNoSave(boolean showWarningMessage) {
        this.showWarningMessage = showWarningMessage;
    }

    public boolean isProtected(String playerName) {
        if (!enabled || playerName == null) return false;
        return activeProtectedSet.contains(playerName.trim());
    }

    public boolean isPlayerEnabled(String name) {
        if (name == null || playerStates == null) return false;
        Boolean val = playerStates.get(name);
        if (val == null) {
            for (Map.Entry<String, Boolean> e : playerStates.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) return Boolean.TRUE.equals(e.getValue());
            }
            return false;
        }
        return val;
    }

    public void setPlayerEnabled(String name, boolean enabled) {
        setPlayerEnabledNoSave(name, enabled);
        save();
    }

    public void setPlayerEnabledNoSave(String name, boolean enabled) {
        if (name == null || playerStates == null) return;
        for (String key : new ArrayList<>(playerStates.keySet())) {
            if (key.equalsIgnoreCase(name)) {
                playerStates.put(key, enabled);
                syncLookup();
                return;
            }
        }
        playerStates.put(name.trim(), enabled);
        syncLookup();
    }

    public boolean addPlayer(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String clean = name.trim();
        for (String existing : playerStates.keySet()) {
            if (existing.equalsIgnoreCase(clean)) {
                return false;
            }
        }
        playerStates.put(clean, true);
        save();
        return true;
    }

    public boolean removePlayer(String name) {
        if (name == null || playerStates == null) return false;
        String toRemove = null;
        for (String existing : playerStates.keySet()) {
            if (existing.equalsIgnoreCase(name.trim())) {
                toRemove = existing;
                break;
            }
        }
        if (toRemove != null) {
            playerStates.remove(toRemove);
            save();
            return true;
        }
        return false;
    }

    public void enableAllPlayers() {
        if (playerStates != null) {
            for (String key : playerStates.keySet()) {
                playerStates.put(key, true);
            }
            save();
        }
    }

    public void disableAllPlayers() {
        if (playerStates != null) {
            for (String key : playerStates.keySet()) {
                playerStates.put(key, false);
            }
            save();
        }
    }

    public Map<String, Boolean> getPlayerStates() {
        return Collections.unmodifiableMap(playerStates);
    }

    public List<String> getPlayerNames() {
        return new ArrayList<>(playerStates.keySet());
    }

    public boolean isFriendGlowEnabled() {
        return friendGlow;
    }

    public void setFriendGlowEnabled(boolean friendGlow) {
        this.friendGlow = friendGlow;
        save();
    }

    public String getDefaultGlowColor() {
        return defaultGlowColor != null ? defaultGlowColor : "GREEN";
    }

    public void setDefaultGlowColor(String color) {
        this.defaultGlowColor = color;
        save();
    }

    public int getGlowColorValue() {
        return com.friendlyguard.glow.GlowColor.fromName(getDefaultGlowColor()).getColorValue();
    }

    public int getPlayerGlowColorValue(String playerName) {
        if (playerGlowColors != null && playerName != null && playerGlowColors.containsKey(playerName.trim())) {
            String colName = playerGlowColors.get(playerName.trim());
            return com.friendlyguard.glow.GlowColor.fromName(colName).getColorValue();
        }
        return getGlowColorValue();
    }

    public com.friendlyguard.glow.GlowColor getPlayerGlowColor(String playerName) {
        if (playerGlowColors != null && playerName != null && playerGlowColors.containsKey(playerName.trim())) {
            return com.friendlyguard.glow.GlowColor.fromName(playerGlowColors.get(playerName.trim()));
        }
        return com.friendlyguard.glow.GlowColor.fromName(getDefaultGlowColor());
    }

    public void cyclePlayerGlowColor(String playerName) {
        if (playerGlowColors == null) {
            playerGlowColors = new LinkedHashMap<>();
        }
        com.friendlyguard.glow.GlowColor current = getPlayerGlowColor(playerName);
        com.friendlyguard.glow.GlowColor next = current.next();
        playerGlowColors.put(playerName.trim(), next.name());
        save();
    }

    public void cycleDefaultGlowColor() {
        com.friendlyguard.glow.GlowColor current = com.friendlyguard.glow.GlowColor.fromName(getDefaultGlowColor());
        com.friendlyguard.glow.GlowColor next = current.next();
        setDefaultGlowColor(next.name());
    }
}

package com.friendlyguard.glow;

public enum GlowColor {
    GREEN("Green", 0x2ECC71),
    CYAN("Cyan", 0x00E5FF),
    BLUE("Blue", 0x3498DB),
    PURPLE("Purple", 0x9B59B6),
    PINK("Pink", 0xFF69B4),
    YELLOW("Yellow", 0xF1C40F),
    ORANGE("Orange", 0xE67E22),
    RED("Red", 0xE74C3C),
    WHITE("White", 0xFFFFFF);

    private final String displayName;
    private final int colorValue;

    GlowColor(String displayName, int colorValue) {
        this.displayName = displayName;
        this.colorValue = colorValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColorValue() {
        return colorValue;
    }

    public GlowColor next() {
        GlowColor[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    public static GlowColor fromName(String name) {
        if (name != null) {
            for (GlowColor c : values()) {
                if (c.name().equalsIgnoreCase(name) || c.displayName.equalsIgnoreCase(name)) {
                    return c;
                }
            }
        }
        return GREEN;
    }
}

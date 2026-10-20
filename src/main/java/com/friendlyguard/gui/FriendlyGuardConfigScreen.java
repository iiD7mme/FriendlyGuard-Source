package com.friendlyguard.gui;

import com.friendlyguard.FriendlyGuardClient;
import com.friendlyguard.config.FriendlyGuardConfig;
import com.friendlyguard.glow.GlowColor;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class FriendlyGuardConfigScreen extends Screen {
    private final Screen parent;
    private final FriendlyGuardConfig config;
    private TextFieldWidget nameField;
    private KeyBinding rebindingKey = null;
    private int scrollY = 0;

    // Suggestions & Autocomplete state
    private final List<String> suggestions = new ArrayList<>();
    private int selectedSuggestionIndex = 0;

    // Layout configuration
    private int panelWidth = 390;
    private int panelHeight = 330;
    private int panelX;
    private int panelY;

    public FriendlyGuardConfigScreen(Screen parent) {
        super(Text.literal("FriendlyGuard"));
        this.parent = parent;
        this.config = FriendlyGuardConfig.getInstance();
    }

    public static Screen create(Screen parent) {
        return new FriendlyGuardConfigScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.min(400, this.width - 20);
        this.panelHeight = Math.min(340, this.height - 20);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        // Player username text field
        int fieldY = this.panelY + 100;
        int fieldWidth = this.panelWidth - 84;
        String oldText = this.nameField != null ? this.nameField.getText() : "";
        this.nameField = new TextFieldWidget(this.textRenderer, this.panelX + 12, fieldY, fieldWidth, 20, Text.literal("Player Name"));
        this.nameField.setPlaceholder(Text.literal("Enter player username..."));
        this.nameField.setMaxLength(16);
        this.nameField.setText(oldText);
        this.nameField.setFocusUnlocked(true);
        this.nameField.setEditable(true);
        this.nameField.setChangedListener(text -> updateSuggestions());

        this.addSelectableChild(this.nameField);
        this.setInitialFocus(this.nameField);
        this.nameField.setFocused(true);

        updateSuggestions();
    }

    private void updateSuggestions() {
        this.suggestions.clear();
        this.selectedSuggestionIndex = 0;

        if (this.nameField == null) return;
        String input = this.nameField.getText().trim();
        if (input.isEmpty()) return;

        List<String> onlinePlayers = getOnlinePlayers();
        for (String player : onlinePlayers) {
            if (player.toLowerCase().startsWith(input.toLowerCase())) {
                this.suggestions.add(player);
            }
        }
        for (String player : onlinePlayers) {
            if (!this.suggestions.contains(player) && player.toLowerCase().contains(input.toLowerCase())) {
                this.suggestions.add(player);
            }
        }
    }

    private List<String> getOnlinePlayers() {
        List<String> list = new ArrayList<>();
        if (this.client != null) {
            ClientPlayNetworkHandler networkHandler = this.client.getNetworkHandler();
            if (networkHandler != null) {
                Collection<PlayerListEntry> entries = networkHandler.getPlayerList();
                if (entries != null) {
                    for (PlayerListEntry entry : entries) {
                        if (entry != null) {
                            GameProfile profile = entry.getProfile();
                            if (profile != null && profile.getName() != null && !profile.getName().trim().isEmpty()) {
                                String name = profile.getName().trim();
                                if (!list.contains(name)) {
                                    list.add(name);
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private void playClickSound() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void addCurrentPlayer() {
        if (this.nameField != null) {
            String name = this.nameField.getText().trim();
            if (!name.isEmpty()) {
                this.config.addPlayer(name);
                this.nameField.setText("");
                updateSuggestions();
                playClickSound();
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark blurred overlay background
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // Main modal container card
        context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xF518191D);
        context.drawBorder(this.panelX, this.panelY, this.panelWidth, this.panelHeight, 0xFF2D3037);

        // Top Header
        context.drawText(this.textRenderer, "FriendlyGuard", this.panelX + 14, this.panelY + 10, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "v1.0.0", this.panelX + 96, this.panelY + 11, 0xFF72767D, false);

        // Header Close Button [ X ]
        int closeBtnX = this.panelX + this.panelWidth - 24;
        int closeBtnY = this.panelY + 7;
        boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 16 && mouseY >= closeBtnY && mouseY <= closeBtnY + 16;
        context.fill(closeBtnX, closeBtnY, closeBtnX + 16, closeBtnY + 16, closeHovered ? 0xFFDA373C : 0xFF23252A);
        context.drawBorder(closeBtnX, closeBtnY, 16, 16, closeHovered ? 0xFFF04747 : 0xFF353840);
        int closeTextX = closeBtnX + (16 - this.textRenderer.getWidth("X")) / 2;
        context.drawText(this.textRenderer, "X", closeTextX, closeBtnY + 4, closeHovered ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        // Divider under header
        context.fill(this.panelX + 12, this.panelY + 28, this.panelX + this.panelWidth - 12, this.panelY + 29, 0xFF2B2E35);

        int halfWidth = (this.panelWidth - 30) / 2;
        int col2X = this.panelX + 18 + halfWidth;

        // Row 1: Master Protection & Warning Message (Y = panelY + 34)
        int row1Y = this.panelY + 33;
        boolean isProtOn = this.config.isProtectionEnabled();
        boolean protHovered = mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row1Y && mouseY <= row1Y + 18;
        int protBg = isProtOn ? (protHovered ? 0xFF27AE60 : 0xFF1E824C) : (protHovered ? 0xFF43474E : 0xFF2B2D33);
        context.fill(this.panelX + 12, row1Y, this.panelX + 12 + halfWidth, row1Y + 18, protBg);
        context.drawBorder(this.panelX + 12, row1Y, halfWidth, 18, isProtOn ? 0xFF2ECC71 : 0xFF3E424B);
        String protText = "Protection: " + (isProtOn ? "ON" : "OFF");
        int protTextX = this.panelX + 12 + (halfWidth - this.textRenderer.getWidth(protText)) / 2;
        context.drawText(this.textRenderer, protText, protTextX, row1Y + 5, 0xFFFFFFFF, false);

        boolean isWarnOn = this.config.isShowWarningMessage();
        boolean warnHovered = mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row1Y && mouseY <= row1Y + 18;
        int warnBg = isWarnOn ? (warnHovered ? 0xFF2980B9 : 0xFF1F618D) : (warnHovered ? 0xFF43474E : 0xFF2B2D33);
        context.fill(col2X, row1Y, col2X + halfWidth, row1Y + 18, warnBg);
        context.drawBorder(col2X, row1Y, halfWidth, 18, isWarnOn ? 0xFF3498DB : 0xFF3E424B);
        String warnText = "Warning Msg: " + (isWarnOn ? "ON" : "OFF");
        int warnTextX = col2X + (halfWidth - this.textRenderer.getWidth(warnText)) / 2;
        context.drawText(this.textRenderer, warnText, warnTextX, row1Y + 5, 0xFFFFFFFF, false);

        // Row 2: Friend Glow Toggle & Glow Color (Y = panelY + 55)
        int row2Y = this.panelY + 55;
        boolean isGlowOn = this.config.isFriendGlowEnabled();
        boolean glowHovered = mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row2Y && mouseY <= row2Y + 18;
        int glowBg = isGlowOn ? (glowHovered ? 0xFF27AE60 : 0xFF1E824C) : (glowHovered ? 0xFF43474E : 0xFF2B2D33);
        context.fill(this.panelX + 12, row2Y, this.panelX + 12 + halfWidth, row2Y + 18, glowBg);
        context.drawBorder(this.panelX + 12, row2Y, halfWidth, 18, isGlowOn ? 0xFF2ECC71 : 0xFF3E424B);
        String glowText = "Friend Glow: " + (isGlowOn ? "ON" : "OFF");
        int glowTextX = this.panelX + 12 + (halfWidth - this.textRenderer.getWidth(glowText)) / 2;
        context.drawText(this.textRenderer, glowText, glowTextX, row2Y + 5, 0xFFFFFFFF, false);

        GlowColor defaultColor = GlowColor.fromName(this.config.getDefaultGlowColor());
        boolean colorBtnHovered = mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row2Y && mouseY <= row2Y + 18;
        int colorBtnBg = colorBtnHovered ? 0xFF3B3E46 : 0xFF25272D;
        context.fill(col2X, row2Y, col2X + halfWidth, row2Y + 18, colorBtnBg);
        context.drawBorder(col2X, row2Y, halfWidth, 18, 0xFF373B43);
        // Small color swatch indicator inside button
        context.fill(col2X + 8, row2Y + 5, col2X + 16, row2Y + 13, 0xFF000000 | defaultColor.getColorValue());
        context.drawBorder(col2X + 8, row2Y + 5, 8, 8, 0xFFFFFFFF);
        String colLabel = "Glow: " + defaultColor.getDisplayName();
        int colTextX = col2X + 22;
        context.drawText(this.textRenderer, colLabel, colTextX, row2Y + 5, 0xFF000000 | defaultColor.getColorValue(), false);

        // Row 3: Interactive Keybinds (Y = panelY + 77)
        int row3Y = this.panelY + 77;
        KeyBinding menuKey = FriendlyGuardClient.getOpenMenuKeyBinding();
        KeyBinding toggleKey = FriendlyGuardClient.getToggleProtectionKeyBinding();

        boolean isRebindingMenu = this.rebindingKey == menuKey;
        boolean menuHovered = mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row3Y && mouseY <= row3Y + 18;
        int menuBg = isRebindingMenu ? 0xFFD35400 : (menuHovered ? 0xFF3B3E46 : 0xFF25272D);
        context.fill(this.panelX + 12, row3Y, this.panelX + 12 + halfWidth, row3Y + 18, menuBg);
        context.drawBorder(this.panelX + 12, row3Y, halfWidth, 18, isRebindingMenu ? 0xFFF39C12 : 0xFF373B43);
        String menuKeyName = isRebindingMenu ? "> ??? <" : menuKey.getBoundKeyLocalizedText().getString();
        String menuLabel = "Menu: [" + menuKeyName + "]";
        int menuTextX = this.panelX + 12 + (halfWidth - this.textRenderer.getWidth(menuLabel)) / 2;
        context.drawText(this.textRenderer, menuLabel, menuTextX, row3Y + 5, isRebindingMenu ? 0xFFF1C40F : 0xFFE0E0E0, false);

        boolean isRebindingToggle = this.rebindingKey == toggleKey;
        boolean toggleKeyHovered = mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row3Y && mouseY <= row3Y + 18;
        int toggleBg = isRebindingToggle ? 0xFFD35400 : (toggleKeyHovered ? 0xFF3B3E46 : 0xFF25272D);
        context.fill(col2X, row3Y, col2X + halfWidth, row3Y + 18, toggleBg);
        context.drawBorder(col2X, row3Y, halfWidth, 18, isRebindingToggle ? 0xFFF39C12 : 0xFF373B43);
        String toggleKeyName = isRebindingToggle ? "> ??? <" : toggleKey.getBoundKeyLocalizedText().getString();
        String toggleLabel = "Toggle: [" + toggleKeyName + "]";
        int toggleTextX = col2X + (halfWidth - this.textRenderer.getWidth(toggleLabel)) / 2;
        context.drawText(this.textRenderer, toggleLabel, toggleTextX, row3Y + 5, isRebindingToggle ? 0xFFF1C40F : 0xFFE0E0E0, false);

        // Row 4: Add Player Field & Button (Y = panelY + 100)
        this.nameField.render(context, mouseX, mouseY, delta);

        if (this.nameField.isFocused() && !this.suggestions.isEmpty()) {
            String typed = this.nameField.getText();
            int idx = Math.max(0, Math.min(this.selectedSuggestionIndex, this.suggestions.size() - 1));
            String topMatch = this.suggestions.get(idx);
            if (topMatch.toLowerCase().startsWith(typed.toLowerCase()) && topMatch.length() > typed.length()) {
                String ghost = topMatch.substring(typed.length());
                int typedWidth = this.textRenderer.getWidth(typed);
                int ghostX = this.nameField.getX() + 4 + typedWidth;
                int ghostY = this.nameField.getY() + (this.nameField.getHeight() - 8) / 2;
                context.drawText(this.textRenderer, ghost, ghostX, ghostY, 0x8872767D, false);
            }
        }

        int addBtnX = this.panelX + this.panelWidth - 66;
        int addBtnY = this.panelY + 100;
        boolean addHovered = mouseX >= addBtnX && mouseX <= addBtnX + 54 && mouseY >= addBtnY && mouseY <= addBtnY + 20;
        context.fill(addBtnX, addBtnY, addBtnX + 54, addBtnY + 20, addHovered ? 0xFF5865F2 : 0xFF4752C4);
        context.drawBorder(addBtnX, addBtnY, 54, 20, addHovered ? 0xFF7983F5 : 0xFF5865F2);
        int addTextX = addBtnX + (54 - this.textRenderer.getWidth("+ Add")) / 2;
        context.drawText(this.textRenderer, "+ Add", addTextX, addBtnY + 6, 0xFFFFFFFF, false);

        // Row 5: Player List Header & Batch Buttons (Y = panelY + 125)
        int row5Y = this.panelY + 125;
        context.drawText(this.textRenderer, "Protected Friends", this.panelX + 14, row5Y + 5, 0xFFFFFFFF, false);

        int enAllX = this.panelX + this.panelWidth - 146;
        boolean enAllHovered = mouseX >= enAllX && mouseX <= enAllX + 64 && mouseY >= row5Y && mouseY <= row5Y + 16;
        context.fill(enAllX, row5Y, enAllX + 64, row5Y + 16, enAllHovered ? 0xFF27AE60 : 0xFF202328);
        context.drawBorder(enAllX, row5Y, 64, 16, enAllHovered ? 0xFF2ECC71 : 0xFF353942);
        int enAllTextX = enAllX + (64 - this.textRenderer.getWidth("Enable All")) / 2;
        context.drawText(this.textRenderer, "Enable All", enAllTextX, row5Y + 4, enAllHovered ? 0xFFFFFFFF : 0xFFCCCCCC, false);

        int disAllX = this.panelX + this.panelWidth - 76;
        boolean disAllHovered = mouseX >= disAllX && mouseX <= disAllX + 64 && mouseY >= row5Y && mouseY <= row5Y + 16;
        context.fill(disAllX, row5Y, disAllX + 64, row5Y + 16, disAllHovered ? 0xFFC0392B : 0xFF202328);
        context.drawBorder(disAllX, row5Y, 64, 16, disAllHovered ? 0xFFE74C3C : 0xFF353942);
        int disAllTextX = disAllX + (64 - this.textRenderer.getWidth("Disable All")) / 2;
        context.drawText(this.textRenderer, "Disable All", disAllTextX, row5Y + 4, disAllHovered ? 0xFFFFFFFF : 0xFFCCCCCC, false);

        // Row 6: Scrollable Player List
        int listLeft = this.panelX + 12;
        int listRight = this.panelX + this.panelWidth - 12;
        int listTop = this.panelY + 145;
        int listBottom = this.panelY + this.panelHeight - 26;
        int listHeight = listBottom - listTop;
        int itemHeight = 24;

        context.fill(listLeft, listTop, listRight, listBottom, 0x55111215);
        context.drawBorder(listLeft, listTop, listRight - listLeft, listHeight, 0xFF2B2E35);

        Map<String, Boolean> states = this.config.getPlayerStates();
        List<String> playerList = new ArrayList<>(states.keySet());

        Set<String> onlinePlayersSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        onlinePlayersSet.addAll(getOnlinePlayers());

        int totalContentHeight = playerList.size() * itemHeight;
        int maxScroll = Math.max(0, totalContentHeight - listHeight + 6);
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScroll));

        context.enableScissor(listLeft + 1, listTop + 1, listRight - 1, listBottom - 1);

        if (playerList.isEmpty()) {
            String emptyMsg = "No players added. Enter a name above to add.";
            int emptyX = listLeft + ((listRight - listLeft) - this.textRenderer.getWidth(emptyMsg)) / 2;
            context.drawText(this.textRenderer, emptyMsg, emptyX, listTop + (listHeight / 2) - 4, 0xFF72767D, false);
        } else {
            for (int i = 0; i < playerList.size(); i++) {
                String playerName = playerList.get(i);
                boolean isEnabled = Boolean.TRUE.equals(states.get(playerName));
                boolean isOnline = onlinePlayersSet.contains(playerName);
                GlowColor playerColor = this.config.getPlayerGlowColor(playerName);
                int itemY = listTop + 3 - this.scrollY + (i * itemHeight);

                if (itemY + itemHeight < listTop || itemY > listBottom) {
                    continue;
                }

                int rowRight = (totalContentHeight > listHeight) ? listRight - 8 : listRight;
                boolean rowHovered = mouseX >= listLeft + 2 && mouseX <= rowRight - 2 && mouseY >= itemY && mouseY < itemY + itemHeight - 2;

                context.fill(listLeft + 2, itemY, rowRight - 2, itemY + itemHeight - 2, rowHovered ? 0xFF282B32 : 0xFF1E2025);
                context.drawBorder(listLeft + 2, itemY, (rowRight - 2) - (listLeft + 2), itemHeight - 2, 0xFF2D3038);

                // Online indicator: Solid green if Online, dim hollow/transparent if Offline (مطفي)
                if (isOnline) {
                    context.fill(listLeft + 8, itemY + 8, listLeft + 14, itemY + 14, 0xFF2ECC71);
                } else {
                    context.fill(listLeft + 8, itemY + 8, listLeft + 14, itemY + 14, 0x25FFFFFF);
                    context.drawBorder(listLeft + 8, itemY + 8, 6, 6, 0x55999999);
                }

                // Player name
                int nameColor = isEnabled ? (isOnline ? 0xFFFFFFFF : 0xFFCCCCCC) : 0xFF72767D;
                context.drawText(this.textRenderer, playerName, listLeft + 20, itemY + 6, nameColor, false);

                // Glow Color Pill / Button (cycles color for this specific player)
                int colorX = rowRight - 94;
                int colorY = itemY + 2;
                int colorW = 18;
                int colorH = 18;
                boolean cHovered = mouseX >= colorX && mouseX <= colorX + colorW && mouseY >= colorY && mouseY <= colorY + colorH;
                context.fill(colorX, colorY, colorX + colorW, colorY + colorH, cHovered ? 0xFF3E424B : 0xFF282A30);
                context.drawBorder(colorX, colorY, colorW, colorH, cHovered ? 0xFFFFFFFF : 0xFF3E424B);
                context.fill(colorX + 4, colorY + 4, colorX + 14, colorY + 14, 0xFF000000 | playerColor.getColorValue());

                // Toggle switch button: [ ON ] / [ OFF ]
                int toggleX = rowRight - 70;
                int toggleY = itemY + 2;
                int toggleW = 42;
                int toggleH = 18;
                boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH;
                int tBg = isEnabled ? (toggleHover ? 0xFF27AE60 : 0xFF1E824C) : (toggleHover ? 0xFF4A4E57 : 0xFF2C2F36);
                context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, tBg);
                context.drawBorder(toggleX, toggleY, toggleW, toggleH, isEnabled ? 0xFF2ECC71 : 0xFF4A4E57);
                String tText = isEnabled ? "ON" : "OFF";
                int tTextX = toggleX + (toggleW - this.textRenderer.getWidth(tText)) / 2;
                context.drawText(this.textRenderer, tText, tTextX, toggleY + 5, 0xFFFFFFFF, false);

                // Delete button: [ X ]
                int delX = rowRight - 24;
                int delY = itemY + 2;
                int delW = 20;
                int delH = 18;
                boolean delHover = mouseX >= delX && mouseX <= delX + delW && mouseY >= delY && mouseY <= delY + delH;
                context.fill(delX, delY, delX + delW, delY + delH, delHover ? 0xFFC0392B : 0xFF282A30);
                context.drawBorder(delX, delY, delW, delH, delHover ? 0xFFE74C3C : 0xFF3B3E46);
                int dTextX = delX + (delW - this.textRenderer.getWidth("X")) / 2;
                context.drawText(this.textRenderer, "X", dTextX, delY + 5, delHover ? 0xFFFFFFFF : 0xFFE74C3C, false);
            }
        }

        if (totalContentHeight > listHeight) {
            int scrollBarX = listRight - 6;
            int barHeight = Math.max(16, (int) ((float) listHeight / totalContentHeight * listHeight));
            int barY = listTop + (int) ((float) this.scrollY / maxScroll * (listHeight - barHeight));
            context.fill(scrollBarX, listTop + 1, scrollBarX + 4, listBottom - 1, 0xFF18191D);
            context.fill(scrollBarX, barY, scrollBarX + 4, barY + barHeight, 0xFF5865F2);
        }

        context.disableScissor();

        // Footer Bar
        context.fill(this.panelX + 12, this.panelY + this.panelHeight - 22, this.panelX + this.panelWidth - 12, this.panelY + this.panelHeight - 21, 0xFF2B2E35);

        int activeCount = 0;
        for (Boolean b : states.values()) {
            if (Boolean.TRUE.equals(b)) activeCount++;
        }
        String countText = activeCount + " of " + playerList.size() + " active";
        context.drawText(this.textRenderer, countText, this.panelX + 14, this.panelY + this.panelHeight - 14, 0xFF72767D, false);

        String discordLabel = "Discord: discord.gg/i777";
        int discordX = this.panelX + this.panelWidth - 14 - this.textRenderer.getWidth(discordLabel);
        int discordY = this.panelY + this.panelHeight - 14;
        boolean discordHovered = mouseX >= discordX && mouseX <= discordX + this.textRenderer.getWidth(discordLabel) && mouseY >= discordY - 2 && mouseY <= discordY + 10;
        context.drawText(this.textRenderer, discordLabel, discordX, discordY, discordHovered ? 0xFF99AAB5 : 0xFF7289DA, false);

        // Suggestions Dropdown
        if (this.nameField.isFocused() && !this.suggestions.isEmpty()) {
            renderSuggestionsDropdown(context, mouseX, mouseY);
        }
    }

    private void renderSuggestionsDropdown(DrawContext context, int mouseX, int mouseY) {
        int dropX = this.nameField.getX();
        int dropY = this.nameField.getY() + this.nameField.getHeight() + 2;
        int dropW = this.nameField.getWidth();
        int itemH = 18;
        int count = Math.min(5, this.suggestions.size());
        int dropH = count * itemH + 4;

        context.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF8181A20);
        context.drawBorder(dropX, dropY, dropW, dropH, 0xFF5865F2);

        for (int i = 0; i < count; i++) {
            String sug = this.suggestions.get(i);
            int rowY = dropY + 2 + (i * itemH);
            boolean isSelected = (i == this.selectedSuggestionIndex);
            boolean isHovered = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= rowY && mouseY < rowY + itemH;

            if (isSelected || isHovered) {
                context.fill(dropX + 2, rowY, dropX + dropW - 2, rowY + itemH, 0xFF353942);
            }

            context.fill(dropX + 8, rowY + 6, dropX + 13, rowY + 11, 0xFF2ECC71);

            int textColor = isSelected ? 0xFFF1C40F : (isHovered ? 0xFFFFFFFF : 0xFFE0E0E0);
            context.drawText(this.textRenderer, sug, dropX + 20, rowY + 5, textColor, false);

            if (isSelected) {
                String tabHint = "[TAB to fill]";
                int hintX = dropX + dropW - 6 - this.textRenderer.getWidth(tabHint);
                context.drawText(this.textRenderer, tabHint, hintX, rowY + 5, 0xFF72767D, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Rebinding keybind click
        if (this.rebindingKey != null) {
            if (button > 1) {
                this.rebindingKey.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
                KeyBinding.updateKeysByCode();
                if (this.client != null && this.client.options != null) {
                    this.client.options.write();
                }
            }
            this.rebindingKey = null;
            playClickSound();
            return true;
        }

        // Suggestions Dropdown Click
        if (this.nameField != null && this.nameField.isFocused() && !this.suggestions.isEmpty()) {
            int dropX = this.nameField.getX();
            int dropY = this.nameField.getY() + this.nameField.getHeight() + 2;
            int dropW = this.nameField.getWidth();
            int itemH = 18;
            int count = Math.min(5, this.suggestions.size());

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + (count * itemH) + 4) {
                int clickedIndex = (int) ((mouseY - dropY - 2) / itemH);
                if (clickedIndex >= 0 && clickedIndex < count) {
                    String picked = this.suggestions.get(clickedIndex);
                    this.config.addPlayer(picked);
                    this.nameField.setText("");
                    updateSuggestions();
                    playClickSound();
                    return true;
                }
            }
        }

        // TextField focus and clicks
        if (this.nameField != null) {
            boolean clickedField = mouseX >= this.nameField.getX() && mouseX <= this.nameField.getX() + this.nameField.getWidth()
                    && mouseY >= this.nameField.getY() && mouseY <= this.nameField.getY() + this.nameField.getHeight();
            this.nameField.setFocused(clickedField);
            if (clickedField) {
                this.setFocused(this.nameField);
                this.nameField.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }

        // Header Close button [ X ]
        int closeBtnX = this.panelX + this.panelWidth - 24;
        int closeBtnY = this.panelY + 7;
        if (mouseX >= closeBtnX && mouseX <= closeBtnX + 16 && mouseY >= closeBtnY && mouseY <= closeBtnY + 16) {
            playClickSound();
            this.close();
            return true;
        }

        int halfWidth = (this.panelWidth - 30) / 2;
        int col2X = this.panelX + 18 + halfWidth;

        // Row 1: Master Protection & Warning Msg
        int row1Y = this.panelY + 33;
        if (mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row1Y && mouseY <= row1Y + 18) {
            this.config.setProtectionEnabled(!this.config.isProtectionEnabled());
            playClickSound();
            return true;
        }
        if (mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row1Y && mouseY <= row1Y + 18) {
            this.config.setShowWarningMessage(!this.config.isShowWarningMessage());
            playClickSound();
            return true;
        }

        // Row 2: Friend Glow & Glow Color
        int row2Y = this.panelY + 55;
        if (mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row2Y && mouseY <= row2Y + 18) {
            this.config.setFriendGlowEnabled(!this.config.isFriendGlowEnabled());
            playClickSound();
            return true;
        }
        if (mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row2Y && mouseY <= row2Y + 18) {
            this.config.cycleDefaultGlowColor();
            playClickSound();
            return true;
        }

        // Row 3: Keybinds
        int row3Y = this.panelY + 77;
        KeyBinding menuKey = FriendlyGuardClient.getOpenMenuKeyBinding();
        KeyBinding toggleKey = FriendlyGuardClient.getToggleProtectionKeyBinding();

        if (mouseX >= this.panelX + 12 && mouseX <= this.panelX + 12 + halfWidth && mouseY >= row3Y && mouseY <= row3Y + 18) {
            this.rebindingKey = (this.rebindingKey == menuKey) ? null : menuKey;
            playClickSound();
            return true;
        }
        if (mouseX >= col2X && mouseX <= col2X + halfWidth && mouseY >= row3Y && mouseY <= row3Y + 18) {
            this.rebindingKey = (this.rebindingKey == toggleKey) ? null : toggleKey;
            playClickSound();
            return true;
        }

        // Row 4: Add Player Button
        int addBtnX = this.panelX + this.panelWidth - 66;
        int addBtnY = this.panelY + 100;
        if (mouseX >= addBtnX && mouseX <= addBtnX + 54 && mouseY >= addBtnY && mouseY <= addBtnY + 20) {
            addCurrentPlayer();
            return true;
        }

        // Row 5: Batch Buttons
        int row5Y = this.panelY + 125;
        int enAllX = this.panelX + this.panelWidth - 146;
        if (mouseX >= enAllX && mouseX <= enAllX + 64 && mouseY >= row5Y && mouseY <= row5Y + 16) {
            this.config.enableAllPlayers();
            playClickSound();
            return true;
        }
        int disAllX = this.panelX + this.panelWidth - 76;
        if (mouseX >= disAllX && mouseX <= disAllX + 64 && mouseY >= row5Y && mouseY <= row5Y + 16) {
            this.config.disableAllPlayers();
            playClickSound();
            return true;
        }

        // Discord link
        String discordLabel = "Discord: discord.gg/i777";
        int discordX = this.panelX + this.panelWidth - 14 - this.textRenderer.getWidth(discordLabel);
        int discordY = this.panelY + this.panelHeight - 14;
        if (mouseX >= discordX && mouseX <= discordX + this.textRenderer.getWidth(discordLabel) && mouseY >= discordY - 2 && mouseY <= discordY + 10) {
            Util.getOperatingSystem().open("https://discord.gg/i777");
            playClickSound();
            return true;
        }

        // Row 6: List Items Interaction
        int listLeft = this.panelX + 12;
        int listRight = this.panelX + this.panelWidth - 12;
        int listTop = this.panelY + 145;
        int listBottom = this.panelY + this.panelHeight - 26;
        int itemHeight = 24;

        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            Map<String, Boolean> states = this.config.getPlayerStates();
            List<String> playerList = new ArrayList<>(states.keySet());
            int totalContentHeight = playerList.size() * itemHeight;
            int rowRight = (totalContentHeight > (listBottom - listTop)) ? listRight - 8 : listRight;

            for (int i = 0; i < playerList.size(); i++) {
                String playerName = playerList.get(i);
                int itemY = listTop + 3 - this.scrollY + (i * itemHeight);

                if (mouseY >= itemY && mouseY < itemY + itemHeight - 2) {
                    // Check Color Box Click (Cycles that player's specific glow color)
                    int colorX = rowRight - 94;
                    int colorW = 18;
                    if (mouseX >= colorX && mouseX <= colorX + colorW) {
                        this.config.cyclePlayerGlowColor(playerName);
                        playClickSound();
                        return true;
                    }

                    // Check Toggle Button [ ON / OFF ]
                    int toggleX = rowRight - 70;
                    int toggleW = 42;
                    if (mouseX >= toggleX && mouseX <= toggleX + toggleW) {
                        this.config.setPlayerEnabled(playerName, !this.config.isPlayerEnabled(playerName));
                        playClickSound();
                        return true;
                    }

                    // Check Delete Button [ X ]
                    int delX = rowRight - 24;
                    int delW = 20;
                    if (mouseX >= delX && mouseX <= delX + delW) {
                        this.config.removePlayer(playerName);
                        playClickSound();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listLeft = this.panelX + 12;
        int listRight = this.panelX + this.panelWidth - 12;
        int listTop = this.panelY + 145;
        int listBottom = this.panelY + this.panelHeight - 26;
        int listHeight = listBottom - listTop;

        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            int totalHeight = this.config.getPlayerStates().size() * 24;
            int maxScroll = Math.max(0, totalHeight - listHeight + 6);
            this.scrollY = Math.max(0, Math.min(this.scrollY - (int) (verticalAmount * 18), maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.rebindingKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.rebindingKey.setBoundKey(InputUtil.UNKNOWN_KEY);
            } else {
                this.rebindingKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            KeyBinding.updateKeysByCode();
            if (this.client != null && this.client.options != null) {
                this.client.options.write();
            }
            this.rebindingKey = null;
            playClickSound();
            return true;
        }

        if (this.nameField != null && this.nameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (!this.suggestions.isEmpty()) {
                    if (this.selectedSuggestionIndex < 0 || this.selectedSuggestionIndex >= this.suggestions.size()) {
                        this.selectedSuggestionIndex = 0;
                    }
                    String picked = this.suggestions.get(this.selectedSuggestionIndex);
                    this.nameField.setText(picked);
                    this.nameField.setCursor(picked.length(), false);
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.suggestions.size();
                    playClickSound();
                    return true;
                }
            }

            if (!this.suggestions.isEmpty()) {
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.suggestions.size();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex - 1 + this.suggestions.size()) % this.suggestions.size();
                    return true;
                }
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String toAdd = this.nameField.getText().trim();
                if (toAdd.isEmpty() && !this.suggestions.isEmpty()) {
                    toAdd = this.suggestions.get(Math.max(0, Math.min(this.selectedSuggestionIndex, this.suggestions.size() - 1)));
                }
                if (!toAdd.isEmpty()) {
                    this.config.addPlayer(toAdd);
                    this.nameField.setText("");
                    updateSuggestions();
                    playClickSound();
                    return true;
                }
            }

            if (this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.nameField != null && this.nameField.isFocused()) {
            if (this.nameField.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

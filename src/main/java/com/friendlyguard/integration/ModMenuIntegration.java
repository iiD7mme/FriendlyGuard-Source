package com.friendlyguard.integration;

import com.friendlyguard.gui.FriendlyGuardConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FriendlyGuardConfigScreen::create;
    }
}

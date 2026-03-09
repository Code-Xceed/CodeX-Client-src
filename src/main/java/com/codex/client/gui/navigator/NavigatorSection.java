package com.codex.client.gui.navigator;

public enum NavigatorSection {
    MODULES("Modules"),
    CLIENT_SETTINGS("Client Settings"),
    HUD_EDITOR("HUD Editor");

    private final String title;

    NavigatorSection(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

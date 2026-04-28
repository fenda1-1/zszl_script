package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client;

public final class ClientCommandHandler {

    public static final ClientCommandHandler instance = new ClientCommandHandler();

    private ClientCommandHandler() {
    }

    public void registerCommand(Object command) {
    }

    public int executeCommand(Object source, String command) {
        return 0;
    }
}



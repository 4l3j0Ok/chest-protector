package com.chestprotector.backport.client;

import com.chestprotector.backport.network.PinPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ChestProtectorBackportClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PinPayloads.OpenSetup.ID, (payload, context) -> context.client().execute(() -> context.client().setScreen(new PinScreen(payload.pos(), true))));
        ClientPlayNetworking.registerGlobalReceiver(PinPayloads.OpenUnlock.ID, (payload, context) -> context.client().execute(() -> context.client().setScreen(new PinScreen(payload.pos(), false))));
    }
}

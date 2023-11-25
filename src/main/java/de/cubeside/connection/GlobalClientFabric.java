package de.cubeside.connection;

import de.cubeside.connection.event.GlobalDataCallback;
import de.cubeside.connection.event.GlobalPlayerDisconnectedCallback;
import de.cubeside.connection.event.GlobalPlayerJoinedCallback;
import de.cubeside.connection.event.GlobalServerConnectedCallback;
import de.cubeside.connection.event.GlobalServerDisconnectedCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayDeque;

public class GlobalClientFabric extends GlobalClient {
    // private final GlobalClientMod plugin;
    private boolean stoppingServer;

    protected final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    protected final Object sync = new Object();
    protected boolean running = true;
    private MinecraftClient minecraftClient;

    private static ConnectionAPI instance;

    public GlobalClientFabric(GlobalConnectionFabricClient connectionPlugin, MinecraftClient minecraftClient) {
        super(null);

        // plugin = connectionPlugin;
        this.minecraftClient = minecraftClient;

        ClientPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ClientPlayConnectionEvents.DISCONNECT.register(this::onPlayerQuit);

        instance = this;
    }

    @Override
    public void setServer(String host, int port, String account, String password) {
        GlobalClientFabric.super.setServer(host, port, account, password);
        PlayerEntity player = minecraftClient.player;
        if (player != null) {
            onPlayerOnline(player.getUuid(), player.getName().getString(), System.currentTimeMillis());

        }
    }

    @Override
    protected void runInMainThread(Runnable r) {
        if (!stoppingServer) {
            minecraftClient.execute(r);
        }
    }

    @Override
    protected void processData(GlobalServer source, String channel, GlobalPlayer targetPlayer, GlobalServer targetServer, byte[] data) {
        // GlobalClientMod.LOGGER.debug("processData: " + channel);
        GlobalDataCallback.EVENT.invoker().onGlobalData(source, targetPlayer, channel, data);
    }

    public void onPlayerJoin(ClientPlayNetworkHandler clientPlayNetworkHandler, PacketSender packetSender, MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        GlobalPlayer existing = getPlayer(player.getUuid());
        if (existing == null || !existing.isOnServer(getThisServer())) {
            onPlayerOnline(player.getUuid(), player.getName().getString(), System.currentTimeMillis());
        }
    }

    public void onPlayerQuit(ClientPlayNetworkHandler clientPlayNetworkHandler, MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        GlobalPlayer existing = getPlayer(player.getUuid());
        if (existing != null && existing.isOnServer(getThisServer())) {
            onPlayerOffline(existing.getUniqueId());
        }
    }

    @Override
    protected void onPlayerJoined(GlobalServer server, GlobalPlayer player, boolean joinedTheNetwork) {
        // GlobalClientMod.LOGGER.debug("onPlayerJoined: " + player.getName());
        GlobalPlayerJoinedCallback.EVENT.invoker().onJoin(server, player, joinedTheNetwork);
    }

    @Override
    protected void onPlayerDisconnected(GlobalServer server, GlobalPlayer player, boolean leftTheNetwork) {
        // GlobalClientMod.LOGGER.debug("onPlayerDisconnected: " + player.getName());
        GlobalPlayerDisconnectedCallback.EVENT.invoker().onDisconnect(server, player, leftTheNetwork);
    }

    @Override
    protected void onServerConnected(GlobalServer server) {
        // GlobalClientMod.LOGGER.debug("onServerConnected: " + server.getName());
        GlobalServerConnectedCallback.EVENT.invoker().onConnect(server);
    }

    @Override
    protected void onServerDisconnected(GlobalServer server) {
        // GlobalClientMod.LOGGER.debug("onServerDisconnected: " + server.getName());
        GlobalServerDisconnectedCallback.EVENT.invoker().onDisconnect(server);
    }

    @Override
    public void shutdown() {
        this.stoppingServer = true;
        super.shutdown();
        synchronized (sync) {
            running = false;
            sync.notifyAll();
        }
    }

    public static ConnectionAPI getInstance() {
        return instance;
    }
}
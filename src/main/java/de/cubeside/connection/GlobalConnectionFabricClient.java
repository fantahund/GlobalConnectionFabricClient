package de.cubeside.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class GlobalConnectionFabricClient implements ClientModInitializer {
    public static final String MODID = "globalconnectionfabric";

    public static final Logger LOGGER = LogManager.getLogger();
    private GlobalClientFabric globalClient;
    private GlobalClientConfig config;

    private PlayerMessageImplementation messageAPI;

    private PlayerPropertiesImplementation propertiesAPI;

    private static GlobalConnectionFabricClient instance;

    public GlobalConnectionFabricClient() {
        instance = this;
    }

    @Override
    public void onInitializeClient() {
        FabricLoader.getInstance().getConfigDir().toFile().mkdirs();
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "globalclient.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = gson.fromJson(reader, GlobalClientConfig.class);
            } catch (Exception e) {
                LOGGER.error("Could not load GlobalClient config", e);
            }
        }
        boolean saveConfig = false;
        if (config == null) {
            config = new GlobalClientConfig();
            saveConfig = true;
        }
        if (config.initDefaultValues()) {
            saveConfig = true;
        }
        if (saveConfig) {
            try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(config, writer);
            } catch (Exception e) {
                LOGGER.error("Could not save GlobalClient config", e);
            }
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(this::onServerStarting);
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onServerStopping);
    }
    public void onServerStarting(MinecraftClient client) {
        globalClient = new GlobalClientFabric(this, client);
        globalClient.setServer(config.getHostname(), config.getPort(), config.getUser(), config.getPassword());

        messageAPI = new PlayerMessageImplementation(this, client);
        propertiesAPI = new PlayerPropertiesImplementation(this, client);
    }

    public void onServerStopping(MinecraftClient client) {
        if (globalClient != null) {
            globalClient.shutdown();
            globalClient = null;
        }
    }

    public GlobalClientFabric getConnectionAPI() {
        return globalClient;
    }

    public PlayerMessageImplementation getMessageAPI() {
        return messageAPI;
    }

    public PlayerPropertiesImplementation getPropertiesAPI() {
        return propertiesAPI;
    }

    public static GlobalConnectionFabricClient getInstance() {
        return instance;
    }
}

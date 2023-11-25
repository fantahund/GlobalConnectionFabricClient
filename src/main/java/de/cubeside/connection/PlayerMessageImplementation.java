package de.cubeside.connection;

import de.cubeside.connection.event.GlobalDataCallback;
import de.cubeside.connection.util.ConnectionStringUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class PlayerMessageImplementation implements PlayerMessageAPI {

    private final static int MESSAGE_CHAT = 1;
    private final static int MESSAGE_CHAT_COMPONENTS = 2;
    private final static int MESSAGE_ACTION_BAR = 3;
    private final static int MESSAGE_TITLE = 4;

    // private final GlobalClientMod plugin;
    private final MinecraftClient client;

    private final static String CHANNEL = "GlobalClient.chat";

    public PlayerMessageImplementation(GlobalConnectionFabricClient plugin, MinecraftClient client) {
        // this.plugin = plugin;
        this.client = client;
        GlobalDataCallback.EVENT.register(this::onGlobalData);
    }

    private void onGlobalData(GlobalServer source, GlobalPlayer target, String channel, byte[] data) {
        if (channel.equals(CHANNEL)) {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            try {
                if (target != null) {
                    PlayerEntity clientPlayer = client.player;
                    PlayerEntity player = clientPlayer != null ? target.getUniqueId().equals(clientPlayer.getUuid()) ? clientPlayer : null : null;
                    if (player != null) {
                        int type = dis.readByte();
                        if (type == MESSAGE_CHAT) {
                            String message = dis.readUTF();
                            player.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), false);
                        } else if (type == MESSAGE_CHAT_COMPONENTS) {
                            MutableText message = Text.Serializer.fromJson(dis.readUTF());
                            player.sendMessage(message, false);
                        } else if (type == MESSAGE_ACTION_BAR) {
                            String message = dis.readUTF();
                            player.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), true);
                        } else if (type == MESSAGE_TITLE) {
                            int flags = dis.readByte();
                            String title = ((flags & 1) != 0) ? dis.readUTF() : null;
                            String subtitle = ((flags & 2) != 0) ? dis.readUTF() : null;
                            int fadeInTicks = dis.readInt();
                            int durationTicks = dis.readInt();
                            int fadeOutTicks = dis.readInt();
                            // times, subtitle, title
                            sendTitleToPlayer(player, title, subtitle, fadeInTicks, durationTicks, fadeOutTicks);
                        }
                    }
                }
            } catch (IOException ex) {
                GlobalConnectionFabricClient.LOGGER.error("Could not parse PlayerMessage message", ex);
            }
        }
    }

    private void sendTitleToPlayer(PlayerEntity player, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks) {
        // times, subtitle, title
        client.inGameHud.setTitle(ConnectionStringUtil.parseLegacyColoredString(title));
        client.inGameHud.setSubtitle(ConnectionStringUtil.parseLegacyColoredString(subtitle));
        client.inGameHud.setTitleTicks(fadeInTicks, durationTicks, fadeOutTicks);
    }

    @Override
    public void sendMessage(GlobalPlayer player, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_CHAT);
            dos.writeUTF(message);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        PlayerEntity clientPlayer = client.player;
        PlayerEntity p = clientPlayer != null ? player.getUniqueId().equals(clientPlayer.getUuid()) ? clientPlayer : null : null;
        if (p != null) {
            p.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), false);
        }
    }

    @Override
    public void sendMessage(GlobalPlayer player, MutableText message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_CHAT_COMPONENTS);
            dos.writeUTF(Text.Serializer.toJson(message));
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        PlayerEntity clientPlayer = client.player;
        PlayerEntity p = clientPlayer != null ? player.getUniqueId().equals(clientPlayer.getUuid()) ? clientPlayer : null : null;
        if (p != null) {
            p.sendMessage(message, false);
        }
    }

    public static void main(String[] args) {
        System.out.println(Text.literal("§3hi!"));
    }

    @Override
    public void sendActionBarMessage(GlobalPlayer player, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_ACTION_BAR);
            dos.writeUTF(message);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        PlayerEntity clientPlayer = client.player;
        PlayerEntity p = clientPlayer != null ? player.getUniqueId().equals(clientPlayer.getUuid()) ? clientPlayer : null : null;
        if (p != null) {
            p.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), true);
        }
    }

    @Override
    public void sendTitleBarMessage(GlobalPlayer player, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_TITLE);
            int flags = (title != null ? 1 : 0) | (subtitle != null ? 2 : 0);
            dos.writeByte(flags);
            if (title != null) {
                dos.writeUTF(title);
            }
            if (subtitle != null) {
                dos.writeUTF(subtitle);
            }
            dos.writeInt(fadeInTicks);
            dos.writeInt(durationTicks);
            dos.writeInt(fadeOutTicks);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        PlayerEntity clientPlayer = client.player;
        PlayerEntity p = clientPlayer != null ? player.getUniqueId().equals(clientPlayer.getUuid()) ? clientPlayer : null : null;
        if (p != null) {
            sendTitleToPlayer(p, title, subtitle, fadeInTicks, durationTicks, fadeOutTicks);
        }
    }

}

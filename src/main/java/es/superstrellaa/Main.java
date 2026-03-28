package es.superstrellaa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSkinInitEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.block.Block;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.timer.TaskSchedule;

public class Main {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();

        instance.setChunkSupplier(LightingChunk::new);

        instance.setChunkLoader(new AnvilLoader("worlds/world"));

        instance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 38, Block.STONE);
            unit.modifier().fillHeight(38, 40, Block.DIRT);
            unit.modifier().fillHeight(40, 41, Block.GRASS_BLOCK);
        });

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            instance.saveChunksToStorage();
            System.out.println("[World] Mundo guardado.");
        }).repeat(TaskSchedule.minutes(5)).schedule();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[World] Guardando mundo antes de cerrar...");
            instance.saveChunksToStorage().join();
            System.out.println("[World] Guardado. Hasta luego!");
        }));

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(PlayerSkinInitEvent.class, event -> {
            PlayerSkin skin = PlayerSkin.fromUsername(event.getPlayer().getUsername());
            if (skin != null) event.setSkin(skin);
        });

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0.5, 41, 0.5));
            player.setGameMode(GameMode.SURVIVAL);
        });

        events.addListener(net.minestom.server.event.player.PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            Player player = event.getPlayer();
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p ->
                    p.sendMessage(Component.text("→ ", NamedTextColor.GREEN)
                            .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                            .append(Component.text(" se ha unido al servidor.", NamedTextColor.GREEN)))
            );
        });

        events.addListener(PlayerDisconnectEvent.class, event -> {
            String name = event.getPlayer().getUsername();
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p ->
                    p.sendMessage(Component.text("← ", NamedTextColor.RED)
                            .append(Component.text(name, NamedTextColor.YELLOW))
                            .append(Component.text(" ha salido del servidor.", NamedTextColor.RED)))
            );
        });

        events.addListener(PlayerChatEvent.class, event -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Component msg = Component.text("<", NamedTextColor.WHITE)
                    .append(Component.text(player.getUsername(), NamedTextColor.AQUA))
                    .append(Component.text("> ", NamedTextColor.WHITE))
                    .append(Component.text(event.getRawMessage()));
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(p -> p.sendMessage(msg));
            System.out.println("[Chat] <" + player.getUsername() + "> " + event.getRawMessage());
        });

        Command gamemodeCmd = new Command("gamemode", "gm");
        gamemodeCmd.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Solo jugadores.", NamedTextColor.RED));
                return;
            }
            String[] parts = context.getInput().split(" ");
            if (parts.length < 2) {
                player.sendMessage(Component.text("Uso: /gamemode <survival|creative|adventure|spectator>", NamedTextColor.RED));
                return;
            }
            GameMode mode = switch (parts[1].toLowerCase()) {
                case "survival", "s", "0"   -> GameMode.SURVIVAL;
                case "creative", "c", "1"   -> GameMode.CREATIVE;
                case "adventure", "a", "2"  -> GameMode.ADVENTURE;
                case "spectator", "sp", "3" -> GameMode.SPECTATOR;
                default -> null;
            };
            if (mode == null) {
                player.sendMessage(Component.text("Modo desconocido.", NamedTextColor.RED));
                return;
            }
            player.setGameMode(mode);
            player.sendMessage(Component.text("Gamemode cambiado a " + mode.name().toLowerCase() + ".", NamedTextColor.GREEN));
        });

        Command tpCmd = new Command("tp");
        tpCmd.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String[] parts = context.getInput().split(" ");
            if (parts.length < 4) {
                player.sendMessage(Component.text("Uso: /tp <x> <y> <z>", NamedTextColor.RED));
                return;
            }
            try {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                player.teleport(new Pos(x, y, z));
                player.sendMessage(Component.text("Teletransportado a " + x + ", " + y + ", " + z, NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Coordenadas inválidas.", NamedTextColor.RED));
            }
        });

        MinecraftServer.getCommandManager().register(gamemodeCmd);
        MinecraftServer.getCommandManager().register(tpCmd);

        System.out.println("Arrancando servidor...");
        server.start("0.0.0.0", 25565);
        System.out.println("Servidor listo en el puerto 25565");
    }
}
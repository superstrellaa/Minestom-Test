package es.superstrellaa;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.coordinate.Pos;

public class Main {
    public static void main(String[] args) {
        // Inicializar el servidor
        MinecraftServer server = MinecraftServer.init();

        // Crear la instancia (el "mundo")
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instance = instanceManager.createInstanceContainer();

        // Generador de chunks: plataforma plana de hierba hasta Y=40
        instance.setGenerator(unit ->
                unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
        );

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        // Cuando un jugador se conecta: asignarle instancia y spawn
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0, 42, 0));
            System.out.println("[+] " + player.getUsername() + " se está conectando...");
        });

        // Arrancar en el puerto 25565
        System.out.println("Arrancando servidor Minestom...");
        server.start("0.0.0.0", 25565);
        System.out.println("Servidor listo en el puerto 25565");
    }
}
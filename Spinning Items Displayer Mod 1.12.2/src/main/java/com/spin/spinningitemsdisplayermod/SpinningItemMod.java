package com.spin.spinningitemsdisplayermod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = SpinningItemMod.MODID, name = SpinningItemMod.NAME, version = SpinningItemMod.VERSION)
public class SpinningItemMod {
    public static final String MODID = "spinningitemsdisplayermod";
    public static final String NAME = "Spinning Items Displayer Mod";
    public static final String VERSION = "1.0";

    private static Logger logger;

    // --- MÉTODO AÑADIDO ---
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // El logger se inicializa en la fase de pre-inicialización
        logger = event.getModLog();
        logger.info("Pre-Initializing " + NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Ahora podemos usar el logger que ya fue inicializado
        logger.info("Initializing " + NAME);

        // Registramos nuestro manejador de eventos
        MinecraftForge.EVENT_BUS.register(new ItemDisplayHandler());
    }
}
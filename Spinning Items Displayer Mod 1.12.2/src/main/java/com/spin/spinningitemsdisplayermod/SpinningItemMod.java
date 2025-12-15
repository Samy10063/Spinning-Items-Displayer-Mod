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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Pre-Initializing " + NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing " + NAME);
        MinecraftForge.EVENT_BUS.register(new ItemDisplayHandler());
    }
}
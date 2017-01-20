package org.squiddev.cctweaks.runtimes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.squiddev.cctweaks.api.CCTweaksAPI;

import java.io.File;

@Mod(
	modid = RuntimesMod.ID,
	name = RuntimesMod.NAME,
	version = "${mod_version}",
	dependencies = "required-after:ComputerCraft@[${cc_version},);",
	acceptedMinecraftVersions = "[1.8.9,1.10.2]",
	guiFactory = "org.squiddev.cctweaks.runtimes.GuiConfigFactory"
)
public class RuntimesMod {
	public static final String ID = "cctweaks-runtimes";
	public static final String NAME = "CCTweaks Runtimes";

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		org.squiddev.cctweaks.runtimes.ConfigForgeLoader.init(new File(event.getModConfigurationDirectory(), RuntimesMod.ID + ".cfg"));
		Config.configuration = org.squiddev.cctweaks.runtimes.ConfigForgeLoader.getConfiguration();

		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		Runtimes.register(CCTweaksAPI.instance().luaEnvironment());
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.modID.equals(RuntimesMod.ID)) {
			org.squiddev.cctweaks.runtimes.ConfigForgeLoader.sync();
		}
	}
}

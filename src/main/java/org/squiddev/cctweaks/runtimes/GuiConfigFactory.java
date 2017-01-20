package org.squiddev.cctweaks.runtimes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GuiConfigFactory implements IModGuiFactory {
	@Override
	public void initialize(Minecraft minecraft) {
	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return GuiConfigCCTweaks.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@Override
	public IModGuiFactory.RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement runtimeOptionCategoryElement) {
		return null;
	}

	public static class GuiConfigCCTweaks extends GuiConfig {

		public GuiConfigCCTweaks(GuiScreen screen) {
			super(screen, getConfigElements(), RuntimesMod.ID, false, false, RuntimesMod.NAME);
		}

		@SuppressWarnings("rawtypes")
		private static List<IConfigElement> getConfigElements() {
			ArrayList<IConfigElement> elements = new ArrayList<IConfigElement>();
			for (String category : Config.configuration.getCategoryNames()) {
				if (!category.contains(".")) {
					elements.add(new ConfigElement(Config.configuration.getCategory(category)));
				}
			}
			return elements;
		}
	}
}

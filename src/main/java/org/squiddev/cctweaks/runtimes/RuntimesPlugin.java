package org.squiddev.cctweaks.runtimes;

import org.squiddev.cctweaks.api.lua.CCTweaksPlugin;
import org.squiddev.cctweaks.api.lua.ILuaEnvironment;
import org.squiddev.patcher.Logger;

public class RuntimesPlugin extends CCTweaksPlugin {
	@Override
	public void register(ILuaEnvironment environment) {
		org.squiddev.cctweaks.runtimes.ConfigPropertyLoader.init();
		Runtimes.register(environment);

		Logger.debug("[LuaJC] Using threshold=" + Config.LuaJC.threshold + ", verify=" + Config.LuaJC.verify);
	}
}

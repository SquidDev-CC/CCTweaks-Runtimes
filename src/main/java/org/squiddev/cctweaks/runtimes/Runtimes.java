package org.squiddev.cctweaks.runtimes;

import org.squiddev.cctweaks.api.lua.ILuaEnvironment;
import org.squiddev.cctweaks.runtimes.cobalt.CobaltLuaJCFactory;
import org.squiddev.cctweaks.runtimes.luaj.LuaLuaJCFactory;
import org.squiddev.cctweaks.runtimes.rembulan.RembulanFactory;

public final class Runtimes {
	private Runtimes() {
	}

	public static void register(ILuaEnvironment environment) {
		environment.registerMachine(new RembulanFactory());
		environment.registerMachine(new CobaltLuaJCFactory());
		environment.registerMachine(new LuaLuaJCFactory());
	}
}

package org.squiddev.cctweaks.runtimes.cobalt;

import dan200.computercraft.core.computer.Computer;
import org.squiddev.cctweaks.lua.lib.cobalt.CobaltFactory;
import org.squiddev.cctweaks.lua.lib.cobalt.CobaltMachine;

public class CobaltLuaJCFactory extends CobaltFactory {
	@Override
	public String getID() {
		return super.getID() + ".luajc";
	}

	@Override
	public CobaltMachine create(Computer computer) {
		CobaltMachine machine = super.create(computer);
		FallbackLuaJC.install(machine.getState());
		return machine;
	}
}

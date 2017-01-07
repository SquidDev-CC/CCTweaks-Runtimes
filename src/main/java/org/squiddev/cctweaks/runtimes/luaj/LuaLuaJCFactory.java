package org.squiddev.cctweaks.runtimes.luaj;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.lua.LuaJLuaMachine;
import org.squiddev.cctweaks.lua.lib.luaj.LuaJFactory;

public class LuaLuaJCFactory extends LuaJFactory {
	@Override
	public String getID() {
		return super.getID() + ".luajc";
	}

	@Override
	public LuaJLuaMachine create(Computer computer) {
		LuaJLuaMachine machine = super.create(computer);
		FallbackLuaJC.install();
		return machine;
	}
}

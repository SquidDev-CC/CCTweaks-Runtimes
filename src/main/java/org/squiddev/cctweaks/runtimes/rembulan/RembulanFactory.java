package org.squiddev.cctweaks.runtimes.rembulan;

import dan200.computercraft.core.computer.Computer;
import org.squiddev.cctweaks.api.lua.ILuaMachineFactory;

public class RembulanFactory implements ILuaMachineFactory<RembulanMachine> {
	@Override
	public String getID() {
		return "rembulan";
	}

	@Override
	public RembulanMachine create(Computer computer) {
		return new RembulanMachine(computer);
	}

	@Override
	public boolean supportsMultithreading() {
		return true;
	}

	@Override
	public String getPreBios() {
		return PRE_BIOS;
	}
}

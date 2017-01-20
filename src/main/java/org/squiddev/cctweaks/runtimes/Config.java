package org.squiddev.cctweaks.runtimes;

import net.minecraftforge.common.config.Configuration;
import org.squiddev.configgen.DefaultBoolean;
import org.squiddev.configgen.DefaultInt;
import org.squiddev.configgen.Range;
import org.squiddev.luaj.luajc.CompileOptions;

@org.squiddev.configgen.Config(languagePrefix = "gui.config.cctweaks-runtimes.", propertyPrefix = "cctweaks-runtimes")
public class Config {
	public static Configuration configuration;

	/**
	 * Compile Lua bytecode to Java bytecode.
	 * This speeds up code execution. Use the "cobalt.luajc" or "luaj.luajc" runtimes to enable this.
	 */
	public static class LuaJC {
		/**
		 * Verify sources on generation.
		 * This will slow down compilation.
		 * If you have errors, please turn this and debug on and
		 * send it with the bug report.
		 */
		@DefaultBoolean(false)
		public static boolean verify;

		/**
		 * Number of calls required before compiling:
		 * 1 compiles when first called,
		 * 0 or less compiles when loaded
		 */
		@DefaultInt(CompileOptions.THRESHOLD)
		@Range(min = 0)
		public static int threshold;
	}
}

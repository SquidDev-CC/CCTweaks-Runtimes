package org.squiddev.cctweaks.runtimes.cobalt;

import org.squiddev.cctweaks.runtimes.Config;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.luajc.CompileOptions;
import org.squiddev.cobalt.luajc.ErrorHandler;
import org.squiddev.cobalt.luajc.LuaJC;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.patcher.Logger;

/**
 * A version of LuaJC that falls back to normal Lua interpretation
 */
public final class FallbackLuaJC {
	public static void install(LuaState state) {
		LuaJC.install(state, new CompileOptions(
			CompileOptions.PREFIX,
			Config.LuaJC.threshold,
			Config.LuaJC.verify,
			handler
		));
	}

	private static final ErrorHandler handler = new ErrorHandler() {
		@Override
		public void handleError(ProtoInfo info, Throwable throwable) {
			Logger.error(
				"There was an error when compiling " + info.loader.filename + info.name + ". " +
					"(lines " + info.prototype.linedefined + "-" + info.prototype.lastlinedefined
					+ ", " + info.prototype.code.length + " instructions )\n" +
					"Please report this error message to http://github.com/SquidDev/luaj.luajc\n" +
					info.toString(),
				throwable
			);
		}
	};
}

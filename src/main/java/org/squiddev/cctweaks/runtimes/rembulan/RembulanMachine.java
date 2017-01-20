package org.squiddev.cctweaks.runtimes.rembulan;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.lua.ILuaMachine;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.sandius.rembulan.*;
import net.sandius.rembulan.compiler.CompilerChunkLoader;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.lib.*;
import net.sandius.rembulan.runtime.*;
import org.squiddev.cctweaks.api.lua.ArgumentDelegator;
import org.squiddev.cctweaks.api.lua.IMethodDescriptor;
import org.squiddev.cctweaks.lua.Config;
import org.squiddev.cctweaks.lua.Semaphore;
import org.squiddev.cctweaks.lua.StreamHelpers;
import org.squiddev.cctweaks.lua.ThreadBuilder;
import org.squiddev.cctweaks.lua.lib.AbstractLuaContext;
import org.squiddev.patcher.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.squiddev.cctweaks.lua.lib.LuaMachineHelpers.ILLEGAL_NAMES;
import static org.squiddev.cctweaks.lua.lib.LuaMachineHelpers.getHost;

public class RembulanMachine implements ILuaMachine {
	private static final ExecutorService threads = ThreadBuilder.createThread("Rembulan", 16, Integer.MAX_VALUE, ThreadBuilder.NORM_PRIORITY);

	private final Computer computer;
	private final ILuaContext noYieldContext;

	private final StateContext state;
	private final Table globals;
	private final DirectCallExecutor executor = DirectCallExecutor.newExecutor();

	private final CompilerChunkLoader loader;
	private String eventFilter = null;
	private String hardAbort = null;
	private String softAbort = null;
	private LuaFunction continuation;

	public RembulanMachine(Computer computer) {
		this.computer = computer;
		this.noYieldContext = new LuaContextNoYield(computer);

		StateContext state = this.state = StateContexts.newDefaultInstance();
		loader = CompilerChunkLoader.of("generated.computer_" + computer.getID() + "$x");

		Table globals = this.globals = state.newTable();
		BasicLib.installInto(state, globals, null, loader);
		CoroutineLib.installInto(state, globals);
		StringLib.installInto(state, globals);
		MathLib.installInto(state, globals);
		TableLib.installInto(state, globals);
		Utf8Lib.installInto(state, globals);

		if (Config.APIs.debug) DebugLib.installInto(state, globals);
		if (Config.APIs.bigInteger) BigIntegerValue.setup(state, globals);
		if (Config.APIs.bitop) BitOpLib.setup(state, globals);

		for (String global : ILLEGAL_NAMES) {
			globals.rawset(global, null);
		}

		String host = getHost(computer);
		if (host != null) globals.rawset("_HOST", host);

		globals.rawset("_CC_VERSION", ComputerCraft.getVersion());
		globals.rawset("_MC_VERSION", MinecraftForge.MC_VERSION);
		globals.rawset("_LUAJ_VERSION", "Rembulan 0.1");
		if (ComputerCraft.disable_lua51_features) {
			globals.rawset("_CC_DISABLE_LUA51_FEATURES", true);
		}
	}

	public void injectDebug() {
		DebugLib.installInto(state, globals);
	}

	@Override
	public void addAPI(ILuaAPI api) {
		Table table = wrapLuaObject(api);
		for (String name : api.getNames()) {
			globals.rawset(name, table);
		}
	}

	private Table wrapLuaObject(final ILuaObject object) {
		String[] methods = object.getMethodNames();
		Table result = state.newTable(0, methods.length);

		for (int i = 0; i < methods.length; i++) {
			result.rawset(methods[i], new WrappedLuaFunction(computer, object, i));
		}

		return result;
	}

	@Override
	public void loadBios(InputStream inputStream) {
		if (continuation != null) return;
		try {
			LuaFunction function = loader.loadTextChunk(new Variable(globals), "bios.lua", StreamHelpers.toString(inputStream));
			continuation = (LuaFunction) executor.call(state, CoroutineLib.wrap(), function)[0];
		} catch (Exception e) {
			continuation = null;
		}
	}

	@Override
	public void handleEvent(String eventName, Object[] arguments) {
		if (continuation == null) return;

		if (eventFilter == null || eventName == null || eventName.equals(eventFilter) || eventName.equals("terminate")) {
			try {
				Object[] args;
				if (eventName != null) {
					Object[] params = toValues(arguments);
					if (params.length == 0) {
						args = new Object[]{eventName};
					} else {
						args = new Object[params.length + 1];
						args[0] = eventName;
						System.arraycopy(params, 0, args, 1, params.length);
					}
				} else {
					args = new Object[0];
				}

				Object[] results;
				try {
					results = executor.call(state, continuation, args);
				} catch (InterruptedException e) {
					continuation = null;
					return;
				} catch (CallException e) {
					continuation = null;
					return;
				} catch (CallPausedException e) {
					continuation = null;
					return;
				}

				if (hardAbort != null) {
					continuation = null;
					return;
				}

				if (results != null && results.length > 0) {
					if (results[0] instanceof ByteString) {
						eventFilter = ((ByteString) results[0]).toRawString();
					} else if (results[0] instanceof String) {
						eventFilter = (String) results[0];
					} else {
						eventFilter = null;
					}
				} else {
					eventFilter = null;
				}
			} finally {
				softAbort = null;
				hardAbort = null;
			}
		}
	}

	@Override
	public void softAbort(String message) {
		softAbort = message;
	}

	@Override
	public void hardAbort(String message) {
		softAbort = message;
		hardAbort = message;
	}

	@Override
	public boolean saveState(OutputStream outputStream) {
		return false;
	}

	@Override
	public boolean restoreState(InputStream inputStream) {
		return false;
	}

	@Override
	public boolean isFinished() {
		return continuation == null;
	}

	@Override
	public void unload() {
	}

	private Object toValue(Object object, Map<Object, Table> tables) {
		if (object == null || object instanceof Number || object instanceof Boolean) {
			return object;
		} else if (object instanceof byte[]) {
			return ByteString.copyOf((byte[]) object);
		} else if (object instanceof String) {
			return ByteString.fromRaw((String) object);
		} else if (object instanceof Map) {
			if (tables == null) {
				tables = new IdentityHashMap<Object, Table>();
			} else {
				Table value = tables.get(object);
				if (value != null) return value;
			}

			Table table = state.newTable();
			tables.put(object, table);

			for (Map.Entry<?, ?> pair : ((Map<?, ?>) object).entrySet()) {
				Object key = toValue(pair.getKey(), tables);
				Object value = toValue(pair.getValue(), tables);
				if (key != null && value != null) {
					table.rawset(key, value);
				}
			}

			return table;
		} else if (object instanceof ILuaObject) {
			return wrapLuaObject((ILuaObject) object);
		} else {
			Logger.warn("Discarding " + object.getClass());
			return null;
		}
	}

	private Object[] toValues(Object[] objects) {
		if (objects != null && objects.length != 0) {
			Object[] values = new Object[objects.length];

			for (int i = 0; i < objects.length; ++i) {
				Object object = objects[i];
				values[i] = toValue(object, null);
			}

			return values;
		} else {
			return new Object[0];
		}
	}

	private final class WrappedLuaFunction extends AbstractFunctionAnyArg {
		private final Computer computer;
		private final ILuaObject object;
		private final int method;

		private final boolean noYield;

		private WrappedLuaFunction(Computer computer, ILuaObject object, int method) {
			this.computer = computer;
			this.object = object;
			this.method = method;

			this.noYield = object instanceof IMethodDescriptor && !((IMethodDescriptor) object).willYield(method);
		}

		@Override
		public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
			String message = softAbort;
			if (message != null) {
				softAbort = null;
				hardAbort = null;
				throw new LuaRuntimeException(message);
			}

			if (noYield) {
				try {
					Object[] returned = ArgumentDelegator.delegateLuaObject(object, noYieldContext, method, new RembulanArguments(args));
					context.getReturnBuffer().setToContentsOf(toValues(returned));
				} catch (LuaException e) {
					throw new LuaRuntimeException(e.getMessage());
				} catch (InterruptedException e) {
					throw new LuaRuntimeException(e);
				} catch (Throwable e) {
					throw new LuaRuntimeException("Java exception thrown " + e.getMessage());
				}
			} else {
				final LuaContext invoker = new LuaContext(computer, context.getCurrentCoroutine(), object, method, args);
				threads.execute(invoker);
				try {
					handleResult(context, invoker);
				} catch (InterruptedException e) {
					throw new LuaRuntimeException(e);
				}
			}
		}

		@Override
		public void resume(ExecutionContext context, Object state) throws ResolvedControlThrowable {
			if (noYield) throw new NonsuspendableFunctionException();

			LuaContext invoker = (LuaContext) state;
			try {
				invoker.resume(context.getReturnBuffer().getAsArray());
				handleResult(context, invoker);
			} catch (InterruptedException e) {
				throw new LuaRuntimeException(e);
			}
		}

		private void handleResult(ExecutionContext context, LuaContext invoker) throws ResolvedControlThrowable, InterruptedException {
			invoker.externalLock.await();

			if (invoker.isDone()) {
				try {
					context.getReturnBuffer().setToContentsOf(invoker.get());
				} catch (LuaException e) {
					throw new LuaRuntimeException(e.getMessage());
				} catch (InterruptedException e) {
					throw e;
				} catch (Throwable e) {
					Logger.error("Within handler", e);
					throw new LuaRuntimeException("Java exception thrown " + e.getMessage());
				}
			} else {
				try {
					context.yield(invoker.values);
				} catch (UnresolvedControlThrowable ct) {
					throw ct.resolve(this, invoker);
				}
			}
		}
	}

	private final class LuaContext extends AbstractLuaContext implements Runnable {
		private final Semaphore externalLock = new Semaphore();
		private final Semaphore yieldLock = new Semaphore();

		private final WeakReference<Coroutine> coroutine;

		private final ILuaObject object;
		private final int method;
		private volatile boolean done = false;
		private volatile Object[] values;
		private volatile Throwable exception;

		public LuaContext(Computer computer, Coroutine coroutine, ILuaObject object, int method, Object[] args) {
			super(computer);
			this.coroutine = new WeakReference<Coroutine>(coroutine);
			this.object = object;
			this.method = method;
			this.values = args;
		}

		@Override
		public Object[] yield(Object[] objects) throws InterruptedException {
			if (done) throw new IllegalStateException("Cannot yield when complete");

			values = toValues(objects);

			externalLock.signal();
			try {
				// Every 30 seconds check to see if the coroutine has been GCed
				// if so then abort this task.
				while (!yieldLock.await(30000)) {
					if (coroutine.get() == null) {
						throw new InterruptedException("Orphaned async task");
					}
				}
			} catch (InterruptedException e) {
				Logger.error("Interrupted yielding ", e);
				throw e;
			}

			return RembulanConverter.toObjects(values, 0, false);
		}

		public void resume(Object[] objects) throws InterruptedException {
			if (done) throw new IllegalStateException("Cannot resume when complete");
			values = objects;
			yieldLock.signal();
		}

		public boolean isDone() {
			return done;
		}

		public Object[] get() throws Throwable {
			if (!done) throw new IllegalStateException("Cannot get values when not complete");
			if (exception != null) throw exception;
			return values;
		}

		@Override
		public void run() {
			try {
				values = toValues(ArgumentDelegator.delegateLuaObject(object, this, method, new RembulanArguments(values)));
			} catch (Throwable e) {
				exception = e;
			} finally {
				done = true;
				externalLock.signal();
			}
		}
	}

	private final class LuaContextNoYield extends AbstractLuaContext {
		public LuaContextNoYield(Computer computer) {
			super(computer);
		}

		@Override
		public Object[] yield(Object[] objects) throws InterruptedException {
			throw new IllegalStateException("Cannot yield from non-yieldable method");
		}
	}
}

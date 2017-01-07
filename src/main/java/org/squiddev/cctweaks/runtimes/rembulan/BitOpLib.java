package org.squiddev.cctweaks.runtimes.rembulan;

import net.sandius.rembulan.*;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.lib.BadArgumentException;
import net.sandius.rembulan.runtime.*;
import org.squiddev.cctweaks.lua.lib.BinaryConverter;


/**
 * Reimplementation of the bitop library
 *
 * http://bitop.luajit.org/api.html
 *
 * Yeah, this isn't needed as we have actual bit ops. However comparability!
 */
public class BitOpLib {
	private static final String[] names = new String[]{
		"tobit", "bnot", "bswap",
		"tohex", "lshift", "rshift", "arshift", "rol", "ror",
		"band", "bor", "bxor",
	};

	private static int checkInt(Object object, String name, int index) {
		try {
			return (int) Conversions.toIntegerValue(object);
		} catch (NoIntegerRepresentationException e) {
			throw new BadArgumentException(index + 1, name, "expected number, got " + PlainValueTypeNamer.INSTANCE.typeNameOf(object));
		}
	}

	private static int optInt(Object object, String name, int index, int def) {
		return object == null ? def : checkInt(object, name, index);
	}

	private static class BitOneArg extends AbstractFunction1 {
		private final int opcode;
		private final String name;

		private BitOneArg(String name, int opcode) {
			this.name = name;
			this.opcode = opcode;
		}

		public Object call(Object luaValue) {
			switch (opcode) {
				case 0: // tobit
					return checkInt(luaValue, name, 1);
				case 1: // bnot
					return ~checkInt(luaValue, name, 1);
				case 2: // bswap
				{
					int i = checkInt(luaValue, name, 1);
					return (i & 0xff) << 24 | (i & 0xff00) << 8 | (i & 0xff0000) >> 8 | (i >> 24) & 0xff;
				}
				default:
					return null;
			}
		}

		private static void bind(Table table) {
			for (int i = 0; i < 3; i++) {
				BitOneArg func = new BitOneArg(names[i], i);
				table.rawset(names[i], func);
			}
		}

		@Override
		public void invoke(ExecutionContext context, Object arg1) throws ResolvedControlThrowable {
			context.getReturnBuffer().setTo(call(arg1));
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException();
		}
	}

	private static final byte[] lowerHexDigits = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private static final byte[] upperHexDigits = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	private static class BitTwoArg extends AbstractFunction2 {
		private final int opcode;
		private final String name;

		private BitTwoArg(String name, int opcode) {
			this.name = name;
			this.opcode = opcode;
		}

		public Object call(Object bitValue, Object nValue) {
			switch (opcode) {
				case 0: // tohex
				{
					int n = optInt(nValue, name, 2, 8);
					int bit = checkInt(bitValue, name, 1);

					byte[] hexes = lowerHexDigits;
					if (n < 0) {
						n = -n;
						hexes = upperHexDigits;
					}
					if (n > 8) n = 8;

					byte[] out = new byte[n];
					for (int i = n - 1; i >= 0; i--) {
						out[i] = hexes[bit & 15];
						bit >>= 4;
					}

					return BinaryConverter.decodeString(out);
				}
				case 1: // lshift
					return checkInt(bitValue, name, 1) << (checkInt(nValue, name, 2) & 31);
				case 2: // rshift
					return checkInt(bitValue, name, 1) >>> (checkInt(nValue, name, 2) & 31);
				case 3: // arshift
					return checkInt(bitValue, name, 1) >> (checkInt(nValue, name, 2) & 31);
				case 4: // rol
				{
					int b = checkInt(bitValue, name, 1);
					int n = checkInt(nValue, name, 2) & 31;
					return (b << n) | (b >>> (32 - n));
				}
				case 5: // ror
				{
					int b = checkInt(bitValue, name, 1);
					int n = checkInt(nValue, name, 2) & 31;
					return (b << (32 - n)) | (b >>> n);
				}
				default:
					return null;
			}
		}

		private static void bind(Table table) {
			for (int i = 3; i < 9; i++) {
				BitTwoArg func = new BitTwoArg(names[i], i - 3);
				table.rawset(names[i], func);
			}
		}

		@Override
		public void invoke(ExecutionContext context, Object arg1, Object arg2) throws ResolvedControlThrowable {
			context.getReturnBuffer().setTo(call(arg1, arg2));
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException();
		}
	}

	private static class BitVarArg extends AbstractFunctionAnyArg {
		private final int opcode;
		private final String name;

		private BitVarArg(String name, int opcode) {
			this.name = name;
			this.opcode = opcode;
		}

		public int invoke(Object[] varargs) {
			if (varargs.length == 0) throw new BadArgumentException(1, name, "expected number, got nil");

			int value = checkInt(varargs[0], name, 1), len = varargs.length;
			if (len == 1) return value;

			switch (opcode) {
				case 0: {
					for (int i = 1; i < len; i++) {
						value &= checkInt(varargs[i], name, i + 1);
					}
					break;
				}
				case 1: {
					for (int i = 1; i < len; i++) {
						value |= checkInt(varargs[i], name, i + 1);
					}
					break;
				}
				case 2: {
					for (int i = 1; i < len; i++) {
						value ^= checkInt(varargs[i], name, i + 1);
					}
					break;
				}
			}

			return value;
		}

		private static void bind(Table table) {
			for (int i = 9; i < 12; i++) {
				BitVarArg func = new BitVarArg(names[i], i - 9);
				table.rawset(names[i], func);
			}
		}

		@Override
		public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
			context.getReturnBuffer().setTo(invoke(args));
		}

		@Override
		public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
			throw new NonsuspendableFunctionException();
		}
	}

	public static void setup(StateContext state, Table env) {
		Table table = state.newTable(0, names.length + 3);
		BitOneArg.bind(table);
		BitTwoArg.bind(table);
		BitVarArg.bind(table);

		table.rawset("blshift", table.rawget("lshift"));
		table.rawset("brshift", table.rawget("arlshift"));
		table.rawset("blogic_rshift", table.rawget("rshift"));

		env.rawset("bitop", table);
	}
}

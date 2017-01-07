package org.squiddev.cctweaks.runtimes.rembulan;

import net.sandius.rembulan.LuaRuntimeException;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultUserdata;
import net.sandius.rembulan.lib.AbstractLibFunction;
import net.sandius.rembulan.lib.ArgumentIterator;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.runtime.ExecutionContext;
import org.squiddev.cctweaks.lua.lib.RandomProvider;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class BigIntegerValue extends DefaultUserdata {
	private static final String NAME = "biginteger";

	private final BigInteger number;

	private BigIntegerValue(BigInteger number, Table metatable) {
		super(metatable, number);
		this.number = number;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof BigIntegerValue && number.equals(((BigIntegerValue) o).number));
	}

	@Override
	public int hashCode() {
		return number.hashCode();
	}

	public static void setup(StateContext state, Table env) {
		env.rawset(NAME, BigIntegerFunction.makeTable(state, env));
	}

	private static BigInteger getValue(ArgumentIterator iterator) {
		Object value = iterator.hasNext() ? iterator.peek() : null;
		if (value == null) {
			iterator.nextUserdata(NAME, BigIntegerValue.class);
		} else if (value instanceof BigIntegerValue) {
			iterator.skip();
			return ((BigIntegerValue) value).number;
		} else if (value instanceof String) {
			try {
				return new BigInteger(value.toString());
			} catch (NumberFormatException e) {
				iterator.nextUserdata(NAME, BigIntegerValue.class);
			}
		} else {
			return BigInteger.valueOf(iterator.nextInteger());
		}

		throw new RuntimeException("Unreachable code in biginteger API");
	}

	private static class BigIntegerFunction extends AbstractLibFunction {
		private static final String[] META_NAMES = new String[]{
			"unm", "add", "sub", "mul", "mod", "pow", "div", "idiv",
			"band", "bor", "bxor", "shl", "shr", "bnot",
			"eq", "lt", "le",
			"tostring", "tonumber",
		};

		private static final String[] MAIN_NAMES = new String[]{
			"new", "modinv", "gcd", "modpow", "abs", "min", "max",
			"isProbPrime", "nextProbPrime", "newProbPrime", "seed",
		};

		private final String name;
		private final Table metatable;
		private final int opcode;
		private final RandomProvider random;

		private BigIntegerFunction(String name, Table metatable, int opcode, RandomProvider random) {
			this.name = name;
			this.metatable = metatable;
			this.opcode = opcode;
			this.random = random;
		}

		public Object call(ArgumentIterator iterator) {
			try {
				switch (opcode) {
					case 0: { // unm
						BigInteger leftB = getValue(iterator);
						return new BigIntegerValue(leftB.negate(), metatable);
					}
					case 1: { // add
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.add(rightNum), metatable);
					}
					case 2: { // sub
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.subtract(rightNum), metatable);
					}
					case 3: { // mul
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.multiply(rightNum), metatable);
					}
					case 4: { // mod
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.remainder(rightNum), metatable);
					}
					case 5: { // pow
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.pow(iterator.nextInt()), metatable);
					}
					case 6:
					case 7: { // div
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.divide(rightNum), metatable);
					}
					case 8: { // band
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.and(rightNum), metatable);
					}
					case 9: { // bor
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.or(rightNum), metatable);
					}
					case 10: { // bxor
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.xor(rightNum), metatable);
					}
					case 11: { // shl
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.shiftLeft(iterator.nextInt()), metatable);
					}
					case 12: { // shr
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.shiftRight(iterator.nextInt()), metatable);
					}
					case 13: { // bnot
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.not(), metatable);
					}
					case 14: { // eq
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return leftNum.equals(rightNum);
					}
					case 15: { // lt
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return leftNum.compareTo(rightNum) < 0;
					}
					case 16: { // le
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return leftNum.compareTo(rightNum) <= 0;
					}
					case 17: { // tostring
						return getValue(iterator).toString();
					}
					case 18: { // tonumber
						return getValue(iterator).doubleValue();
					}
					case 19: { // new
						Object left = iterator.hasNext() ? iterator.peek() : null;
						if (left instanceof BigIntegerValue) {
							return left;
						} else {
							return new BigIntegerValue(getValue(iterator), metatable);
						}
					}
					case 20: { // modinv
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.modInverse(rightNum), metatable);
					}
					case 21: { // gcd
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.gcd(rightNum), metatable);
					}
					case 22: { // modpow
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator), thirdNum = getValue(iterator);
						return new BigIntegerValue(leftNum.modPow(rightNum, thirdNum), metatable);
					}
					case 23: { // abs
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.abs(), metatable);
					}
					case 24: { // min
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.min(rightNum), metatable);
					}
					case 25: { // max
						BigInteger leftNum = getValue(iterator), rightNum = getValue(iterator);
						return new BigIntegerValue(leftNum.max(rightNum), metatable);
					}
					case 26: { // isProbPrime
						BigInteger leftNum = getValue(iterator);
						int rightProb = iterator.nextOptionalInt(100);
						return leftNum.isProbablePrime(rightProb);
					}
					case 27: { // nextProbPrime
						BigInteger leftNum = getValue(iterator);
						return new BigIntegerValue(leftNum.nextProbablePrime(), metatable);
					}
					case 28: { // newProbPrime
						int length = iterator.nextInt();
						SecureRandom seed;
						if (iterator.remaining() == 0) {
							seed = random.get();
						} else {
							seed = RandomProvider.create();
							seed.setSeed(getValue(iterator).toByteArray());
						}
						return new BigIntegerValue(BigInteger.probablePrime(length, seed), metatable);
					}
					case 29: { // seed
						if (iterator.remaining() == 0) {
							random.seed();
						} else {
							random.seed(getValue(iterator));
						}
						return null;
					}
					default:
						throw new LuaRuntimeException("No such method " + opcode);
				}
			} catch (ArithmeticException e) {
				// TODO: Handle this more sensibly
				return Double.NaN;
			}
		}

		@Override
		protected String name() {
			return name;
		}

		@Override
		public void invoke(ExecutionContext state, ArgumentIterator iterator) {
			state.getReturnBuffer().setTo(call(iterator));
		}

		private static Table makeTable(StateContext state, Table env) {
			Table meta = state.newTable(0, META_NAMES.length + 2);
			Table table = state.newTable(0, META_NAMES.length + MAIN_NAMES.length);

			RandomProvider random = new RandomProvider();

			for (int i = 0; i < META_NAMES.length; i++) {
				BigIntegerFunction func = new BigIntegerFunction(META_NAMES[i], meta, i, random);
				table.rawset(META_NAMES[i], func);
				meta.rawset("__" + META_NAMES[i], func);
			}

			for (int i = 0; i < MAIN_NAMES.length; i++) {
				BigIntegerFunction func = new BigIntegerFunction(MAIN_NAMES[i], meta, i + META_NAMES.length, random);
				table.rawset(MAIN_NAMES[i], func);
			}

			meta.rawset("__index", table);
			meta.rawset("__type", NAME);
			meta.rawset(BasicLib.MT_NAME, NAME);

			return table;
		}
	}
}

package org.squiddev.cctweaks.runtimes.rembulan;

import dan200.computercraft.api.lua.LuaException;
import net.sandius.rembulan.ByteString;
import org.squiddev.cctweaks.api.lua.IArguments;
import org.squiddev.cctweaks.lua.lib.BinaryConverter;

public class RembulanArguments implements IArguments {
	private final Object[] args;
	private final int offset;

	public RembulanArguments(Object[] args) {
		this(args, 0);
	}

	public RembulanArguments(Object[] args, int offset) {
		this.args = args;
		this.offset = offset;
	}

	@Override
	public int size() {
		return args.length - offset;
	}

	private Object get(int index) {
		return offset + index <= args.length ? null : args[offset + index];
	}

	@Override
	public double getNumber(int index) throws LuaException {
		Object value = get(index);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			throw new LuaException("Expected number");
		}
	}

	@Override
	public boolean getBoolean(int index) throws LuaException {
		Object value = get(index);
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else {
			throw new LuaException("Expected boolean");
		}
	}

	@Override
	public String getString(int index) throws LuaException {
		Object value = get(index);
		if (value instanceof String) {
			return (String) value;
		} else if (value instanceof ByteString) {
			return ((ByteString) value).toRawString();
		} else {
			throw new LuaException("Expected string");
		}
	}

	@Override
	public byte[] getStringBytes(int index) throws LuaException {
		Object value = get(index);
		if (value instanceof String) {
			return BinaryConverter.toBytes((String) value);
		} else if (value instanceof ByteString) {
			return ((ByteString) value).getBytes();
		} else {
			throw new LuaException("Expected string");
		}
	}

	@Override
	public Object getArgumentBinary(int index) {
		return RembulanConverter.toObject(get(index), true);
	}

	@Override
	public Object getArgument(int index) {
		return RembulanConverter.toObject(get(index), false);
	}

	@Override
	public Object[] asArguments() {
		return RembulanConverter.toObjects(args, 0, false);
	}

	@Override
	public Object[] asBinary() {
		return RembulanConverter.toObjects(args, 0, true);
	}

	@Override
	public IArguments subArgs(int offset) {
		return new RembulanArguments(args, offset);
	}
}

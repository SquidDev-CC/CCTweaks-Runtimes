package org.squiddev.cctweaks.runtimes.rembulan;

import net.sandius.rembulan.ByteString;
import net.sandius.rembulan.Table;
import org.squiddev.cctweaks.lua.lib.BinaryConverter;
import org.squiddev.cctweaks.lua.lib.luaj.LuaJConverter;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Duplicate of {@link LuaJConverter} but for Rembulan
 */
public class RembulanConverter {
	public static Object toObject(Object value, boolean binary) {
		return toObject(value, null, binary);
	}

	private static Object toObject(Object value, Map<Table, Object> tables, boolean binary) {
		if (value == null || value instanceof Boolean || value instanceof Double) {
			return value;
		} else if (value instanceof Number) {
			// CC requires a Double in some classes
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return binary ? BinaryConverter.toBytes((String) value) : value;
		} else if (value instanceof ByteString) {
			return binary ? ((ByteString) value).getBytes() : ((ByteString) value).toRawString();
		} else if (value instanceof Table) {
			if (tables == null) {
				tables = new IdentityHashMap<Table, Object>();
			} else {
				Object object = tables.get(value);
				if (object != null) return object;
			}

			Map<Object, Object> table = new HashMap<Object, Object>();
			Table luaValue = (Table) value;
			tables.put(luaValue, table);

			Object k = null;
			while (true) {
				k = k == null ? luaValue.initialKey() : luaValue.successorKeyOf(k);
				if (k == null) break;

				Object v = luaValue.rawget(k);
				Object keyObject = toObject(k, tables, binary);
				Object valueObject = toObject(v, tables, binary);
				if (keyObject != null && valueObject != null) {
					table.put(keyObject, valueObject);
				}
			}
			return table;
		} else {
			return null;
		}
	}

	public static Object[] toObjects(Object[] values, int start, boolean binary) {
		int count = values.length;
		Object[] objects = new Object[count - start];
		for (int n = start; n < count; n++) {
			int i = n - start;
			objects[i] = toObject(values[n], null, binary);
		}
		return objects;
	}
}

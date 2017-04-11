package ru.euphoria.messenger.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A modifiable set of name/value mappings. Names are unique, non-null strings.
 * Values may be any mix of {@link JsonObject JSONObjects}, {@link JsonArray
 * JSONArrays}, Strings, Booleans, Integers, Longs, Doubles or {@link #NULL}.
 * Values may not be {@code null}, {@link Double#isNaN() NaNs}, {@link
 * Double#isInfinite() infinities}, or of any type not listed here.
 * <p>
 * <p>This class can coerce values to another type when requested.
 * <ul>
 * <li>When the requested type is a boolean, strings will be coerced using a
 * case-insensitive comparison to "true" and "false".
 * <li>When the requested type is a double, other {@link Number} types will
 * be coerced using {@link Number#doubleValue() doubleValue}. Strings
 * that can be coerced using {@link Double#valueOf(String)} will be.
 * <li>When the requested type is an int, other {@link Number} types will
 * be coerced using {@link Number#intValue() intValue}. Strings
 * that can be coerced using {@link Double#valueOf(String)} will be,
 * and then cast to int.
 * <li><a name="lossy">When the requested type is a long, other {@link Number} types will
 * be coerced using {@link Number#longValue() longValue}. Strings
 * that can be coerced using {@link Double#valueOf(String)} will be,
 * and then cast to long. This two-step conversion is lossy for very
 * large values. For example, the string "9223372036854775806" yields the
 * long 9223372036854775807.</a>
 * <li>When the requested type is a String, other non-null values will be
 * coerced using {@link String#valueOf(Object)}. Although null cannot be
 * coerced, the sentinel value {@link JsonObject#NULL} is coerced to the
 * string "null".
 * </ul>
 * <p>
 * <p>This class can look up both mandatory and optional values:
 * <ul>
 * <li>Use <code>get<i>Type</i>()</code> to retrieve a mandatory value. This
 * fails with a {@code JsonException} if the requested name has no value
 * or if the value cannot be coerced to the requested type.
 * <li>Use <code>opt<i>Type</i>()</code> to retrieve an optional value. This
 * returns a system- or user-supplied default if the requested name has no
 * value or if the value cannot be coerced to the requested type.
 * </ul>
 * <p>
 * <p><strong>Warning:</strong> this class represents null in two incompatible
 * ways: the standard Java {@code null} reference, and the sentinel value {@link
 * JsonObject#NULL}. In particular, calling {@code put(name, null)} removes the
 * named entry from the object but {@code put(name, JsonObject.NULL)} stores an
 * entry whose value is {@code JsonObject.NULL}.
 * <p>
 * <p>Instances of this class are not thread safe. Although this class is
 * nonfinal, it was not designed for inheritance and should not be subclassed.
 * In particular, self-use by overrideable methods is not specified. See
 * <i>Effective Java</i> Item 17, "Design and Document or inheritance or else
 * prohibit it" for further information.
 */
public class JsonObject implements Serializable {

    private static final Double NEGATIVE_ZERO = -0d;

    /**
     * A sentinel value used to explicitly define a name with no value. Unlike
     * {@code null}, names with this value:
     * <ul>
     * <li>show up in the {@link #names} array
     * <li>show up in the {@link #keys} iterator
     * <li>return {@code true} for {@link #has(String)}
     * <li>do not throw on {@link #get(String)}
     * <li>are included in the encoded JSON string.
     * </ul>
     * <p>
     * <p>This value violates the general contract of {@link Object#equals} by
     * returning true when compared to {@code null}. Its {@link #toString}
     * method returns "null".
     */
    public static final Object NULL = new Object() {
        @Override public boolean equals(Object o) {
            return o == this || o == null; // API specifies this broken equals implementation
        }

        @Override public String toString() {
            return "null";
        }
    };

    private final LinkedHashMap<String, Object> nameValuePairs;

    /**
     * Creates a {@code JsonObject} with no name/value mappings.
     */
    public JsonObject() {
        nameValuePairs = new LinkedHashMap<>();
    }

    /**
     * Creates a new {@code JsonObject} by copying all name/value mappings from
     * the given map.
     *
     * @param from a map whose keys are of type {@link String} and whose
     *             values are of supported types.
     * @throws NullPointerException if any of the map's keys are null.
     */
    /* (accept a raw type for API compatibility) */
    public JsonObject(Map from) {
        this();
        Map<?, ?> contentsTyped = (Map<?, ?>) from;
        for (Map.Entry<?, ?> entry : contentsTyped.entrySet()) {
            /*
             * Deviate from the original by checking that keys are non-null and
             * of the proper type. (We still defer validating the values).
             */
            String key = (String) entry.getKey();
            if (key == null) {
                throw new NullPointerException("key == null");
            }
            nameValuePairs.put(key, wrap(entry.getValue()));
        }
    }

    /**
     * Creates a new {@code JsonObject} with name/value mappings from the next
     * object in the tokener.
     *
     * @param from a tokener whose nextValue() method will yield a
     *             {@code JsonObject}.
     * @throws JsonException if the parse fails or doesn't yield a
     *                       {@code JsonObject}.
     */
    public JsonObject(JsonParser from) throws JsonException {
        /*
         * Getting the parser to populate this could get tricky. Instead, just
         * parse to temporary JsonObject and then steal the data from that.
         */
        Object object = from.nextValue();
        if (object instanceof JsonObject) {
            this.nameValuePairs = ((JsonObject) object).nameValuePairs;
        } else {
            throw Json.typeMismatch(object, "JsonObject");
        }
    }

    /**
     * Creates a new {@code JsonObject} with name/value mappings from the JSON
     * string.
     *
     * @param json a JSON-encoded string containing an object.
     * @throws JsonException if the parse fails or doesn't yield a {@code
     *                       JsonObject}.
     */
    public JsonObject(String json) throws JsonException {
        this(new JsonParser(json));
    }

    /**
     * Creates a new {@code JsonObject} by copying mappings for the listed names
     * from the given object. Names that aren't present in {@code copyFrom} will
     * be skipped.
     */
    public JsonObject(JsonObject copyFrom, String[] names) throws JsonException {
        this();
        for (String name : names) {
            Object value = copyFrom.opt(name);
            if (value != null) {
                nameValuePairs.put(name, value);
            }
        }
    }

    /**
     * Returns the number of name/value mappings in this object.
     */
    public int length() {
        return nameValuePairs.size();
    }

    /**
     * Maps {@code name} to {@code value}, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    public JsonObject put(String name, boolean value) throws JsonException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    /**
     * Maps {@code name} to {@code value}, clobbering any existing name/value
     * mapping with the same name.
     *
     * @param value a finite value. May not be {@link Double#isNaN() NaNs} or
     *              {@link Double#isInfinite() infinities}.
     * @return this object.
     */
    public JsonObject put(String name, double value) throws JsonException {
        nameValuePairs.put(checkName(name), Json.checkDouble(value));
        return this;
    }

    /**
     * Maps {@code name} to {@code value}, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    public JsonObject put(String name, int value) throws JsonException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    /**
     * Maps {@code name} to {@code value}, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    public JsonObject put(String name, long value) throws JsonException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    /**
     * Maps {@code name} to {@code value}, clobbering any existing name/value
     * mapping with the same name. If the value is {@code null}, any existing
     * mapping for {@code name} is removed.
     *
     * @param value a {@link JsonObject}, {@link JsonArray}, String, Boolean,
     *              Integer, Long, Double, {@link #NULL}, or {@code null}. May not be
     *              {@link Double#isNaN() NaNs} or {@link Double#isInfinite()
     *              infinities}.
     * @return this object.
     */
    public JsonObject put(String name, Object value) throws JsonException {
        if (value == null) {
            nameValuePairs.remove(name);
            return this;
        }
        if (value instanceof Number) {
            // deviate from the original by checking all Numbers, not just floats & doubles
            Json.checkDouble(((Number) value).doubleValue());
        }
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    /**
     * Equivalent to {@code put(name, value)} when both parameters are non-null;
     * does nothing otherwise.
     */
    public JsonObject putOpt(String name, Object value) throws JsonException {
        if (name == null || value == null) {
            return this;
        }
        return put(name, value);
    }

    /**
     * Appends {@code value} to the array already mapped to {@code name}. If
     * this object has no mapping for {@code name}, this inserts a new mapping.
     * If the mapping exists but its value is not an array, the existing
     * and new values are inserted in order into a new array which is itself
     * mapped to {@code name}. In aggregate, this allows values to be added to a
     * mapping one at a time.
     * <p>
     * <p> Note that {@code append(String, Object)} provides better semantics.
     * In particular, the mapping for {@code name} will <b>always</b> be a
     * {@link JsonArray}. Using {@code accumulate} will result in either a
     * {@link JsonArray} or a mapping whose type is the type of {@code value}
     * depending on the number of calls to it.
     *
     * @param value a {@link JsonObject}, {@link JsonArray}, String, Boolean,
     *              Integer, Long, Double, {@link #NULL} or null. May not be {@link
     *              Double#isNaN() NaNs} or {@link Double#isInfinite() infinities}.
     */
    // TODO: Change {@code append) to {@link #append} when append is
    // unhidden.
    public JsonObject accumulate(String name, Object value) throws JsonException {
        Object current = nameValuePairs.get(checkName(name));
        if (current == null) {
            return put(name, value);
        }

        if (current instanceof JsonArray) {
            JsonArray array = (JsonArray) current;
            array.checkedPut(value);
        } else {
            JsonArray array = new JsonArray();
            array.checkedPut(current);
            array.checkedPut(value);
            nameValuePairs.put(name, array);
        }
        return this;
    }

    /**
     * Appends values to the array mapped to {@code name}. A new {@link JsonArray}
     * mapping for {@code name} will be inserted if no mapping exists. If the existing
     * mapping for {@code name} is not a {@link JsonArray}, a {@link JsonException}
     * will be thrown.
     *
     * @throws JsonException if {@code name} is {@code null} or if the mapping for
     *                       {@code name} is non-null and is not a {@link JsonArray}.
     */
    public JsonObject append(String name, Object value) throws JsonException {
        Object current = nameValuePairs.get(checkName(name));

        final JsonArray array;
        if (current instanceof JsonArray) {
            array = (JsonArray) current;
        } else if (current == null) {
            JsonArray newArray = new JsonArray();
            nameValuePairs.put(name, newArray);
            array = newArray;
        } else {
            throw new JsonException("Key " + name + " is not a JsonArray");
        }

        array.checkedPut(value);

        return this;
    }

    private String checkName(String name) throws JsonException {
        if (name == null) {
            throw new JsonException("Names must be non-null");
        }
        return name;
    }

    /**
     * Removes the named mapping if it exists; does nothing otherwise.
     *
     * @return the value previously mapped by {@code name}, or null if there was
     * no such mapping.
     */
    public Object remove(String name) {
        return nameValuePairs.remove(name);
    }

    /**
     * Returns true if this object has no mapping for {@code name} or if it has
     * a mapping whose value is {@link #NULL}.
     */
    public boolean isNull(String name) {
        Object value = nameValuePairs.get(name);
        return value == null || value == NULL;
    }

    /**
     * Returns true if this object has a mapping for {@code name}. The mapping
     * may be {@link #NULL}.
     */
    public boolean has(String name) {
        return nameValuePairs.containsKey(name);
    }

    /**
     * Returns the value mapped by {@code name}, or throws if no such mapping exists.
     *
     * @throws JsonException if no such mapping exists.
     */
    public Object get(String name) throws JsonException {
        Object result = nameValuePairs.get(name);
        if (result == null) {
            throw new JsonException("No value for " + name);
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name}, or null if no such mapping
     * exists.
     */
    public Object opt(String name) {
        return nameValuePairs.get(name);
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a boolean or
     * can be coerced to a boolean, or throws otherwise.
     *
     * @throws JsonException if the mapping doesn't exist or cannot be coerced
     *                       to a boolean.
     */
    public boolean getBoolean(String name) throws JsonException {
        Object object = get(name);
        Boolean result = Json.toBoolean(object);
        if (result == null) {
            throw Json.typeMismatch(name, object, "boolean");
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a boolean or
     * can be coerced to a boolean, or false otherwise.
     */
    public boolean optBoolean(String name) {
        return optBoolean(name, false);
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a boolean or
     * can be coerced to a boolean, or {@code fallback} otherwise.
     */
    public boolean optBoolean(String name, boolean fallback) {
        Object object = opt(name);
        Boolean result = Json.toBoolean(object);
        return result != null ? result : fallback;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a double or
     * can be coerced to a double, or throws otherwise.
     *
     * @throws JsonException if the mapping doesn't exist or cannot be coerced
     *                       to a double.
     */
    public double getDouble(String name) throws JsonException {
        Object object = get(name);
        Double result = Json.toDouble(object);
        if (result == null) {
            throw Json.typeMismatch(name, object, "double");
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a double or
     * can be coerced to a double, or {@code NaN} otherwise.
     */
    public double optDouble(String name) {
        return optDouble(name, Double.NaN);
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a double or
     * can be coerced to a double, or {@code fallback} otherwise.
     */
    public double optDouble(String name, double fallback) {
        Object object = opt(name);
        Double result = Json.toDouble(object);
        return result != null ? result : fallback;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is an int or
     * can be coerced to an int, or throws otherwise.
     *
     * @throws JsonException if the mapping doesn't exist or cannot be coerced
     *                       to an int.
     */
    public int getInt(String name) throws JsonException {
        Object object = get(name);
        Integer result = Json.toInteger(object);
        if (result == null) {
            throw Json.typeMismatch(name, object, "int");
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is an int or
     * can be coerced to an int, or 0 otherwise.
     */
    public int optInt(String name) {
        return optInt(name, 0);
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is an int or
     * can be coerced to an int, or {@code fallback} otherwise.
     */
    public int optInt(String name, int fallback) {
        Object object = opt(name);
        Integer result = Json.toInteger(object);
        return result != null ? result : fallback;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a long or
     * can be coerced to a long, or throws otherwise.
     * Note that JSON represents numbers as doubles,
     * so this is <a href="#lossy">lossy</a>; use strings to transfer numbers via JSON.
     *
     * @throws JsonException if the mapping doesn't exist or cannot be coerced
     *                       to a long.
     */
    public long getLong(String name) throws JsonException {
        Object object = get(name);
        Long result = Json.toLong(object);
        if (result == null) {
            throw Json.typeMismatch(name, object, "long");
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a long or
     * can be coerced to a long, or 0 otherwise. Note that JSON represents numbers as doubles,
     * so this is <a href="#lossy">lossy</a>; use strings to transfer numbers via JSON.
     */
    public long optLong(String name) {
        return optLong(name, 0L);
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a long or
     * can be coerced to a long, or {@code fallback} otherwise. Note that JSON represents
     * numbers as doubles, so this is <a href="#lossy">lossy</a>; use strings to transfer
     * numbers via JSON.
     */
    public long optLong(String name, long fallback) {
        Object object = opt(name);
        Long result = Json.toLong(object);
        return result != null ? result : fallback;
    }

    /**
     * Returns the value mapped by {@code name} if it exists, coercing it if
     * necessary, or throws if no such mapping exists.
     *
     * @throws JsonException if no such mapping exists.
     */
    public String getString(String name) throws JsonException {
        Object object = get(name);
        String result = Json.toString(object);
        if (result == null) {
            throw Json.typeMismatch(name, object, "String");
        }
        return result;
    }

    /**
     * Returns the value mapped by {@code name} if it exists, coercing it if
     * necessary, or the empty string if no such mapping exists.
     */
    public String optString(String name) {
        return optString(name, "");
    }

    /**
     * Returns the value mapped by {@code name} if it exists, coercing it if
     * necessary, or {@code fallback} if no such mapping exists.
     */
    public String optString(String name, String fallback) {
        Object object = opt(name);
        String result = Json.toString(object);
        return result != null ? result : fallback;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a {@code
     * JsonArray}, or throws otherwise.
     *
     * @throws JsonException if the mapping doesn't exist or is not a {@code
     *                       JsonArray}.
     */
    public JsonArray getJsonArray(String name) throws JsonException {
        Object object = get(name);
        if (object instanceof JsonArray) {
            return (JsonArray) object;
        } else {
            throw Json.typeMismatch(name, object, "JsonArray");
        }
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a {@code
     * JsonArray}, or null otherwise.
     */
    public JsonArray optJsonArray(String name) {
        Object object = opt(name);
        return object instanceof JsonArray ? (JsonArray) object : null;
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a {@code
     * JsonObject}, or throws otherwise.
     *
     * @throws JsonException if the mapping doesn't exist or is not a {@code
     *                       JsonObject}.
     */
    public JsonObject getJsonObject(String name) throws JsonException {
        Object object = get(name);
        if (object instanceof JsonObject) {
            return (JsonObject) object;
        } else {
            throw Json.typeMismatch(name, object, "JsonObject");
        }
    }

    /**
     * Returns the value mapped by {@code name} if it exists and is a {@code
     * JsonObject}, or null otherwise.
     */
    public JsonObject optJsonObject(String name) {
        Object object = opt(name);
        return object instanceof JsonObject ? (JsonObject) object : null;
    }

    /**
     * Returns an array with the values corresponding to {@code names}. The
     * array contains null for names that aren't mapped. This method returns
     * null if {@code names} is either null or empty.
     */
    public JsonArray toJsonArray(JsonArray names) throws JsonException {
        JsonArray result = new JsonArray();
        if (names == null) {
            return null;
        }
        int length = names.length();
        if (length == 0) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            String name = Json.toString(names.opt(i));
            result.put(opt(name));
        }
        return result;
    }

    /**
     * Returns an iterator of the {@code String} names in this object. The
     * returned iterator supports {@link Iterator#remove() remove}, which will
     * remove the corresponding mapping from this object. If this object is
     * modified after the iterator is returned, the iterator's behavior is
     * undefined. The order of the keys is undefined.
     */
    public Iterator<String> keys() {
        return nameValuePairs.keySet().iterator();
    }

    /**
     * Returns the set of {@code String} names in this object. The returned set
     * is a view of the keys in this object. {@link Set#remove(Object)} will remove
     * the corresponding mapping from this object and set iterator behaviour
     * is undefined if this object is modified after it is returned.
     * <p>
     * See {@link #keys()}.
     */
    public Set<String> keySet() {
        return nameValuePairs.keySet();
    }

    /**
     * Returns an array containing the string names in this object. This method
     * returns null if this object contains no mappings.
     */
    public JsonArray names() {
        return nameValuePairs.isEmpty()
                ? null
                : new JsonArray(new ArrayList<String>(nameValuePairs.keySet()));
    }

    /**
     * Encodes this object as a compact JSON string, such as:
     * <pre>{"query":"Pizza","locations":[94043,90210]}</pre>
     */
    @Override
    public String toString() {
        try {
            JsonStringer stringer = new JsonStringer();
            writeTo(stringer);
            return stringer.toString();
        } catch (JsonException e) {
            return null;
        }
    }

    /**
     * Encodes this object as a human readable JSON string for debugging, such
     * as:
     * <pre>
     * {
     *     "query": "Pizza",
     *     "locations": [
     *         94043,
     *         90210
     *     ]
     * }</pre>
     *
     * @param indentSpaces the number of spaces to indent for each level of
     *                     nesting.
     */
    public String toString(int indentSpaces) throws JsonException {
        JsonStringer stringer = new JsonStringer(indentSpaces);
        writeTo(stringer);
        return stringer.toString();
    }

    void writeTo(JsonStringer stringer) throws JsonException {
        stringer.object();
        for (Map.Entry<String, Object> entry : nameValuePairs.entrySet()) {
            stringer.key(entry.getKey()).value(entry.getValue());
        }
        stringer.endObject();
    }

    /**
     * Encodes the number as a JSON string.
     *
     * @param number a finite value. May not be {@link Double#isNaN() NaNs} or
     *               {@link Double#isInfinite() infinities}.
     */
    public static String numberToString(Number number) throws JsonException {
        if (number == null) {
            throw new JsonException("Number must be non-null");
        }

        double doubleValue = number.doubleValue();
        Json.checkDouble(doubleValue);

        // the original returns "-0" instead of "-0.0" for negative zero
        if (number.equals(NEGATIVE_ZERO)) {
            return "-0";
        }

        long longValue = number.longValue();
        if (doubleValue == (double) longValue) {
            return Long.toString(longValue);
        }

        return number.toString();
    }

    /**
     * Encodes {@code data} as a JSON string. This applies quotes and any
     * necessary character escaping.
     *
     * @param data the string to encode. Null will be interpreted as an empty
     *             string.
     */
    public static String quote(String data) {
        if (data == null) {
            return "\"\"";
        }
        try {
            JsonStringer stringer = new JsonStringer();
            stringer.open(JsonStringer.Scope.NULL, "");
            stringer.value(data);
            stringer.close(JsonStringer.Scope.NULL, JsonStringer.Scope.NULL, "");
            return stringer.toString();
        } catch (JsonException e) {
            throw new AssertionError();
        }
    }

    /**
     * Wraps the given object if necessary.
     * <p>
     * <p>If the object is null or , returns {@link #NULL}.
     * If the object is a {@code JsonArray} or {@code JsonObject}, no wrapping is necessary.
     * If the object is {@code NULL}, no wrapping is necessary.
     * If the object is an array or {@code Collection}, returns an equivalent {@code JsonArray}.
     * If the object is a {@code Map}, returns an equivalent {@code JsonObject}.
     * If the object is a primitive wrapper type or {@code String}, returns the object.
     * Otherwise if the object is from a {@code java} package, returns the result of {@code toString}.
     * If wrapping fails, returns null.
     */
    public static Object wrap(Object o) {
        if (o == null) {
            return NULL;
        }
        if (o instanceof JsonArray || o instanceof JsonObject) {
            return o;
        }
        if (o.equals(NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JsonArray((Collection) o);
            } else if (o.getClass().isArray()) {
                return new JsonArray(o);
            }
            if (o instanceof Map) {
                return new JsonObject((Map) o);
            }
            if (o instanceof Boolean ||
                    o instanceof Byte ||
                    o instanceof Character ||
                    o instanceof Double ||
                    o instanceof Float ||
                    o instanceof Integer ||
                    o instanceof Long ||
                    o instanceof Short ||
                    o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.utils

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.Enum.valueOf
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Wrapper for [JSONObject]. Use to store saves and configurations.
 *
 * WARNING: NOT ALL METHODS IN ORG.JSON ARE PRESENT ON ANDROID/IOS!
 * Many methods which work on desktop will cause the game to crash on Android and iOS
 *
 * This is because the Android runtime includes its own version of org.json which does not
 * implement all methods. MobiVM uses the Android runtime and so this applies to iOS as well.
 *
 * org.json is very fast (~2x faster than libgdx JSON), which is why the game uses it despite
 * this dependency conflict.
 *
 * See https://developer.android.com/reference/org/json/package-summary for details on
 * what methods exist in all versions of org.json. This class is also commented in places
 * Where Android/iOS force the use of unusual methods.
 */
class Bundle private constructor(
    private val data: JSONObject
) {

    constructor() : this(JSONObject())

    override fun toString() = data.toString()

    operator fun contains(key: String) = !data.isNull(key)

    fun remove(key: String): Boolean = data.remove(key) != null

    // region get...

    operator fun get(key: String): Bundlable? = getBundle(key)?.get()

    // JSONObject.keyset() doesn't exist on Android/iOS
    fun getKeys(): List<String> = data.keys().asSequence().toList()

    fun getBoolean(key: String): Boolean = data.optBoolean(key)

    fun getInt(key: String): Int = data.optInt(key)

    fun getLong(key: String): Long = data.optLong(key)

    fun getFloat(key: String): Float = data.optDouble(key, 0.0).toFloat()

    fun getString(key: String): String = data.optString(key)

    fun getClass(key: String): Class<*>? = Reflection.forName(getString(key).replace("class ", "").let { cls ->
        if (cls == "") return null
        aliases[cls] ?: cls
    })

    fun getBundle(key: String): Bundle? = data.optJSONObject(key)?.let { Bundle(it) }

    private fun get(): Bundlable? {
        return (Reflection.newInstance(Reflection.forName(getString(CLASS_NAME).let { cls ->
            aliases[cls] ?: cls
        }) ?: return null) as Bundlable).also {
            it.restoreFromBundle(this)
        }
    }

    fun <E : Enum<E>> getEnum(key: String, enumClass: Class<E>): E = valueOf(enumClass, data.getString(key))

    fun getIntArray(key: String): IntArray = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            it.getInt(index)
        }
    }.toIntArray()

    fun getLongArray(key: String): LongArray = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            it.getLong(index)
        }
    }.toLongArray()

    fun getFloatArray(key: String): FloatArray = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            it.optDouble(index, 0.0).toFloat()
        }
    }.toFloatArray()

    fun getBooleanArray(key: String): BooleanArray = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            it.getBoolean(index)
        }
    }.toBooleanArray()

    fun getStringArray(key: String): Array<String> = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            it.getString(index)
        }
    }.toTypedArray()

    fun getClassArray(key: String): Array<Class<*>> = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            Reflection.forName(it.getString(index)
                .replace("class ", "")
                .let { cls -> aliases[cls] ?: cls })
        }
    }.toTypedArray()

    @JvmOverloads
    fun getBundleArray(key: String = DEFAULT_KEY): Array<Bundle> = data.getJSONArray(key).let {
        (0 until it.length()).map { index ->
            Bundle(it.getJSONObject(index))
        }
    }.toTypedArray()

    fun getCollection(key: String): Collection<Bundlable> = data.getJSONArray(key).let {
        (0 until it.length()).mapNotNull { index ->
            Bundle(it.getJSONObject(index)).get()
        }
    }

    // endregion

    // region put...

    fun put(key: String, value: Boolean?) {
        data.put(key, value)
    }

    fun put(key: String, value: Int?) {
        data.put(key, value)
    }

    fun put(key: String, value: Long?) {
        data.put(key, value)
    }

    fun put(key: String, value: Float?) {
        data.put(key, value?.toDouble())
    }

    fun put(key: String, value: String?) {
        data.put(key, value)
    }

    fun put(key: String, value: Class<*>?) {
        data.put(key, value)
    }

    fun put(key: String, bundle: Bundle?) {
        data.put(key, bundle?.data)
    }

    fun put(key: String, obj: Bundlable?) {
        data.put(key, storeObject(obj))
    }

    fun put(key: String, value: Enum<*>?) {
        data.put(key, value?.name)
    }

    fun put(key: String, array: IntArray?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i]) }
        })
    }

    fun put(key: String, array: LongArray?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i]) }
        })
    }

    fun put(key: String, array: FloatArray?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i].toDouble()) }
        })
    }

    fun put(key: String, array: BooleanArray?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i]) }
        })
    }

    fun put(key: String, array: Array<String>?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i]) }
        })
    }

    fun put(key: String, array: Array<Class<*>>?) {
        data.put(key, JSONArray().also {
            array?.indices?.forEach { i -> it.put(i, array[i].name) }
        })
    }

    fun put(key: String, collection: Collection<Bundlable?>?) {
        data.put(key, collection?.let { col ->
            JSONArray(col.mapNotNull { storeObject(it) })
        } ?: JSONArray())
    }

    // endregion

    /**
     * Writes contents of the bundle into a stream.
     * @param stream stream to write into
     * @param compressed whether the data should be compressed
     */
    fun toStream(stream: OutputStream, compressed: Boolean = COMPRESSION) {
        val writer =
            if (compressed) BufferedWriter(OutputStreamWriter(GZIPOutputStream(stream, GZIP_BUFFER)))
            else BufferedWriter(OutputStreamWriter(stream))
        writer.write(data.toString()) // JSONObject.write doesn't exist on Android/iOS
        writer.close()
        stream.close()
    }

    companion object {

        private const val CLASS_NAME = "__className"
        private const val DEFAULT_KEY = "key"

        private val aliases = HashMap<String, String>()

        // Turn this off for save data debugging.
        private const val COMPRESSION = true
        private const val GZIP_BUFFER = 1024 * 4 // 4Kb

        /**
         * Produces a bundle from the stream.
         * @param stream stream to read data from
         * @return resulting bundle
         */
        @JvmStatic
        fun read(stream: InputStream): Bundle {

            // JSONTokenizer only has a string-based constructor on Android/iOS.
            val reader = BufferedReader(InputStreamReader(checkCompression(stream)))
            val jsonBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) jsonBuilder.append(line + "\n")
            var json = JSONTokener(jsonBuilder.toString()).nextValue()
            reader.close()

            // If the data is an array, put it in a fresh object with the default key.
            if (json is JSONArray) json = JSONObject().put(DEFAULT_KEY, json)

            return Bundle(json as JSONObject)
        }

        private fun checkCompression(stream: InputStream): InputStream {

            var str = stream
            if (!str.markSupported()) str = BufferedInputStream(str, 2)

            // Determine if it's a regular or compressed file.
            str.mark(2)
            val header = ByteArray(2)
            str.read(header)
            str.reset()

            // GZIP header is 0x1f8b.
            return if (header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte())
                GZIPInputStream(str, GZIP_BUFFER)
            else
                str
        }

        private fun storeObject(obj: Bundlable?): JSONObject? = obj?.let {
            val cl: Class<*> = it.javaClass
            // Skip none-static inner classes as they can't be instantiated through bundle restoring.
            // Classes which make use of none-static inner classes must manage instantiation manually.
            if (Reflection.isMemberClass(cl) && !Reflection.isStatic(cl)) return null
            val bundle = Bundle()
            bundle.put(CLASS_NAME, cl.name)
            it.storeInBundle(bundle)
            bundle.data
        }

        /**
         * Adds an alias to the class.
         * This will essentially convert bundled [aliased][alias] object into the specified [class][cl] the first time the bundle is loaded.
         * Aliases can be used to remove items from the game without breaking the existing save files.
         * @param cl class to convert found aliased objects into
         * @param alias old class name as it was saved into the bundle
         */
        @JvmStatic
        fun addAlias(cl: Class<*>, alias: String) {
            aliases[alias] = cl.name
        }
    }
}
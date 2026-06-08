package com.taylorz.logutils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.IntDef
import androidx.collection.SimpleArrayMap
import com.taylorz.logutils.GsonUtils.gson4LogUtils
import com.taylorz.logutils.ThrowableUtils.getFullStackTrace
import com.taylorz.logutils.UtilsBridge.FileHead
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.lang.reflect.ParameterizedType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.math.min

class LogUtils private constructor(){
    @IntDef(V, D, I, W, E, A)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TYPE
    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }

    class Config {
        val defaultDir: String
            get() = (UtilsBridge.getAppContext()?.filesDir?.toString() ?: "") + FILE_SEP + "log" + FILE_SEP // The default storage directory of log.

        var filePrefix: String = "util" // The file prefix of log.
            set(value) {
                field = if (UtilsBridge.isSpace(value)) "util" else value
            }
        var fileExtension: String = ".txt" // The file extension of log.
            set(value) {
                if (UtilsBridge.isSpace(value)) {
                    field = ".txt"
                } else {
                    if (value.startsWith(".")) {
                        field = value
                    } else {
                        field = ".$fileExtension"
                    }
                }
            }
        var isLogSwitch: Boolean = true // The switch of log.
        var isLog2ConsoleSwitch: Boolean = true // The logcat's switch of log.
        var mTagIsSpace: Boolean = true // The global tag is space.
        var isLogHeadSwitch: Boolean = true // The head's switch of log.
        var isLog2FileSwitch: Boolean = false // The file's switch of log.
        var isLogBorderSwitch: Boolean = true // The border's switch of log.
        var isSingleTagSwitch: Boolean = true // The single tag of log.
        var mConsoleFilter: Int = V // The console's filter of log.
        var mFileFilter: Int = V // The file's filter of log.
        var stackDeep: Int = 1 // The stack's deep of log.

        var stackOffset: Int = 0 // The stack's offset of log.
        var saveDays: Int = -1 // The save days of log.
        var mFileWriter: IFileWriter? = null
        var onConsoleOutputListener: ((type: Int, tag: String, content: String)->Unit)? = null
        var mOnFileOutputListener: ((filePath: String, content: String)->Unit)? = null
        internal val mFileHead = FileHead("Log")

        fun <T> addFormatter(iFormatter: IFormatter<T>?): Config {
            if (iFormatter != null) {
                I_FORMATTER_MAP.put(getTypeClassFromParadigm(iFormatter), iFormatter)
            }
            return this
        }


        fun addFileExtraHead(fileExtraHead: Map<String, String>?): Config {
            mFileHead.append(fileExtraHead)
            return this
        }

        fun addFileExtraHead(key: String, value: String): Config {
            mFileHead.append(key, value)
            return this
        }

        var processName: String = "process_name"
            get() {
                return field.replace(":", "_")
            }

        var dir: String= defaultDir
            set(value) {
                field = if (UtilsBridge.isSpace(value)) {
                    ""
                } else {
                    if (value.endsWith(FILE_SEP)) value else value + FILE_SEP
                }
            }
            get() = field

        var globalTag: String = ""
            set(value) {
                mTagIsSpace = UtilsBridge.isSpace(value)
                field = value
            }
            get() {
                if (UtilsBridge.isSpace(field)) return ""
                return field
            }

        val consoleFilter: Char
            get() = T[mConsoleFilter - V]

        val fileFilter: Char
            get() = T[mFileFilter - V]
    }

    abstract class IFormatter<T> {
        abstract fun format(t: Any?): String
    }

    interface IFileWriter {
        fun write(file: String?, content: String?)
    }

    private class TagHead(var tag: String, var consoleHead: Array<String>?, var fileHead: String)

    private object LogFormatter {
        @JvmOverloads
        fun object2String(obj: Any, type: Int = -1): String {
            if (obj.javaClass.isArray) return array2String(obj)
            if (obj is Throwable) return getFullStackTrace(obj)
            if (obj is Bundle) return bundle2String(obj)
            if (obj is Intent) return intent2String(obj)
            if (type == JSON) {
                return object2Json(obj)
            } else if (type == XML) {
                return formatXml(obj.toString())
            }
            return obj.toString()
        }

        fun bundle2String(bundle: Bundle): String {
            val iterator: Iterator<String> = bundle.keySet().iterator()
            if (!iterator.hasNext()) {
                return "Bundle {}"
            }
            val sb = StringBuilder(128)
            sb.append("Bundle { ")
            while (true) {
                val key = iterator.next()
                val value = bundle[key]
                sb.append(key).append('=')
                if (value is Bundle) {
                    sb.append(
                        if (value === bundle) "(this Bundle)" else bundle2String(
                            value
                        )
                    )
                } else {
                    sb.append(formatObject(value))
                }
                if (!iterator.hasNext()) return sb.append(" }").toString()
                sb.append(',').append(' ')
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        fun intent2String(intent: Intent): String {
            val sb = StringBuilder(128)
            sb.append("Intent { ")
            var first = true
            val mAction = intent.action
            if (mAction != null) {
                sb.append("act=").append(mAction)
                first = false
            }
            val mCategories = intent.categories
            if (mCategories != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("cat=[")
                var firstCategory = true
                for (c in mCategories) {
                    if (!firstCategory) {
                        sb.append(',')
                    }
                    sb.append(c)
                    firstCategory = false
                }
                sb.append("]")
            }
            val mData = intent.data
            if (mData != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("dat=").append(mData)
            }
            val mType = intent.type
            if (mType != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("typ=").append(mType)
            }
            val mFlags = intent.flags
            if (mFlags != 0) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("flg=0x").append(Integer.toHexString(mFlags))
            }
            val mPackage = intent.getPackage()
            if (mPackage != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("pkg=").append(mPackage)
            }
            val mComponent = intent.component
            if (mComponent != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("cmp=").append(mComponent.flattenToShortString())
            }
            val mSourceBounds = intent.sourceBounds
            if (mSourceBounds != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("bnds=").append(mSourceBounds.toShortString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val mClipData = intent.clipData
                if (mClipData != null) {
                    if (!first) {
                        sb.append(' ')
                    }
                    first = false
                    clipData2String(mClipData, sb)
                }
            }
            val mExtras = intent.extras
            if (mExtras != null) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append("extras={")
                sb.append(bundle2String(mExtras))
                sb.append('}')
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                val mSelector = intent.selector
                if (mSelector != null) {
                    if (!first) {
                        sb.append(' ')
                    }
                    first = false
                    sb.append("sel={")
                    sb.append(if (mSelector === intent) "(this Intent)" else intent2String(mSelector))
                    sb.append("}")
                }
            }
            sb.append(" }")
            return sb.toString()
        }

        fun clipData2String(clipData: ClipData, sb: StringBuilder) {
            val item = clipData.getItemAt(0)
            if (item == null) {
                sb.append("ClipData.Item {}")
                return
            }
            sb.append("ClipData.Item { ")
            val mHtmlText = item.htmlText
            if (mHtmlText != null) {
                sb.append("H:")
                sb.append(mHtmlText)
                sb.append("}")
                return
            }
            val mText = item.text
            if (mText != null) {
                sb.append("T:")
                sb.append(mText)
                sb.append("}")
                return
            }
            val uri = item.uri
            if (uri != null) {
                sb.append("U:").append(uri)
                sb.append("}")
                return
            }
            val intent = item.intent
            if (intent != null) {
                sb.append("I:")
                sb.append(intent2String(intent))
                sb.append("}")
                return
            }
            sb.append("NULL")
            sb.append("}")
        }

        fun object2Json(obj: Any): String {
            if (obj is CharSequence) {
                return JsonUtils.formatJson(obj.toString())
            }
            return try {
                gson4LogUtils.toJson(obj)
            } catch (t: Throwable) {
                obj.toString()
            }
        }

        fun formatJson(json: String): String {
            try {
                var i = 0
                val len = json.length
                while (i < len) {
                    val c = json[i]
                    if (c == '{') {
                        return JSONObject(json).toString(2)
                    } else if (c == '[') {
                        return JSONArray(json).toString(2)
                    } else if (!Character.isWhitespace(c)) {
                        return json
                    }
                    i++
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return json
        }

        fun formatXml(xml: String): String {
            var result = xml
            try {
                val xmlInput: Source = StreamSource(StringReader(xml))
                val xmlOutput = StreamResult(StringWriter())
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                transformer.transform(xmlInput, xmlOutput)
                result = xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">" + LINE_SEP)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        fun array2String(obj: Any): String {
            return when (obj) {
                is Array<*> -> obj.contentDeepToString()
                is BooleanArray -> obj.contentToString()
                is ByteArray -> obj.contentToString()
                is CharArray -> obj.contentToString()
                is DoubleArray -> obj.contentToString()
                is FloatArray -> obj.contentToString()
                is IntArray -> obj.contentToString()
                is LongArray -> obj.contentToString()
                is ShortArray -> obj.contentToString()
                else -> throw IllegalArgumentException("Unsupported array type: ${obj.javaClass}")
            }
        }
    }

    companion object  {
        const val V: Int = Log.VERBOSE
        const val D: Int = Log.DEBUG
        const val I: Int = Log.INFO
        const val W: Int = Log.WARN
        const val E: Int = Log.ERROR
        const val A: Int = Log.ASSERT

        private val T = charArrayOf('V', 'D', 'I', 'W', 'E', 'A')

        private const val FILE = 0x10
        private const val JSON = 0x20
        private const val XML = 0x30

        private val FILE_SEP: String = File.separator
        private val LINE_SEP: String = System.lineSeparator()
        private val LOG_DATE_PATTERN = Pattern.compile("[0-9]{4}_[0-9]{2}_[0-9]{2}")
        private const val TOP_CORNER = "┌"
        private const val TOP_RIGHT_CORNER = "┐"
        private const val MIDDLE_CORNER = "├"
        private const val MIDDLE_RIGHT_CORNER = "┤"
        private const val LEFT_BORDER = "│ "
        private const val RIGHT_BORDER = " │"
        private const val BOTTOM_CORNER = "└"
        private const val BOTTOM_RIGHT_CORNER = "┘"
        private const val BORDER_CONTENT_LENGTH = 110
        private val TOP_BORDER = createBorder(TOP_CORNER, "─", TOP_RIGHT_CORNER)
        private val MIDDLE_BORDER = createBorder(MIDDLE_CORNER, "┄", MIDDLE_RIGHT_CORNER)
        private val BOTTOM_BORDER = createBorder(BOTTOM_CORNER, "─", BOTTOM_RIGHT_CORNER)
        private const val MAX_LEN = 1100 // fit for Chinese character
        private const val NOTHING = "log nothing"
        private const val NULL = "null"
        private const val ARGS = "args"
        private const val PLACEHOLDER = " "
        val config: Config = Config()

        private var simpleDateFormat: SimpleDateFormat? = null

        private val EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()

        private val I_FORMATTER_MAP = SimpleArrayMap<Class<*>?, IFormatter<*>>()

        fun v(vararg contents: Any?) {
            log(V, config.globalTag, *contents)
        }

        fun vTag(tag: String, vararg contents: Any?) {
            log(V, tag, *contents)
        }

        fun d(vararg contents: Any?) {
            log(D, config.globalTag, *contents)
        }

        fun dTag(tag: String, vararg contents: Any?) {
            log(D, tag, *contents)
        }

        fun i(vararg contents: Any?) {
            log(I, config.globalTag, *contents)
        }

        fun iTag(tag: String, vararg contents: Any?) {
            log(I, tag, *contents)
        }

        fun w(vararg contents: Any?) {
            log(W, config.globalTag, *contents)
        }

        fun wTag(tag: String, vararg contents: Any?) {
            log(W, tag, *contents)
        }

        fun e(vararg contents: Any?) {
            log(E, config.globalTag, *contents)
        }

        fun eTag(tag: String, vararg contents: Any?) {
            log(E, tag, *contents)
        }

        fun a(vararg contents: Any?) {
            log(A, config.globalTag, *contents)
        }

        fun aTag(tag: String, vararg contents: Any?) {
            log(A, tag, *contents)
        }

        fun file(content: Any?) {
            log(
                FILE or D,
                config.globalTag, content
            )
        }

        fun file(@TYPE type: Int, content: Any?) {
            log(
                FILE or type,
                config.globalTag, content
            )
        }

        fun file(tag: String, content: Any?) {
            log(FILE or D, tag, content)
        }

        fun file(@TYPE type: Int, tag: String, content: Any?) {
            log(FILE or type, tag, content)
        }

        fun json(content: Any?) {
            log(
                JSON or D,
                config.globalTag, content
            )
        }

        fun json(@TYPE type: Int, content: Any?) {
            log(
                JSON or type,
                config.globalTag, content
            )
        }

        fun json(tag: String, content: Any?) {
            val realtag = if (config.globalTag.isEmpty()) tag else config.globalTag.plus("-$tag")
            log(JSON or D, realtag, content)
        }

        fun json(@TYPE type: Int, tag: String, content: Any?) {
            val realtag = if (config.globalTag.isEmpty()) tag else config.globalTag.plus("-$tag")
            log(JSON or type, realtag, content)
        }

        fun xml(content: String?) {
            log(
                XML or D,
                config.globalTag, content
            )
        }

        fun xml(@TYPE type: Int, content: String?) {
            log(
                XML or type,
                config.globalTag, content
            )
        }

        fun xml(tag: String, content: String?) {
            log(XML or D, tag, content)
        }

        fun xml(@TYPE type: Int, tag: String, content: String?) {
            log(XML or type, tag, content)
        }

        fun log(type: Int, tag: String, vararg contents: Any?) {
            if (!config.isLogSwitch) return
            val type_low = type and 0x0f
            val type_high = type and 0xf0
            if (config.isLog2ConsoleSwitch || config.isLog2FileSwitch || type_high == FILE) {
                if (type_low < config.mConsoleFilter && type_low < config.mFileFilter) return
                val tagHead = processTagAndHead(tag)
                val body = processBody(type_high, *contents)
                if (config.isLog2ConsoleSwitch && type_high != FILE && type_low >= config.mConsoleFilter) {
                    print2Console(type_low, tagHead.tag, tagHead.consoleHead, body)
                }
                if ((config.isLog2FileSwitch || type_high == FILE) && type_low >= config.mFileFilter) {
                    EXECUTOR.execute {
                        print2File(
                            type_low,
                            tagHead.tag,
                            tagHead.fileHead + body
                        )
                    }
                }
            }
        }

        val currentLogFilePath: String
            get() = getCurrentLogFilePath(Date())

        val logFiles: List<File>
            get() {
                val logDir = File(config.dir)
                if (!logDir.exists()) return emptyList()
                return logDir.listFiles { _, name -> isMatchLogFileName(name) }?.toList() ?: emptyList()
            }
        private fun processTagAndHead(initialTag: String): TagHead {
            var tag = initialTag
            if (!config.mTagIsSpace && !config.isLogHeadSwitch) {
                return TagHead(config.globalTag, null, ": ")
            }

            val stackTrace = Throwable().stackTrace
            val stackIndex = 3 + config.stackOffset
            if (stackIndex >= stackTrace.size) {
                val targetElement = stackTrace.getOrElse(3) { return TagHead(tag, null, ": ") }
                val fileName = getFileName(targetElement)

                if (config.mTagIsSpace && UtilsBridge.isSpace(tag)) {
                    tag = fileName.substringBefore('.', fileName)
                }
                return TagHead(tag, null, ": ")
            }

            val targetElement = stackTrace[stackIndex]
            val fileName = getFileName(targetElement)

            if (config.mTagIsSpace && UtilsBridge.isSpace(tag)) {
                tag = fileName.substringBefore('.', fileName)
            }

            if (!config.isLogHeadSwitch) {
                return TagHead(tag, null, ": ")
            }

            val threadName = Thread.currentThread().name
            val head = "%s, %s.%s(%s:%d)".format(
                threadName,
                targetElement.className,
                targetElement.methodName,
                fileName,
                targetElement.lineNumber
            )
            val fileHead = " [$head]: "

            if (config.stackDeep <= 1) {
                return TagHead(tag, arrayOf(head), fileHead)
            }

            val maxDepth = min(config.stackDeep, stackTrace.size - stackIndex)
            val consoleHead = Array(maxDepth) { i ->
                if (i == 0) {
                    head
                } else {
                    val element = stackTrace[stackIndex + i]
                    val space = " ".repeat(threadName.length + 2)
                    "%s%s.%s(%s:%d)".format(
                        space,
                        element.className,
                        element.methodName,
                        getFileName(element),
                        element.lineNumber
                    )
                }
            }

            return TagHead(tag, consoleHead, fileHead)
        }

        private fun getFileName(targetElement: StackTraceElement): String {
            val fileName = targetElement.fileName
            if (fileName != null) return fileName
            // If name of file is null, should add
            // "-keepattributes SourceFile,LineNumberTable" in proguard file.
            var className = targetElement.className
            val classNameInfo =
                className.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (classNameInfo.size > 0) {
                className = classNameInfo[classNameInfo.size - 1]
            }
            val index = className.indexOf('$')
            if (index != -1) {
                className = className.substring(0, index)
            }
            return "$className.java"
        }

        private fun processBody(type: Int, vararg contents: Any?): String {
            val body = if (contents.size == 1) {
                formatObject(type, contents[0])
            } else {
                val sb = StringBuilder()
                var i = 0
                val len = contents.size
                while (i < len) {
                    val content = contents[i]
                    sb.append(ARGS)
                        .append("[")
                        .append(i)
                        .append("]")
                        .append(" = ")
                        .append(formatObject(content))
                        .append(LINE_SEP)
                    ++i
                }
                sb.toString()
            }
            return if (body.length == 0) NOTHING else body
        }

        private fun formatObject(type: Int, obj: Any?): String {
            if (obj == null) return NULL
            if (type == JSON) return LogFormatter.object2String(obj, JSON)
            if (type == XML) return LogFormatter.object2String(obj, XML)
            return formatObject(obj)
        }

        private fun formatObject(obj: Any?): String {
            if (obj == null) return NULL
            if (!I_FORMATTER_MAP.isEmpty) {
                val iFormatter = I_FORMATTER_MAP[getClassFromObject(
                    obj
                )]
                if (iFormatter != null) {
                    return iFormatter.format(obj)
                }
            }
            return LogFormatter.object2String(obj)
        }

        private fun print2Console(
            type: Int,
            tag: String,
            head: Array<String>?,
            msg: String
        ) {
            if (config.isSingleTagSwitch) {
                printSingleTagMsg(type, tag, processSingleTagMsg(head, msg))
            } else {
                printBorder(type, tag, true)
                printHead(type, tag, head)
                printMsg(type, tag, msg)
                printBorder(type, tag, false)
            }
        }

        private fun printBorder(type: Int, tag: String, isTop: Boolean) {
            if (config.isLogBorderSwitch) {
                print2Console(type, tag, if (isTop) TOP_BORDER else BOTTOM_BORDER)
            }
        }

        private fun createBorder(left: String, divider: String, right: String): String {
            return left + divider.repeat(BORDER_CONTENT_LENGTH + 2) + right
        }

        private fun printHead(type: Int, tag: String, head: Array<String>?) {
            if (head != null) {
                for (aHead in head) {
                    formatBorderLines(aHead).forEach { line ->
                        print2Console(type, tag, line)
                    }
                }
                if (config.isLogBorderSwitch) print2Console(type, tag, MIDDLE_BORDER)
            }
        }

        private fun printMsg(type: Int, tag: String, msg: String) {
            val len = msg.length
            val countOfSub = len / MAX_LEN
            if (countOfSub > 0) {
                var index = 0
                for (i in 0 until countOfSub) {
                    printSubMsg(type, tag, msg.substring(index, index + MAX_LEN))
                    index += MAX_LEN
                }
                if (index != len) {
                    printSubMsg(type, tag, msg.substring(index, len))
                }
            } else {
                printSubMsg(type, tag, msg)
            }
        }

        private fun printSubMsg(type: Int, tag: String, msg: String) {
            if (!config.isLogBorderSwitch) {
                print2Console(type, tag, msg)
                return
            }
            val lines =
                msg.split(LINE_SEP.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                formatBorderLines(line).forEach { borderLine ->
                    print2Console(type, tag, borderLine)
                }
            }
        }

        private fun formatBorderLines(line: String): List<String> {
            if (!config.isLogBorderSwitch) return listOf(line)
            if (line.isEmpty()) return listOf(formatBorderLine(line))
            return line.chunkedByDisplayWidth(BORDER_CONTENT_LENGTH).map { formatBorderLine(it) }
        }

        private fun formatBorderLine(line: String): String {
            if (!config.isLogBorderSwitch) return line
            return LEFT_BORDER + line.padEndByDisplayWidth(BORDER_CONTENT_LENGTH) + RIGHT_BORDER
        }

        private fun String.chunkedByDisplayWidth(maxWidth: Int): List<String> {
            val result = mutableListOf<String>()
            val builder = StringBuilder()
            var width = 0
            var index = 0
            while (index < length) {
                val codePoint = codePointAt(index)
                val text = String(Character.toChars(codePoint))
                val textWidth = codePoint.displayWidth()
                if (builder.isNotEmpty() && width + textWidth > maxWidth) {
                    result.add(builder.toString())
                    builder.clear()
                    width = 0
                }
                builder.append(text)
                width += textWidth
                index += Character.charCount(codePoint)
            }
            if (builder.isNotEmpty()) result.add(builder.toString())
            return result
        }

        private fun String.padEndByDisplayWidth(width: Int): String {
            val padCount = width - displayWidth()
            return if (padCount > 0) this + " ".repeat(padCount) else this
        }

        private fun String.displayWidth(): Int {
            var width = 0
            var index = 0
            while (index < length) {
                val codePoint = codePointAt(index)
                width += codePoint.displayWidth()
                index += Character.charCount(codePoint)
            }
            return width
        }

        private fun Int.displayWidth(): Int {
            if (this == '\t'.code) return 4
            val type = Character.getType(this)
            if (
                type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt() ||
                type == Character.FORMAT.toInt()
            ) return 0
            val block = Character.UnicodeBlock.of(this)
            return if (
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                block == Character.UnicodeBlock.HIRAGANA ||
                block == Character.UnicodeBlock.KATAKANA ||
                block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                block == Character.UnicodeBlock.HANGUL_JAMO ||
                block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO ||
                type == Character.OTHER_SYMBOL.toInt()
            ) 2 else 1
        }

        private fun processSingleTagMsg(
            head: Array<String>?,
            msg: String
        ): String {
            val sb = StringBuilder()
            if (config.isLogBorderSwitch) {
                sb.append(PLACEHOLDER).append(LINE_SEP)
                sb.append(TOP_BORDER).append(LINE_SEP)
                if (head != null) {
                    for (aHead in head) {
                        appendBorderLines(sb, aHead)
                    }
                    sb.append(MIDDLE_BORDER).append(LINE_SEP)
                }
                for (line in msg.split(LINE_SEP.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    appendBorderLines(sb, line)
                }
                sb.append(BOTTOM_BORDER)
            } else {
                if (head != null) {
                    sb.append(PLACEHOLDER).append(LINE_SEP)
                    for (aHead in head) {
                        sb.append(aHead).append(LINE_SEP)
                    }
                }
                sb.append(msg)
            }
            return sb.toString()
        }

        private fun appendBorderLines(sb: StringBuilder, line: String) {
            formatBorderLines(line).forEach {
                sb.append(it).append(LINE_SEP)
            }
        }

        private fun printSingleTagMsg(type: Int, tag: String, msg: String) {
            if (config.isLogBorderSwitch) {
                printBorderMsgByLine(type, tag, msg)
                return
            }
            val len = msg.length
            val countOfSub = len / MAX_LEN
            if (countOfSub > 0) {
                print2Console(type, tag, msg.substring(0, MAX_LEN))
                var index = MAX_LEN
                for (i in 1 until countOfSub) {
                    print2Console(
                        type, tag,
                        PLACEHOLDER + LINE_SEP + msg.substring(index, index + MAX_LEN)
                    )
                    index += MAX_LEN
                }
                if (index != len) {
                    print2Console(type, tag, PLACEHOLDER + LINE_SEP + msg.substring(index, len))
                }
            } else {
                print2Console(type, tag, msg)
            }
        }

        private fun printBorderMsgByLine(type: Int, tag: String, msg: String) {
            val builder = StringBuilder()
            msg.split(LINE_SEP).forEach { line ->
                val nextLen = if (builder.isEmpty()) line.length else builder.length + LINE_SEP.length + line.length
                if (builder.isNotEmpty() && nextLen > MAX_LEN) {
                    print2Console(type, tag, builder.toString())
                    builder.clear()
                }
                if (builder.isNotEmpty()) builder.append(LINE_SEP)
                builder.append(line)
            }
            if (builder.isNotEmpty()) print2Console(type, tag, builder.toString())
        }

        private fun print2Console(type: Int, tag: String, msg: String) {
            Log.println(type, tag, msg)
            if (config.onConsoleOutputListener != null) {
                config.onConsoleOutputListener?.invoke(type, tag, msg)
            }
        }

        private fun print2File(type: Int, tag: String, msg: String) {
            val d = Date()
            val format = sdf.format(d)
            val date = format.substring(0, 10)
            val currentLogFilePath = getCurrentLogFilePath(d)
            if (!createOrExistsFile(currentLogFilePath, date)) {
                Log.e("LogUtils", "create $currentLogFilePath failed!")
                return
            }
            val time = format.substring(11)
            val content = time +
                    T[type - V] +
                    "/" +
                    tag +
                    msg +
                    LINE_SEP
            input2File(currentLogFilePath, content)
        }

        private fun getCurrentLogFilePath(d: Date): String {
            val format = sdf.format(d)
            val date = format.substring(0, 10)
            return (config.dir + config.filePrefix + "_"
                    + date + "_" +
                    config.processName + config.fileExtension)
        }


        private val sdf: SimpleDateFormat
            get() {
                if (simpleDateFormat == null) {
                    simpleDateFormat = SimpleDateFormat(
                        "yyyy_MM_dd HH:mm:ss.SSS ",
                        Locale.getDefault()
                    )
                }
                return simpleDateFormat!!
            }

        private fun createOrExistsFile(filePath: String, date: String): Boolean {
            val file = File(filePath)
            if (file.exists()) {
                val isF = file.isFile
                Log.d("LogUtils", "file exists, isFile=$isF: $filePath")
                return isF
            }
            val parent = file.parentFile
            val dirOk = UtilsBridge.createOrExistsDir(parent)
            if (!dirOk) {
                Log.e("LogUtils", "mkdirs failed for parent: $parent (exists=${parent?.exists()}, canWrite=${parent?.canWrite()})")
                return false
            }
            try {
                val isCreate = file.createNewFile()
                if (!isCreate) {
                    Log.e("LogUtils", "createNewFile returned false: $filePath")
                }
                if (isCreate) {
                    printDeviceInfo(filePath, date)
                    deleteDueLogs(config.dir, date)
                }
                return isCreate
            } catch (e: IOException) {
                Log.e("LogUtils", "IOException creating file: $filePath", e)
                return false
            }
        }

        private fun deleteDueLogs(filePath: String, date: String) {
            if (config.saveDays <= 0) return

            val logRoot = getLogRoot(File(filePath))
            val dueDate = getDueDate(date) ?: return
            logRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val logDay = findDate(file.name)
                    if (!isMatchLogFileName(file.name, logDay)) return@forEach

                    if (logDay <= dueDate) {
                        if (!file.delete()) {
                            Log.e("LogUtils", "Failed to delete $file!")
                        } else {
                            Log.d("LogUtils", "Successfully deleted $file!")
                        }
                    }
                }

            deleteEmptyLogDirs(logRoot)
        }

        private fun getLogRoot(logDir: File): File {
            val dirName = logDir.name
            return if (LOG_DATE_PATTERN.matcher(dirName).matches()) {
                logDir.parentFile ?: logDir
            } else {
                logDir
            }
        }

        private fun getDueDate(date: String): String? {
            return try {
                val calendar = Calendar.getInstance()
                calendar.time = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).parse(date) ?: return null
                calendar.add(Calendar.DAY_OF_YEAR, -config.saveDays)
                SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(calendar.time)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun deleteEmptyLogDirs(logRoot: File) {
            logRoot.walkBottomUp()
                .filter { it.isDirectory && it != logRoot && it.list()?.isEmpty() == true }
                .forEach { dir ->
                    if (!dir.delete()) {
                        Log.e("LogUtils", "Failed to delete empty dir $dir!")
                    } else {
                        Log.d("LogUtils", "Successfully deleted empty dir $dir!")
                    }
                }
        }

        private fun isMatchLogFileName(name: String): Boolean {
            return isMatchLogFileName(name, findDate(name))
        }

        private fun isMatchLogFileName(name: String, logDay: String): Boolean {
            return logDay.isNotEmpty() && name.endsWith(config.fileExtension)
        }

        private fun findDate(str: String): String {
            val matcher = LOG_DATE_PATTERN.matcher(str)
            if (matcher.find()) {
                return matcher.group()
            }
            return ""
        }

        private fun printDeviceInfo(filePath: String, date: String) {
            config.mFileHead.addFirst("创建日期", date)
            input2File(filePath, config.mFileHead.toString())
        }

        private fun input2File(filePath: String, input: String) {
            config.mFileWriter?.write(filePath, input)
                ?: UtilsBridge.writeFileFromString(filePath, input, append = true)
            config.mOnFileOutputListener?.invoke(filePath, input)
        }


        private fun <T> getTypeClassFromParadigm(formatter: IFormatter<T>): Class<*>? {
            val type = when {
                formatter.javaClass.genericInterfaces.size == 1 -> formatter.javaClass.genericInterfaces[0]
                else -> formatter.javaClass.genericSuperclass
            }.let { rawType ->
                generateSequence(rawType as ParameterizedType) {
                    if (it.actualTypeArguments[0] is ParameterizedType) it.actualTypeArguments[0] as ParameterizedType
                    else null
                }.last().actualTypeArguments[0]
            }

            val className = type.toString()
                .removePrefix("class ")
                .removePrefix("interface ")

            return try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }
        private fun getClassFromObject(obj: Any): Class<*> {
            val objClass = obj.javaClass
            if (!objClass.isAnonymousClass && !objClass.isSynthetic) {
                return objClass
            }

            val type = when {
                objClass.genericInterfaces.size == 1 -> objClass.genericInterfaces[0] // Interface
                else -> objClass.genericSuperclass // Abstract class or lambda
            }.let { rawType ->
                generateSequence(rawType) { if (it is ParameterizedType) it.rawType else null }.last()
            }

            val className = type.toString()
                .removePrefix("class ")
                .removePrefix("interface ")

            return try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                objClass
            }
        }
    }
}

package com.opencalc.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("opencalc", Context.MODE_PRIVATE) }
    private var dark = true
    private var screen = "calc"
    private var expression = ""
    private var memory = 0.0
    private val history = mutableListOf<String>()

    private var bg = 0
    private var panel = 0
    private var fg = 0
    private var muted = 0
    private var accent = 0
    private var key = 0

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var expressionView: EditText
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = prefs.getBoolean("dark", true)
        history.addAll(
            prefs.getString("history", "")!!
                .split("\n").filter { it.isNotBlank() }.take(100)
        )
        build()
    }

    private fun build() {
        if (dark) {
            bg = Color.rgb(17, 20, 28)
            panel = Color.rgb(30, 35, 47)
            fg = Color.WHITE
            muted = Color.rgb(180, 190, 207)
            accent = Color.rgb(64, 120, 245)
            key = Color.rgb(43, 49, 64)
        } else {
            bg = Color.rgb(246, 248, 252)
            panel = Color.WHITE
            fg = Color.rgb(25, 31, 43)
            muted = Color.rgb(91, 102, 120)
            accent = Color.rgb(36, 99, 235)
            key = Color.rgb(230, 235, 243)
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            TextView(this).apply {
                text = "OpenCalc"
                textSize = 27f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(fg)
            },
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )

        header.addView(
            btn(if (dark) "☀" else "☾", accent) {
                dark = !dark
                prefs.edit().putBoolean("dark", dark).apply()
                build()
            },
            LinearLayout.LayoutParams(dp(52), dp(42))
        )
        root.addView(header)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }

        listOf(
            "calc" to "ગણતરી",
            "convert" to "કન્વર્ટ",
            "finance" to "નાણાં",
            "history" to "હિસ્ટ્રી"
        ).forEach { (id, label) ->
            nav.addView(
                btn(label, if (screen == id) accent else key) {
                    screen = id
                    build()
                },
                LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginEnd = dp(3)
                }
            )
        }
        root.addView(nav)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        when (screen) {
            "calc" -> calculator()
            "convert" -> converter()
            "finance" -> finance()
            else -> historyScreen()
        }

        setContentView(root)
    }

    private fun calculator() {
        expressionView = EditText(this).apply {
            setText(expression)
            textSize = 23f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(fg)
            setHintTextColor(muted)
            hint = "ગણતરી દાખલ કરો"
            setSingleLine(true)
            background = round(panel, dp(12))
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        content.addView(expressionView, LinearLayout.LayoutParams(-1, dp(54)))

        resultView = TextView(this).apply {
            text = "પરિણામ અહીં દેખાશે"
            textSize = 22f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(accent)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        content.addView(resultView, LinearLayout.LayoutParams(-1, dp(42)))

        // મોટી મેમરી બટન લાઇન
        val mem = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf("MC", "MR", "M+", "M−", "⌫", "AC").forEach { label ->
            mem.addView(
                btn(label, key) {
                    when (label) {
                        "MC" -> memory = 0.0
                        "MR" -> insert(fmt(memory))
                        "M+" -> memory += runCatching {
                            Parser(expressionView.text.toString()).parse()
                        }.getOrDefault(0.0)
                        "M−" -> memory -= runCatching {
                            Parser(expressionView.text.toString()).parse()
                        }.getOrDefault(0.0)
                        "⌫" -> {
                            val s = expressionView.text.toString()
                            expressionView.setText(s.dropLast(1))
                            expressionView.setSelection(expressionView.text.length)
                            expression = expressionView.text.toString()
                        }
                        "AC" -> {
                            expression = ""
                            expressionView.setText("")
                            resultView.text = "પરિણામ અહીં દેખાશે"
                        }
                    }
                },
                LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    marginEnd = dp(3)
                }
            )
        }
        content.addView(
            mem,
            LinearLayout.LayoutParams(-1, dp(44)).apply {
                bottomMargin = dp(2)
            }
        )

        val keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rows = listOf(
            listOf("sin(", "cos(", "tan(", "sqrt("),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "−"),
            listOf("(", "0", ".", "+"),
            listOf("π", "e", "^", "%"),
            listOf("log(", "ln(", "!", ")")
        )

        rows.forEachIndexed { index, labels ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            labels.forEach { label ->
                val operator = label in listOf("÷", "×", "−", "+", "^", "%")
                row.addView(
                    btn(label, if (operator) accent else key) {
                        insert(
                            when (label) {
                                "÷" -> "/"
                                "×" -> "*"
                                "−" -> "-"
                                "π" -> "pi"
                                else -> label
                            }
                        )
                    },
                    LinearLayout.LayoutParams(0, -1, 1f).apply {
                        marginStart = dp(2)
                        marginEnd = dp(2)
                        topMargin = dp(1)
                        bottomMargin = dp(1)
                    }
                )
            }

            // મુખ્ય અંકવાળી પંક્તિઓ થોડી નાની
            // વૈજ્ઞાનિક અને વધારાના બટનોની પંક્તિઓ પણ ટૂંકી
            val weight = when (index) {
                0, 5, 6 -> 0.72f
                else -> 0.88f
            }
            keypad.addView(row, LinearLayout.LayoutParams(-1, 0, weight))
        }

        content.addView(
            keypad,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(1)
                bottomMargin = dp(2)
            }
        )

        // નીચે Copy ડાબી બાજુ અને મોટું = જમણી બાજુ
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        bottom.addView(
            btn("Copy", key) {
                val cb = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(
                    ClipData.newPlainText("OpenCalc", resultView.text)
                )
                Toast.makeText(this, "Copy થયું", Toast.LENGTH_SHORT).show()
            },
            LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginEnd = dp(5)
            }
        )

        bottom.addView(
            btn("=", accent) {
                expression = expressionView.text.toString()
                try {
                    val value = Parser(expression).parse()
                    if (!value.isFinite()) throw IllegalArgumentException()

                    val answer = fmt(value)
                    resultView.text = answer

                    history.add(0, "$expression = $answer")
                    while (history.size > 100) history.removeAt(history.lastIndex)

                    prefs.edit()
                        .putString("history", history.joinToString("\n"))
                        .apply()

                    expression = answer
                    expressionView.setText(answer)
                    expressionView.setSelection(expressionView.text.length)
                } catch (_: Exception) {
                    resultView.text = "ગણતરીમાં ભૂલ"
                }
            },
            LinearLayout.LayoutParams(0, dp(52), 1.7f)
        )

        content.addView(
            bottom,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(3)
            }
        )
    }

    private fun insert(value: String) {
        val field = expressionView
        val start = field.selectionStart.coerceAtLeast(0)
        val end = field.selectionEnd.coerceAtLeast(start)
        val old = field.text.toString()
        val next = old.substring(0, start) + value + old.substring(end)

        field.setText(next)
        field.setSelection((start + value.length).coerceAtMost(next.length))
        expression = next
    }

    private fun converter() {
        title("યુનિટ કન્વર્ટર")

        val cats = listOf(
            "Length", "Weight", "Temperature", "Area",
            "Volume", "Speed", "Time", "Data"
        )

        val units = mapOf(
            "Length" to listOf("m", "km", "cm", "mm", "mile", "ft", "inch"),
            "Weight" to listOf("kg", "g", "mg", "lb", "oz"),
            "Temperature" to listOf("C", "F", "K"),
            "Area" to listOf("m²", "km²", "ft²", "acre", "hectare"),
            "Volume" to listOf("L", "mL", "m³", "gal"),
            "Speed" to listOf("m/s", "km/h", "mph"),
            "Time" to listOf("s", "min", "h", "day"),
            "Data" to listOf("B", "KB", "MB", "GB", "TB")
        )

        val cat = Spinner(this)
        cat.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, cats
        )
        content.addView(cat, params(dp(46)))

        val amount = field("મૂલ્ય દાખલ કરો", "1")
        content.addView(amount, params(dp(54)))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val from = Spinner(this)
        val to = Spinner(this)

        fun refresh() {
            val us = units[cats[cat.selectedItemPosition]]!!
            from.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, us
            )
            to.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, us
            )
            if (us.size > 1) to.setSelection(1)
        }

        cat.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}

                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?,
                    v: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    refresh()
                }
            }

        refresh()
        row.addView(from, LinearLayout.LayoutParams(0, dp(48), 1f))
        row.addView(to, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(row)

        val out = TextView(this).apply {
            text = "પરિણામ અહીં દેખાશે"
            textSize = 21f
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        }
        content.addView(out, params(dp(68)))

        content.addView(
            btn("કન્વર્ટ કરો", accent) {
                try {
                    val a = amount.text.toString().toDouble()
                    val f = from.selectedItem.toString()
                    val t = to.selectedItem.toString()
                    out.text =
                        "${fmt(a)} $f = ${fmt(convert(cats[cat.selectedItemPosition], a, f, t))} $t"
                } catch (_: Exception) {
                    out.text = "મૂલ્ય તપાસો"
                }
            },
            params(dp(48))
        )

        note("Length, Weight, Temperature, Area, Volume, Speed, Time અને Data રૂપાંતરણ.")
    }

    private fun convert(cat: String, v: Double, f: String, t: String): Double {
        if (f == t) return v

        if (cat == "Temperature") {
            val c = when (f) {
                "C" -> v
                "F" -> (v - 32) * 5 / 9
                else -> v - 273.15
            }
            return when (t) {
                "C" -> c
                "F" -> c * 9 / 5 + 32
                else -> c + 273.15
            }
        }

        val factors = when (cat) {
            "Length" -> mapOf(
                "m" to 1.0, "km" to 1000.0, "cm" to .01,
                "mm" to .001, "mile" to 1609.344,
                "ft" to .3048, "inch" to .0254
            )
            "Weight" -> mapOf(
                "kg" to 1.0, "g" to .001, "mg" to .000001,
                "lb" to .45359237, "oz" to .028349523125
            )
            "Area" -> mapOf(
                "m²" to 1.0, "km²" to 1e6, "ft²" to .09290304,
                "acre" to 4046.8564224, "hectare" to 10000.0
            )
            "Volume" -> mapOf(
                "L" to 1.0, "mL" to .001,
                "m³" to 1000.0, "gal" to 3.785411784
            )
            "Speed" -> mapOf(
                "m/s" to 1.0, "km/h" to (1.0 / 3.6), "mph" to .44704
            )
            "Time" -> mapOf(
                "s" to 1.0, "min" to 60.0,
                "h" to 3600.0, "day" to 86400.0
            )
            else -> mapOf(
                "B" to 1.0, "KB" to 1000.0, "MB" to 1e6,
                "GB" to 1e9, "TB" to 1e12
            )
        }

        return v * (factors[f] ?: 1.0) / (factors[t] ?: 1.0)
    }

    private fun finance() {
        title("નાણાકીય ગણતરી")

        val modes = listOf(
            "GST ઉમેરો", "GST કાઢો", "Simple Interest",
            "Compound Interest", "Discount"
        )

        val mode = Spinner(this)
        mode.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, modes
        )
        content.addView(mode, params(dp(46)))

        val amount = field("રકમ / મૂડી (₹)", "10000")
        val rate = field("દર (%)", "18")
        val years = field("સમય (વર્ષ)", "1")

        content.addView(amount, params(dp(52)))
        content.addView(rate, params(dp(52)))
        content.addView(years, params(dp(52)))

        val out = TextView(this).apply {
            text = "વિગતો ભરો"
            textSize = 18f
            setTextColor(accent)
            setPadding(0, dp(12), 0, dp(12))
        }

        content.addView(
            btn("ગણતરી કરો", accent) {
                try {
                    val p = amount.text.toString().toDouble()
                    val r = rate.text.toString().toDouble() / 100
                    val y = years.text.toString().toDouble()

                    out.text = when (mode.selectedItemPosition) {
                        0 -> "GST સહિત કુલ: ₹${fmt(p * (1 + r))}\nGST રકમ: ₹${fmt(p * r)}"
                        1 -> "GST પહેલાં: ₹${fmt(p / (1 + r))}\nGST રકમ: ₹${fmt(p - p / (1 + r))}"
                        2 -> "વ્યાજ: ₹${fmt(p * r * y)}\nકુલ: ₹${fmt(p * (1 + r * y))}"
                        3 -> "ચક્રવૃદ્ધિ વ્યાજ: ₹${fmt(p * (1 + r).pow(y) - p)}\nકુલ: ₹${fmt(p * (1 + r).pow(y))}"
                        else -> "ડિસ્કાઉન્ટ: ₹${fmt(p * r)}\nચૂકવવાની રકમ: ₹${fmt(p * (1 - r))}"
                    }
                } catch (_: Exception) {
                    out.text = "બધી વિગતો યોગ્ય રીતે દાખલ કરો"
                }
            },
            params(dp(48))
        )

        content.addView(out)
        note("GST, ડિસ્કાઉન્ટ, સાદું અને ચક્રવૃદ્ધિ વ્યાજ. દર ટકા અને સમય વર્ષમાં દાખલ કરો.")
    }

    private fun historyScreen() {
        title("ગણતરીની હિસ્ટ્રી")

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(
            btn("હિસ્ટ્રી Copy", key) {
                val cb = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(
                    ClipData.newPlainText(
                        "OpenCalc history", history.joinToString("\n")
                    )
                )
                Toast.makeText(this, "હિસ્ટ્રી Copy થઈ", Toast.LENGTH_SHORT).show()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        row.addView(
            btn("બધી કાઢો", key) {
                history.clear()
                prefs.edit().putString("history", "").apply()
                build()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )
        content.addView(row)

        if (history.isEmpty()) note("હજુ કોઈ ગણતરી સાચવાઈ નથી.")

        history.forEach { entry ->
            content.addView(
                TextView(this).apply {
                    text = entry
                    textSize = 16f
                    setTextColor(fg)
                    setPadding(dp(12), dp(14), dp(12), dp(14))
                    background = round(panel, dp(10))
                    setOnClickListener {
                        expression = entry.substringBefore(" = ")
                        screen = "calc"
                        build()
                    }
                },
                params(dp(50))
            )
        }
    }

    private fun title(s: String) {
        content.addView(
            TextView(this).apply {
                text = s
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(fg)
                setPadding(0, dp(8), 0, dp(10))
            }
        )
    }

    private fun note(s: String) {
        content.addView(
            TextView(this).apply {
                text = s
                textSize = 13f
                setTextColor(muted)
                setPadding(dp(8), dp(12), dp(8), dp(8))
            }
        )
    }

    private fun field(h: String, initial: String) =
        EditText(this).apply {
            hint = h
            setText(initial)
            textSize = 17f
            setTextColor(fg)
            setHintTextColor(muted)
            setSingleLine(true)
            background = round(panel, dp(10))
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }

    private fun btn(label: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(if (color == key) fg else Color.WHITE)
            background = round(color, dp(11))
            setOnClickListener { action() }
        }

    private fun round(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun params(h: Int) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
        }

    private fun dp(n: Int) =
        (n * resources.displayMetrics.density).toInt()

    private fun fmt(v: Double): String {
        if (abs(v) < 1e-12) return "0"
        return DecimalFormat("0.##########").format(v)
    }

    private class Parser(input: String) {
        private val s = input.replace(" ", "").lowercase(Locale.US)
        private var p = 0

        fun parse(): Double {
            if (s.isBlank()) error("empty")
            val v = expr()
            if (p < s.length) error("extra")
            return v
        }

        private fun expr(): Double {
            var v = term()
            while (p < s.length && s[p] in charArrayOf('+', '-')) {
                val op = s[p++]
                val b = term()
                v = if (op == '+') v + b else v - b
            }
            return v
        }

        private fun term(): Double {
            var v = power()
            while (p < s.length && s[p] in charArrayOf('*', '/')) {
                val op = s[p++]
                val b = power()
                v = if (op == '*') v * b else v / b
            }
            return v
        }

        private fun power(): Double {
            var v = unary()
            if (p < s.length && s[p] == '^') {
                p++
                v = v.pow(power())
            }
            return v
        }

        private fun unary(): Double {
            if (p < s.length && s[p] == '+') {
                p++
                return unary()
            }
            if (p < s.length && s[p] == '-') {
                p++
                return -unary()
            }

            var v = primary()
            while (p < s.length) {
                when (s[p]) {
                    '%' -> {
                        p++
                        v /= 100.0
                    }
                    '!' -> {
                        p++
                        if (v < 0 || v > 170 || v % 1.0 != 0.0) {
                            error("factorial")
                        }
                        var f = 1.0
                        for (i in 2..v.toInt()) f *= i
                        v = f
                    }
                    else -> return v
                }
            }
            return v
        }

        private fun primary(): Double {
            if (p >= s.length) error("missing")

            if (s[p] == '(') {
                p++
                val v = expr()
                expect(')')
                return v
            }

            if (s[p].isDigit() || s[p] == '.') {
                val start = p
                while (
                    p < s.length &&
                    (s[p].isDigit() || s[p] == '.')
                ) {
                    p++
                }
                return s.substring(start, p).toDouble()
            }

            if (s[p].isLetter()) {
                val start = p
                while (p < s.length && s[p].isLetter()) p++
                val n = s.substring(start, p)

                if (n == "pi") return PI
                if (n == "e") return E

                expect('(')
                val x = expr()
                expect(')')

                return when (n) {
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "sqrt" -> sqrt(x)
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> error("function")
                }
            }

            error("character")
        }

        private fun expect(c: Char) {
            if (p >= s.length || s[p] != c) error("expected")
            p++
        }
    }
}

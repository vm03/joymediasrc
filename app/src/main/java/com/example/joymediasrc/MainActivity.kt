package com.example.joymediasrc

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем простой интерфейс программно, чтобы не мучаться с layout XML
        val context = this
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val title = android.widget.TextView(context).apply {
            text = "Выберите источник звука при запуске машины:"
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }
        layout.addView(title)

        val radioGroup = RadioGroup(context)

        // Список доступных интентов из вашей магнитолы
        val sources = mapOf(
            "Bluetooth" to "com.bw.intent.action.BTAUDIO",
            "USB" to "com.bw.mediasource.usb",
            "Радио" to "com.bw.intent.action.RADIO",
        )

        val prefs = getSharedPreferences("AutoSourcePrefs", Context.MODE_PRIVATE)
        val savedAction = prefs.getString("selected_action", "com.bw.intent.action.BTAUDIO")

        // Динамически создаем кнопки
        for ((name, action) in sources) {
            val radioButton = RadioButton(context).apply {
                text = name
                tag = action
                id = android.view.View.generateViewId()
                if (action == savedAction) isChecked = true
            }
            radioGroup.addView(radioButton)
        }
        layout.addView(radioGroup)

        val saveButton = Button(context).apply {
            text = "Сохранить настройки"
            setOnClickListener {
                val checkedId = radioGroup.checkedRadioButtonId
                val selectedRadio = radioGroup.findViewById<RadioButton>(checkedId)
                val actionToSave = selectedRadio?.tag as? String ?: "com.bw.intent.action.BTAUDIO"

                prefs.edit().putString("selected_action", actionToSave).apply()
                Toast.makeText(context, "Настройки сохранены! Выбран: ${selectedRadio?.text}", Toast.LENGTH_LONG).show()
                finish() // Закрываем приложение
            }
        }
        layout.addView(saveButton)

        setContentView(layout)
    }
}

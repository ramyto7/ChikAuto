package com.example.chikauto.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AppDateField(
    label: String,
    value: String,
    onDateSelected: (text: String, millis: Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val selectedCalendar = Calendar.getInstance()
                        selectedCalendar.set(year, month, day, 0, 0, 0)
                        selectedCalendar.set(Calendar.MILLISECOND, 0)

                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                        val text = formatter.format(selectedCalendar.time)

                        onDateSelected(text, selectedCalendar.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
    )
}
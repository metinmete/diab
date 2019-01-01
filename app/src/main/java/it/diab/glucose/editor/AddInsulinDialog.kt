/*
 * Copyright (c) 2018 Bevilacqua Joey
 *
 * Licensed under the GNU GPLv3 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl.txt
 */
package it.diab.glucose.editor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.widget.AppCompatSpinner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import it.diab.R
import it.diab.db.entities.Glucose
import it.diab.db.entities.Insulin
import it.diab.util.UIUtils
import it.diab.util.extensions.asTimeFrame
import java.util.Date

@SuppressLint("InflateParams")
class AddInsulinDialog(
    private val activity: Activity,
    private val glucose: Glucose,
    private val isFirst: Boolean
) {
    private val dialog = BottomSheetDialog(activity)

    private val nameSpinner: AppCompatSpinner
    private val valueEditText: EditText
    private val addButton: MaterialButton
    private val removeButton: MaterialButton

    private lateinit var insulins: Array<Insulin>

    init {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_insulin_to_glucose, null)

        nameSpinner = view.findViewById(R.id.glucose_editor_insulin_spinner)
        valueEditText = view.findViewById(R.id.glucose_editor_insulin_value)
        addButton = view.findViewById(R.id.glucose_editor_insulin_add)
        removeButton = view.findViewById(R.id.glucose_editor_insulin_remove)

        dialog.setContentView(view)
    }

    fun setInsulins(list: List<Insulin>) {
        insulins = list.toTypedArray()
        val names = Array(list.size) { "" }

        val now = (if (glucose.date.time == 0L) Date() else glucose.date).asTimeFrame()

        var spinnerPosition = -1
        val currentId = if (isFirst) glucose.insulinId0 else glucose.insulinId1
        val currentValue = if (isFirst) glucose.insulinValue0 else glucose.insulinValue1

        if (currentValue != 0f) {
            valueEditText.setText(currentValue.toString())
        }

        for (i in insulins.indices) {
            names[i] = "${insulins[i].name} (${activity.getString(insulins[i].timeFrame.string)})"

            if (spinnerPosition == -1 && (insulins[i].uid == currentId ||
                            insulins[i].timeFrame === now)
            ) {
                spinnerPosition = i
            }
        }

        nameSpinner.adapter = ArrayAdapter<String>(
                activity,
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, names
        )

        nameSpinner.setSelection(if (spinnerPosition == -1) 0 else spinnerPosition)
    }

    fun show(onAdd: (Insulin, Float) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
        addButton.setOnClickListener {
            val selected = insulins[nameSpinner.selectedItemPosition]
            val value = valueEditText.text.toString().toFloatOrNull() ?: 0F

            onAdd(selected, value)
            dialog.dismiss()
        }
        removeButton.setOnClickListener {
            onRemove()
            dialog.dismiss()
        }
        dialog.setOnDismissListener { onDismiss() }

        UIUtils.setWhiteNavBarIfNeeded(activity, dialog)
        dialog.show()
    }
}

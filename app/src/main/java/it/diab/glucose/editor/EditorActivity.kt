/*
 * Copyright (c) 2018 Bevilacqua Joey
 *
 * Licensed under the GNU GPLv3 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl.txt
 */
package it.diab.glucose.editor

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import it.diab.R
import it.diab.db.entities.Glucose
import it.diab.db.entities.Insulin
import it.diab.fit.BaseFitHandler
import it.diab.ui.EatBar
import it.diab.ui.InsulinSuggestionView
import it.diab.ui.NumericKeyboardView
import it.diab.util.PreferencesUtil
import it.diab.util.SystemUtil
import it.diab.util.VibrationUtil
import it.diab.util.extensions.asTimeFrame
import it.diab.util.extensions.get
import it.diab.util.extensions.getCalendar
import it.diab.util.extensions.getDetailedString
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

class EditorActivity : AppCompatActivity() {

    private lateinit var mViewModel: EditorViewModel

    private lateinit var mConstraintRoot: ConstraintLayout
    private lateinit var mValueView: TextView
    private lateinit var mDateView: TextView
    private lateinit var mEatBar: EatBar
    private lateinit var mInsulinView: TextView
    private lateinit var mBasalView: TextView
    private lateinit var mSuggestionView: InsulinSuggestionView
    private lateinit var mInfoView: TextView
    private lateinit var mKeyboardView: NumericKeyboardView
    private lateinit var mFab: FloatingActionButton

    private var mEditMode = false
    private var mErrorStatus = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glucose_editor)

        // Common views
        mConstraintRoot = findViewById(R.id.glucose_editor_root)
        mValueView = findViewById(R.id.glucose_editor_edit_value)
        mDateView = findViewById(R.id.glucose_editor_edit_date)
        mEatBar = findViewById(R.id.glucose_editor_eat_bar)
        mFab = findViewById(R.id.fab)

        // Edit views
        mKeyboardView = findViewById(R.id.glucose_editor_keyboard)

        // Show views
        mInsulinView = findViewById(R.id.glucose_editor_insulin_value)
        mBasalView = findViewById(R.id.glucose_editor_insulin_basal_value)
        mSuggestionView = findViewById(R.id.glucose_editor_insulin_suggestion)
        mInfoView = findViewById(R.id.glucose_editor_info)

        mEditMode = intent.getBooleanExtra(EXTRA_INSERT_MODE, false)

        val id = intent.getLongExtra(EXTRA_GLUCOSE_ID, -1)
        mViewModel = ViewModelProviders.of(this).get(EditorViewModel::class.java)
        mViewModel.setGlucose(id) {
            mViewModel.prepare {
                setup()
                refresh()
            }
        }
    }

    private fun setup() {
        mValueView.text = mViewModel.glucose.value.toString()
        mKeyboardView.bindTextView(mValueView, this::onValueTextChanged)

        mDateView.text = mViewModel.glucose.date.getDetailedString()
        mDateView.setOnClickListener { onDateClicked() }
        mEatBar.progress = mViewModel.glucose.eatLevel

        mInsulinView.setOnClickListener { onInsulinClicked() }
        mBasalView.setOnClickListener { onBasalClicked() }

        mFab.setOnClickListener { onFabClicked() }
    }

    private fun refresh() {
        if (mEditMode) {
            setEditUi()
        } else {
            setShowUi()
        }
    }

    private fun setEditUi() {
        if (intent.getBooleanExtra(EXTRA_INSERT_MODE, false)) {
            val editMode = ConstraintSet()
            editMode.clone(this, R.layout.constraint_glucose_edit)
            editMode.applyTo(mConstraintRoot)

            mViewModel.glucose.timeFrame = Date().asTimeFrame()
        }

        mInsulinView.visibility = View.GONE
        mBasalView.visibility = View.GONE
        mInsulinView.alpha = 1f
        mBasalView.alpha = 1f
        mEatBar.isEnabled = true

        mFab.setImageResource(R.drawable.ic_done)
    }

    private fun setShowUi() {
        mInsulinView.visibility = View.VISIBLE
        mEatBar.isEnabled = false

        val ids = Pair(mViewModel.glucose.insulinId0, mViewModel.glucose.insulinId1)

        if (ids.first == -1L) {
            mInsulinView.text = getString(R.string.glucose_editor_insulin_add)
        } else {
            val insulin = mViewModel.getInsulin(ids.first)
            val value = mViewModel.glucose.insulinValue0
            mInsulinView.text = insulin.getDisplayedString(value)
        }

        if (mViewModel.hasPotentialBasal()) {
            mBasalView.visibility = View.VISIBLE
            if (ids.second == -1L) {
                mBasalView.text = getString(R.string.glucose_editor_basal_add)
            } else {
                val basal = mViewModel.getInsulin(ids.second)
                val value = mViewModel.glucose.insulinValue1
                mBasalView.text = basal.getDisplayedString(value)
            }
        } else {
            mBasalView.visibility = View.GONE
        }

        val data = mViewModel.previousWeek
        mInfoView.text = getInfo(data)
        mDateView.text = mViewModel.glucose.date.getDetailedString()


        val targetInsulin = mViewModel.getInsulinByTimeFrame()
        mSuggestionView.bind(mViewModel.glucose, targetInsulin, this::onSuggestionApply)
        mViewModel.getInsulinSuggestion(mSuggestionView::onSuggestionLoaded)
        
        mFab.setImageResource(R.drawable.ic_edit)
    }

    private fun onDateClicked() {
        if (!mEditMode) {
            return
        }

        if (mErrorStatus and (1 shl 1) != 0) {
            mErrorStatus = mErrorStatus and (1 shl 1).inv()
            mDateView.setErrorStatus(false)
        }

        val glucoseCal = mViewModel.glucose.date.getCalendar()
        val newTime = Calendar.getInstance()

        val options = arrayOf(
                getString(R.string.time_today),
                getString(R.string.time_yesterday),
                getString(R.string.time_pick))

        val onTimeSet = { _: View, hour: Int, minute: Int ->
            newTime[Calendar.HOUR_OF_DAY] = hour
            newTime[Calendar.MINUTE] = minute

            mViewModel.glucose.date = newTime.time
            mViewModel.glucose.timeFrame = newTime.time.asTimeFrame()
            mDateView.text = newTime.time.getDetailedString()
        }

        val onCustomDateSet = { _: View, year: Int, month: Int, day: Int ->
            newTime.set(year, month, day)
            TimePickerDialog(this, R.style.AppTheme_DatePickerDialog, onTimeSet,
                glucoseCal[Calendar.HOUR_OF_DAY],
                glucoseCal[Calendar.MINUTE], true).show()
        }

        val onPredefinedDateSet = { _: DialogInterface, i: Int ->
            when (i) {
                0 -> {}
                1 -> newTime.time = Date()[-1]
                else -> DatePickerDialog(this, R.style.AppTheme_DatePickerDialog, onCustomDateSet,
                    glucoseCal[Calendar.YEAR], glucoseCal[Calendar.MONTH],
                    glucoseCal[Calendar.DAY_OF_MONTH]).show()
            }

            if (i == 0 || i == 1) {
                TimePickerDialog(this, onTimeSet, glucoseCal[Calendar.HOUR_OF_DAY],
                        glucoseCal[Calendar.MINUTE], true).show()
            }
        }

        AlertDialog.Builder(this)
                .setItems(options, onPredefinedDateSet)
                .setTitle(R.string.glucose_editor_time_dialog)
                .show()
    }

    private fun onInsulinClicked() {
        if (mEditMode) {
            return
        }

        val dialog = AddInsulinDialog(this, mViewModel.glucose, true)
        dialog.setInsulins(mViewModel.insulins)

        dialog.show(
                { insulin, value -> onInsulinPositive(insulin, value, false) },
                { onInsulinNeutral(false) },
                { setShowUi() })
    }

    private fun onBasalClicked() {
        if (mEditMode) {
            return
        }

        val dialog = AddInsulinDialog(this, mViewModel.glucose, false)
        dialog.setInsulins(mViewModel.basalInsulins)

        dialog.show(
                { insulin, value -> onInsulinPositive(insulin, value, true) },
                { onInsulinNeutral(true) },
                { setShowUi() })
    }

    private fun onFabClicked() {
        if (mEditMode) {
            save()
        } else {
            edit()
        }
    }

    private fun onValueTextChanged(value: String) {
        if (mErrorStatus and 1 == 0) {
            return
        }

        mErrorStatus = mErrorStatus and 1.inv()
        mValueView.setErrorStatus(value == "0")
    }

    private fun save() {
        checkForErrors()
        if (mErrorStatus != 0) {
            Snackbar.make(mConstraintRoot, R.string.glucose_editor_save_error, Snackbar.LENGTH_LONG)
                    .show()
            VibrationUtil.vibrateForError(this)
            return
        }

        Snackbar.make(mConstraintRoot, R.string.saved, 800)
                .show()
        Handler().postDelayed({
            saveData()
            saveToFit()
        }, 1000)
    }

    private fun saveData() {
        mViewModel.glucose.value = mKeyboardView.input
        mViewModel.glucose.eatLevel = mEatBar.progress
        mViewModel.save()
    }

    private fun checkForErrors() {
        if ("0" == mValueView.text) {
            mValueView.setErrorStatus(true)
            mErrorStatus = mErrorStatus or 1
        }

        if (Date().time < mViewModel.glucose.date.time) {
            mDateView.setErrorStatus(true)
            mErrorStatus = mErrorStatus or (1 shl 1)
        }
    }

    private fun edit() {
        mInsulinView.animate()
                .alpha(0f)
                .start()
        mBasalView.animate()
                .alpha(0f)
                .start()

        val editSet = ConstraintSet()
        editSet.clone(this, R.layout.constraint_glucose_edit)
        TransitionManager.beginDelayedTransition(mConstraintRoot)
        editSet.applyTo(mConstraintRoot)


        Handler().postDelayed({
            mEditMode = true
            refresh()
        }, 350)
    }

    private fun onInsulinPositive(insulin: Insulin, value: Float, isBasal: Boolean,
                                  shouldSaveData: Boolean = true) {
        if (isBasal) {
            mViewModel.glucose.insulinId1 = insulin.uid
            mViewModel.glucose.insulinValue1 = value
            mBasalView.text = insulin.getDisplayedString(value)
        } else {
            mViewModel.glucose.insulinId0 = insulin.uid
            mViewModel.glucose.insulinValue0 = value
            mInsulinView.text = insulin.getDisplayedString(value)
        }

        if (shouldSaveData) {
            saveData()
        }
    }

    private fun onInsulinNeutral(isBasal: Boolean) {
        if (isBasal) {
            mViewModel.glucose.insulinId1 = -1
            mViewModel.glucose.insulinValue1 = 0f
            mBasalView.text = getString(R.string.glucose_editor_insulin_add)
        } else {
            mViewModel.glucose.insulinId0 = -1
            mViewModel.glucose.insulinValue0 = 0f
            mInsulinView.text = getString(R.string.glucose_editor_basal_add)
        }

        saveData()
    }

    private fun saveToFit() {
        val handler = SystemUtil.getOverrideObject(BaseFitHandler::class.java,
            this, R.string.config_class_fit_handler)

        if (!handler.hasFit(this)) {
            finish()
            return
        }

        handler.upload(this, mViewModel.glucose, !mEditMode, { finish() }, { e -> Log.e(TAG, e.message) })
    }

    private fun getInfo(list: List<Glucose>): String {
        var average = 0f

        if (!list.isEmpty()) {
            for (i in list.indices) {
                average += list[i].value.toFloat()
            }
            average /= list.size
        }

        val highThreshold = PreferencesUtil.getGlucoseHighThreshold(this)
        val lowThreshold = PreferencesUtil.getGlucoseLowThreshold(this)

        val status = when {
            average > highThreshold -> R.string.glucose_type_high
            average > lowThreshold -> R.string.glucose_type_medium
            else -> R.string.glucose_type_low
        }

        return getString(R.string.glucose_report_base, getString(status), average.roundToInt())
    }

    private fun onSuggestionApply(suggestion: Float, insulin: Insulin) {
        mViewModel.applyInsulinSuggestion(suggestion, insulin, this::refresh)

        Snackbar.make(mConstraintRoot, R.string.insulin_suggestion_applied, Snackbar.LENGTH_LONG)
                .show()
    }

    private fun TextView.setErrorStatus(toError: Boolean) {
        val originalColor = ContextCompat.getColor(this@EditorActivity, R.color.colorAccent)
        val errorColor = ContextCompat.getColor(this@EditorActivity, R.color.action_dangerous)

        val animator = ValueAnimator.ofArgb(
                if (toError) originalColor else errorColor,
                if (toError) errorColor else originalColor)

        val drawable = compoundDrawables[0]

        animator.addUpdateListener { animation ->
            drawable.setColorFilter(animation.animatedValue as Int, PorterDuff.Mode.SRC_ATOP)
        }

        animator.start()
    }

    companion object {
        const val EXTRA_INSERT_MODE = "extra_insert"
        const val EXTRA_GLUCOSE_ID = "glucose_id"

        private const val TAG = "EditorActivity"
    }
}
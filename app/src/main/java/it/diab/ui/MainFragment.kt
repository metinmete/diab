package it.diab.ui

import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import it.diab.db.entities.Glucose

abstract class MainFragment : Fragment() {
    @StringRes
    abstract fun getTitle(): Int
}
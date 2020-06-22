/*
 * Copyright (c) 2020 Bevilacqua Joey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.diab.overview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import xyz.diab.core.model.Glucose
import xyz.diab.core.presenter.OverviewPresenter

internal class OverviewViewModel(
    private val presenter: OverviewPresenter
) : ViewModel() {

    val liveList: LiveData<List<Glucose>> =
        presenter.glucoseFlow.asLiveData(viewModelScope.coroutineContext)

    suspend fun deleteItemAtIndex(id: Long) =
        presenter.deleteItemAtIndex(id)

    suspend fun put(glucose: Glucose) {
        presenter.put(glucose)
    }
}
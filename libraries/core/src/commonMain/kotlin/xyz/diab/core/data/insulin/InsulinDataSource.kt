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

package xyz.diab.core.data.insulin

import kotlinx.coroutines.flow.Flow
import xyz.diab.core.data.DataSource
import xyz.diab.core.model.Insulin

/**
 * Generic [Insulin] data source
 */
abstract class InsulinDataSource : DataSource {

    abstract suspend fun getAll(): List<Insulin>

    abstract suspend fun get(name: String): Insulin

    abstract suspend fun put(insulin: Insulin)

    abstract suspend fun delete(insulin: Insulin)

    abstract fun getAllFlow(): Flow<List<Insulin>>
}
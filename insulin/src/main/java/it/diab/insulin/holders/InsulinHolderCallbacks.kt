/*
 * Copyright (c) 2019 Bevilacqua Joey
 *
 * Licensed under the GNU GPLv3 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl.txt
 */
package it.diab.insulin.holders

interface InsulinHolderCallbacks {

    /**
     * On item click callback
     *
     * @param uid insulin uid
     */
    fun onClick(uid: Long)
}
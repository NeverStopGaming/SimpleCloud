/*
 * MIT License
 *
 * Copyright (C) 2020-2022 The SimpleCloud authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package eu.thesimplecloud.api.servicegroup.impl.updater

import eu.thesimplecloud.api.servicegroup.grouptype.ICloudLobbyGroup
import eu.thesimplecloud.api.servicegroup.grouptype.updater.ICloudLobbyGroupUpdater

class DefaultLobbyGroupUpdater(private val lobbyGroup: ICloudLobbyGroup) : DefaultServiceGroupUpdater(lobbyGroup),
    ICloudLobbyGroupUpdater {

    override fun getServiceGroup(): ICloudLobbyGroup {
        return this.lobbyGroup
    }

    override fun getPriority(): Int {
        return getChangedValue("priority") ?: lobbyGroup.getPriority()
    }

    override fun setPriority(priority: Int) {
        changes["priority"] = priority
    }
}
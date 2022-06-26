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

package eu.thesimplecloud.api.event.player.permission

import eu.thesimplecloud.api.eventapi.IEvent
import eu.thesimplecloud.api.player.ICloudPlayer

/**
 * This event is called when [ICloudPlayer.hasPermission] is called. If the result of this event is [PermissionState.UNKNOWN]
 * a packet to check the permission will be sent to the player's proxy.
 * Note, that the permission module of SimpleCloud uses this event and sets the [PermissionState] on every component
 * in the network.
 */
class CloudPlayerPermissionCheckEvent(
    val cloudPlayer: ICloudPlayer,
    val permission: String,
    var state: PermissionState = PermissionState.UNKNOWN
) : IEvent {


    fun setHasPermission(boolean: Boolean) {
        this.state = PermissionState.valueOf(boolean.toString().toUpperCase())
    }

}
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

package eu.thesimplecloud.api.player

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.player.connection.IPlayerConnection
import eu.thesimplecloud.api.property.IPropertyMap
import eu.thesimplecloud.api.utils.Nameable
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import java.util.*

interface IOfflineCloudPlayer : Nameable, IPropertyMap {

    /**
     * Returns the unique id of this player.
     */
    fun getUniqueId(): UUID

    /**
     * Returns the timestamp of the last login.
     */
    fun getLastLogin(): Long

    /**
     * Returns the timestamp of the first login.
     */
    fun getFirstLogin(): Long

    /**
     * Returns the online time of this player in milliseconds
     */
    fun getOnlineTime(): Long

    /**
     * Returns the las connection of this player.
     */
    fun getLastPlayerConnection(): IPlayerConnection

    /**
     * Returns whether this player is connected to the network.
     */
    fun isOnline(): Boolean = false

    /**
     * Sets the displayname of this player
     * @param displayName the displayname of this player
     */
    fun setDisplayName(displayName: String)

    /**
     * Returns the displayname of this player
     */
    fun getDisplayName(): String

    /**
     * Returns a new [IOfflineCloudPlayer] with the data of this player
     */
    fun toOfflinePlayer(): IOfflineCloudPlayer

    /**
     * Updates this player to the network
     * @return a promise that completes when the player was updated
     */
    fun update(): ICommunicationPromise<Unit> = CloudAPI.instance.getCloudPlayerManager().savePlayerToDatabase(this)

}
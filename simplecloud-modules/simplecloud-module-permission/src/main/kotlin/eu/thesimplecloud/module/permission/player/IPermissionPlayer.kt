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

package eu.thesimplecloud.module.permission.player

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.player.IOfflineCloudPlayer
import eu.thesimplecloud.api.utils.Nameable
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import eu.thesimplecloud.module.permission.PermissionPool
import eu.thesimplecloud.module.permission.entity.IPermissionEntity
import eu.thesimplecloud.module.permission.group.IPermissionGroup
import java.util.*

interface IPermissionPlayer : IPermissionEntity, Nameable {

    /**
     * Returns the uuid of this player
     */
    fun getUniqueId(): UUID

    /**
     * Returns the highest [IPermissionGroup] of this player
     */
    fun getHighestPermissionGroup(): IPermissionGroup {
        return getAllNotExpiredPermissionGroups().maxByOrNull { it.getPriority() }!!
    }

    /**
     * Returns the permission group info list
     */
    fun getPermissionGroupInfoList(): Collection<PlayerPermissionGroupInfo>

    /**
     * Returns the permission group info list
     */
    fun getAllNotExpiredPermissionGroupInfoList(): Collection<PlayerPermissionGroupInfo> =
        getPermissionGroupInfoList().filter { !it.isExpired() }

    /**
     * Returns whether this player has the specified permission group.
     * (case insensitive)
     */
    fun hasPermissionGroup(name: String): Boolean = getPermissionGroupInfoList()
        .map { it.permissionGroupName.toLowerCase() }
        .contains(name.toLowerCase())

    /**
     * Returns the [IPermissionGroup] of this player
     */
    fun getAllNotExpiredPermissionGroups(): List<IPermissionGroup> = getAllNotExpiredPermissionGroupInfoList()
        .mapNotNull {
            PermissionPool.instance.getPermissionGroupManager().getPermissionGroupByName(it.permissionGroupName)
        }

    /**
     * Returns the the [IOfflineCloudPlayer] of this permission player wrapped in a promise
     */
    fun getOfflineCloudPlayer(): ICommunicationPromise<IOfflineCloudPlayer> =
        CloudAPI.instance.getCloudPlayerManager().getOfflineCloudPlayer(getUniqueId())

    /**
     * Returns a promise that is completed when the operation is done. [ICommunicationPromise.isSuccess] indicates success or failure.
     */
    fun update(): ICommunicationPromise<Unit>

    /**
     * Adds a permission group to this player
     */
    fun addPermissionGroup(group: PlayerPermissionGroupInfo)

    /**
     * Removes a permission group from this player
     */
    fun removePermissionGroup(name: String)

    /**
     * Clears all groups
     */
    fun clearGroups()

    override fun hasPermission(permission: String): Boolean {
        val permissionByName = super.getNotExpiredPermissionByMatch(permission)
        if (permissionByName != null) return permissionByName.active

        val anyGroupPermission = getAllNotExpiredPermissionGroups().any { it.hasPermission(permission) }
        return if (anyGroupPermission) {
            true
        } else {
            hasAllRights()
        }
    }

}
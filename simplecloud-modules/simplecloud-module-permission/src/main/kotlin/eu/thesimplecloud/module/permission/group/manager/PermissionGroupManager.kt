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

package eu.thesimplecloud.module.permission.group.manager

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.property.IProperty
import eu.thesimplecloud.api.property.Property
import eu.thesimplecloud.api.sync.list.AbstractSynchronizedObjectList
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import eu.thesimplecloud.module.permission.event.group.PermissionGroupUpdatedEvent
import eu.thesimplecloud.module.permission.group.IPermissionGroup
import eu.thesimplecloud.module.permission.group.PermissionGroup
import eu.thesimplecloud.module.permission.manager.PermissionModule
import java.util.concurrent.CopyOnWriteArrayList

class PermissionGroupManager(
    list: List<PermissionGroup> = emptyList()
) : AbstractSynchronizedObjectList<PermissionGroup>(
    CopyOnWriteArrayList(list.map { Property(it) })
), IPermissionGroupManager {

    @Volatile
    private var defaultPermissionGroupName = "default"

    override fun getIdentificationName(): String = "simplecloud-module-permission"

    override fun getCachedObjectByUpdateValue(value: PermissionGroup): IProperty<PermissionGroup>? {
        return this.values.firstOrNull { it.getValue().getName() == value.getName() }
    }

    override fun getAllPermissionGroups(): Collection<IPermissionGroup> = this.values.map { it.getValue() }

    override fun getPermissionGroupByName(name: String): IPermissionGroup? =
        this.values.firstOrNull { it.getValue().getName() == name }?.getValue()

    override fun getDefaultPermissionGroupName(): String = this.defaultPermissionGroupName

    override fun update(permissionGroup: PermissionGroup) {
        update(Property(permissionGroup), false)
    }

    override fun delete(permissionGroup: IPermissionGroup) {
        getCachedObjectByUpdateValue(permissionGroup as PermissionGroup)?.let { super.remove(it, false) }
        CloudAPI.instance.getEventManager().call(PermissionGroupUpdatedEvent(permissionGroup))
        if (CloudAPI.instance.isManager()) {
            PermissionModule.instance.updateGroupsFile()
        }
    }

    override fun update(property: IProperty<PermissionGroup>, fromPacket: Boolean): ICommunicationPromise<Unit> {
        val returnValue = super.update(property, fromPacket)
        CloudAPI.instance.getEventManager().call(PermissionGroupUpdatedEvent(property.getValue()))
        if (CloudAPI.instance.isManager()) {
            PermissionModule.instance.updateGroupsFile()
        }
        return returnValue
    }

    fun setDefaultPermissionGroup(groupName: String) {
        this.defaultPermissionGroupName = groupName
    }

}
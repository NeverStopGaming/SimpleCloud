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

package eu.thesimplecloud.api.servicegroup.impl

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.cachelist.AbstractCacheList
import eu.thesimplecloud.api.cachelist.ICacheObjectUpdateExecutor
import eu.thesimplecloud.api.event.group.CloudServiceGroupCreatedEvent
import eu.thesimplecloud.api.event.group.CloudServiceGroupUpdatedEvent
import eu.thesimplecloud.api.eventapi.IEvent
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroupManager
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroupUpdater
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise

abstract class AbstractCloudServiceGroupManager : AbstractCacheList<ICloudServiceGroupUpdater, ICloudServiceGroup>(),
    ICloudServiceGroupManager {

    private val updater = object : ICacheObjectUpdateExecutor<ICloudServiceGroupUpdater, ICloudServiceGroup> {
        override fun getIdentificationName(): String {
            return "group-cache"
        }

        override fun getCachedObjectByUpdateValue(value: ICloudServiceGroup): ICloudServiceGroup? {
            return getServiceGroupByName(value.getName())
        }

        override fun determineEventsToCall(
            updater: ICloudServiceGroupUpdater,
            cachedValue: ICloudServiceGroup?
        ): List<IEvent> {
            if (cachedValue == null) return listOf(CloudServiceGroupCreatedEvent(updater.getServiceGroup()))
            return listOf(CloudServiceGroupUpdatedEvent(cachedValue))
        }

        override fun addNewValue(value: ICloudServiceGroup) {
            values.add(value)
        }

    }

    override fun delete(value: ICloudServiceGroup, fromPacket: Boolean): ICommunicationPromise<Unit> {
        if (CloudAPI.instance.getCloudServiceManager().getCloudServicesByGroupName(value.getName()).isNotEmpty())
            throw IllegalStateException("Cannot delete service group while services of this group are registered")
        return super<AbstractCacheList>.delete(value, fromPacket)
    }

    override fun getUpdateExecutor(): ICacheObjectUpdateExecutor<ICloudServiceGroupUpdater, ICloudServiceGroup> {
        return this.updater
    }

}
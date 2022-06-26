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
import eu.thesimplecloud.api.cachelist.AbstractCacheList
import eu.thesimplecloud.api.cachelist.ICacheObjectUpdateExecutor
import eu.thesimplecloud.api.event.player.*
import eu.thesimplecloud.api.eventapi.IEvent
import eu.thesimplecloud.clientserverapi.lib.promise.CommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise

abstract class AbstractCloudPlayerManager : AbstractCacheList<ICloudPlayerUpdater, ICloudPlayer>(spreadUpdates = false),
    ICloudPlayerManager {

    private val updater = object : ICacheObjectUpdateExecutor<ICloudPlayerUpdater, ICloudPlayer> {
        override fun getIdentificationName(): String {
            return "player-cache"
        }

        override fun getCachedObjectByUpdateValue(value: ICloudPlayer): ICloudPlayer? {
            return getCachedCloudPlayer(value.getName())
        }

        override fun determineEventsToCall(updater: ICloudPlayerUpdater, cachedValue: ICloudPlayer?): List<IEvent> {
            val events = ArrayList<IEvent>()
            val playerToUse = cachedValue ?: updater.getCloudPlayer()
            events.add(CloudPlayerUpdatedEvent(playerToUse))
            if (cachedValue == null) {
                events.add(CloudPlayerRegisteredEvent(playerToUse))
                return events
            }
            val connectedServer = updater.getConnectedServer()
            connectedServer?.let {
                if (updater.getConnectedServerName() != cachedValue.getConnectedServerName()) {
                    val oldServer = cachedValue.getConnectedServer()
                    events.add(CloudPlayerServerConnectEvent(playerToUse, oldServer, connectedServer))
                }
                if (cachedValue.getServerConnectState() == PlayerServerConnectState.CONNECTING && updater.getServerConnectState() == PlayerServerConnectState.CONNECTED) {
                    events.add(CloudPlayerServerConnectedEvent(playerToUse, connectedServer))
                }
            }

            return events
        }

        override fun addNewValue(value: ICloudPlayer) {
            values.add(value)
        }

    }

    override fun getUpdateExecutor(): ICacheObjectUpdateExecutor<ICloudPlayerUpdater, ICloudPlayer> {
        return this.updater
    }

    override fun delete(value: ICloudPlayer, fromPacket: Boolean): ICommunicationPromise<Unit> {
        val playerToUse = updater.getCachedObjectByUpdateValue(value) ?: value
        return super<AbstractCacheList>.delete(value, fromPacket).addResultListener {
            CloudAPI.instance.getEventManager().call(CloudPlayerUnregisteredEvent(playerToUse))
        }
    }

    /**
     * Creates a [ICommunicationPromise] with the [cloudPlayer]
     * If the [cloudPlayer] is not null it will returns a promise completed with the player.
     * If the [cloudPlayer] is null it will return a promise failed with [NoSuchElementException]
     */
    fun promiseOfNullablePlayer(cloudPlayer: ICloudPlayer?): ICommunicationPromise<ICloudPlayer> {
        return CommunicationPromise.ofNullable(cloudPlayer, NoSuchElementException("CloudPlayer not found."))
    }

}
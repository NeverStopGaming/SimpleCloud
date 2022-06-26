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

package eu.thesimplecloud.api.network.packets.sync.cachelist

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.cachelist.value.ICacheValue
import eu.thesimplecloud.api.cachelist.value.ICacheValueUpdater
import eu.thesimplecloud.clientserverapi.lib.connection.IConnection
import eu.thesimplecloud.clientserverapi.lib.packet.packettype.JsonPacket
import eu.thesimplecloud.clientserverapi.lib.promise.CommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise

class PacketIOUpdateCacheObject() : JsonPacket() {

    constructor(cacheListName: String, value: Any, action: Action) : this() {
        this.jsonLib.append("cacheListName", cacheListName)
            .append("value", value)
            .append("valueClass", value::class.java.name)
            .append("action", action)
    }

    override suspend fun handle(connection: IConnection): ICommunicationPromise<Any> {
        val cacheListName = this.jsonLib.getString("cacheListName")
            ?: return contentException("cacheListName")
        val valueClassName = this.jsonLib.getString("valueClass")
            ?: return contentException("valueClass")
        val action = this.jsonLib.getObject("action", Action::class.java)
            ?: return contentException("action")
        val valueClass = Class.forName(
            valueClassName,
            true,
            connection.getCommunicationBootstrap().getClassLoaderToSearchObjectPacketsClasses()
        )
        val value = this.jsonLib.getObject("value", valueClass) ?: return contentException("value")
        value as ICacheValue<ICacheValueUpdater>

        return when (action) {
            Action.UPDATE -> {
                CloudAPI.instance.getCacheListManager().getCacheListenerByName(cacheListName)
                    ?.update(value, true) ?: CommunicationPromise.UNIT_PROMISE
            }
            Action.DELETE -> {
                CloudAPI.instance.getCacheListManager().getCacheListenerByName(cacheListName)
                    ?.delete(value, true) ?: CommunicationPromise.UNIT_PROMISE
            }
        }
    }

    enum class Action {
        UPDATE, DELETE
    }
}
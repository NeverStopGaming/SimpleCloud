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

package eu.thesimplecloud.api.sync.list.manager

import eu.thesimplecloud.api.network.packets.sync.list.PacketIOUpdateListProperty
import eu.thesimplecloud.api.sync.list.ISynchronizedObjectList
import eu.thesimplecloud.clientserverapi.lib.connection.IConnection
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.combineAllPromises

interface ISynchronizedObjectListManager {

    /**
     * Registers a synchronized object list linked to the objects name
     * @return a promise that completes when the list was synchronized.
     */
    fun registerSynchronizedObjectList(
        synchronizedObjectList: ISynchronizedObjectList<out Any>,
        syncContent: Boolean = true
    ): ICommunicationPromise<Unit>

    /**
     * Returns the [ISynchronizedObjectList] found by the specified [name]
     */
    fun getSynchronizedObjectList(name: String): ISynchronizedObjectList<Any>?

    /**
     * Removes the [ISynchronizedObjectList] registered to the specified [name]
     */
    fun unregisterSynchronizedObjectList(name: String)

    /**
     * Synchronizes the content of a [ISynchronizedObjectList] with the specified [connection]
     */
    fun synchronizeListWithConnection(
        synchronizedObjectList: ISynchronizedObjectList<out Any>,
        connection: IConnection
    ): ICommunicationPromise<Unit> {
        return synchronizedObjectList.getAllCachedObjects()
            .map {
                connection.sendUnitQuery(
                    PacketIOUpdateListProperty(
                        synchronizedObjectList.getIdentificationName(),
                        it
                    )
                )
            }
            .combineAllPromises()
    }

}
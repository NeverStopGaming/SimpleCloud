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
import eu.thesimplecloud.clientserverapi.lib.promise.CommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import net.kyori.adventure.text.Component
import java.util.concurrent.ConcurrentLinkedQueue

class PlayerMessageQueue(private val player: ICloudPlayer) {

    private val queue = ConcurrentLinkedQueue<QueuedComponent>()

    @Volatile
    private var lastMessagePromise: ICommunicationPromise<Unit> = CommunicationPromise.of(Unit)

    @Synchronized
    fun queueMessage(component: Component): ICommunicationPromise<Unit> {
        val promise = CommunicationPromise<Unit>(500)
        this.queue.add(QueuedComponent(component, promise))
        if (this.lastMessagePromise.isDone) {
            sendNextMessage()
        }
        return promise
    }

    private fun sendNextMessage() {
        val queuedText = queue.poll()
        queuedText?.let {
            val promise = CloudAPI.instance.getCloudPlayerManager().sendMessageToPlayer(player, queuedText.component)
            this.lastMessagePromise = promise
            promise.addCompleteListener { sendNextMessage() }
            promise.addCompleteListener { queuedText.promise.trySuccess(Unit) }
        }
    }


    private class QueuedComponent(val component: Component, val promise: ICommunicationPromise<Unit>)

}
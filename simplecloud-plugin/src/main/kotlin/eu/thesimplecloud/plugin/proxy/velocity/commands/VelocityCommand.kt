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

package eu.thesimplecloud.plugin.proxy.velocity.commands

import com.velocitypowered.api.command.RawCommand
import com.velocitypowered.api.proxy.Player
import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.event.player.CloudPlayerCommandExecuteEvent
import eu.thesimplecloud.plugin.extension.getCloudPlayer
import eu.thesimplecloud.plugin.network.packets.PacketOutPlayerExecuteCommand
import eu.thesimplecloud.plugin.proxy.ProxyEventHandler
import eu.thesimplecloud.plugin.proxy.velocity.CloudVelocityPlugin
import eu.thesimplecloud.plugin.startup.CloudPlugin
import java.util.concurrent.CompletableFuture

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 19.05.2020
 * Time: 20:08
 */
class VelocityCommand(private val commandStart: String) : RawCommand {

    override fun execute(invocation: RawCommand.Invocation) {
        val player = invocation.source() as? Player ?: return
        val command = "$commandStart " + invocation.arguments()

        if (CloudVelocityPlugin.instance.synchronizedIngameCommandsProperty.getValue()
                .contains(commandStart.toLowerCase())
        ) {
            CloudPlugin.instance.connectionToManager.sendUnitQuery(
                PacketOutPlayerExecuteCommand(
                    player.getCloudPlayer(),
                    command
                )
            )
        }
        CloudAPI.instance.getEventManager()
            .call(CloudPlayerCommandExecuteEvent(player.uniqueId, player.username, command))
    }

    override fun suggestAsync(invocation: RawCommand.Invocation?): CompletableFuture<MutableList<String>> {
        val player = invocation?.source() as? Player ?: return CompletableFuture.completedFuture(mutableListOf())
        val rawCommand = "$commandStart " + invocation.arguments()
        return getSuggestCompletableFuture(player, rawCommand)
    }

    private fun getSuggestCompletableFuture(
        player: Player,
        rawCommand: String
    ): CompletableFuture<MutableList<String>> {
        val completableFuture = CompletableFuture<MutableList<String>>()
        ProxyEventHandler.handleTabComplete(player.uniqueId, rawCommand)
            .addResultListener { completableFuture.complete(it.toMutableList()) }
        return completableFuture
    }

}
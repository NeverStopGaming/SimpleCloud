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

package eu.thesimplecloud.plugin.proxy

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.dto.PlayerLoginRequestResult
import eu.thesimplecloud.api.event.player.CloudPlayerDisconnectEvent
import eu.thesimplecloud.api.event.player.CloudPlayerLoginEvent
import eu.thesimplecloud.api.player.ICloudPlayer
import eu.thesimplecloud.api.player.PlayerServerConnectState
import eu.thesimplecloud.api.player.connection.DefaultPlayerConnection
import eu.thesimplecloud.api.player.impl.CloudPlayer
import eu.thesimplecloud.api.player.impl.CloudPlayerUpdater
import eu.thesimplecloud.api.service.ServiceState
import eu.thesimplecloud.api.servicegroup.grouptype.ICloudServerGroup
import eu.thesimplecloud.clientserverapi.lib.packet.packetsender.sendQuery
import eu.thesimplecloud.clientserverapi.lib.promise.CommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise
import eu.thesimplecloud.plugin.network.packets.PacketOutCreateCloudPlayer
import eu.thesimplecloud.plugin.network.packets.PacketOutGetTabSuggestions
import eu.thesimplecloud.plugin.network.packets.PacketOutPlayerConnectToServer
import eu.thesimplecloud.plugin.network.packets.PacketOutPlayerLoginRequest
import eu.thesimplecloud.plugin.startup.CloudPlugin
import java.util.*

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 15.05.2020
 * Time: 23:07
 */
object ProxyEventHandler {

    private val thisGroup = CloudPlugin.instance.thisService().getServiceGroup()

    fun handleLogin(playerConnection: DefaultPlayerConnection, cancelEvent: (String) -> Unit) {
        val thisService = CloudPlugin.instance.thisService()
        if (!thisService.isAuthenticated() || !thisService.isOnline()) {
            cancelEvent("§cProxy is still starting.")
            return
        }
        val playerPromise = CloudAPI.instance.getCloudPlayerManager()
            .getCloudPlayer(playerConnection.getUniqueId()).awaitUninterruptibly()
        if (playerPromise.isSuccess) {
            handleAlreadyRegistered(playerPromise.getNow()!!)
        }

        //send login request
        val createPromise = CloudPlugin.instance.connectionToManager
            .sendQuery<CloudPlayer>(
                PacketOutCreateCloudPlayer(playerConnection, CloudPlugin.instance.thisServiceName),
                1000
            ).awaitUninterruptibly()
        if (!createPromise.isSuccess) {
            cancelEvent("§cFailed to create player: ${createPromise.cause().message}")
            println("Failed to create CloudPlayer:")
            throw createPromise.cause()
        }
        val loginRequestPromise = CloudPlugin.instance.connectionToManager.sendQuery<PlayerLoginRequestResult>(
            PacketOutPlayerLoginRequest(playerConnection.getUniqueId()), 1000
        ).awaitUninterruptibly()
        if (!loginRequestPromise.isSuccess) {
            loginRequestPromise.cause().printStackTrace()
            cancelEvent("§cLogin failed: " + loginRequestPromise.cause().message)
            return
        }
        val loginRequestResult = loginRequestPromise.getBlocking()
        if (loginRequestResult.cancel) {
            cancelEvent(loginRequestResult.kickMessage)
        }

        val cloudPlayer = createPromise.getNow()!!
        val permission = thisGroup.getPermission()
        if (permission != null && !cloudPlayer.hasPermissionSync(permission)) {
            val message = CloudAPI.instance.getLanguageManager().getMessage("ingame.no-permission")
            cancelEvent(message)
            return
        }

        //update player to cache to avoid bugs
        CloudAPI.instance.getCloudPlayerManager().update(cloudPlayer, true).awaitUninterruptibly()
    }

    private fun handleAlreadyRegistered(player: ICloudPlayer) {
        player.kick().awaitUninterruptibly()
        CloudAPI.instance.getCloudPlayerManager()
            .sendDeleteToConnection(player, CloudPlugin.instance.connectionToManager)
            .awaitUninterruptibly()
    }

    fun handlePostLogin(uniqueId: UUID, name: String) {
        val thisService = CloudPlugin.instance.thisService()
        thisService.setOnlineCount(thisService.getOnlineCount() + 1)
        thisService.update().awaitUninterruptibly()

        CloudAPI.instance.getEventManager().call(CloudPlayerLoginEvent(uniqueId, name))
    }


    fun handleDisconnect(uniqueId: UUID, name: String) {
        CloudAPI.instance.getEventManager().call(CloudPlayerDisconnectEvent(uniqueId, name))

        val cloudPlayer = CloudAPI.instance.getCloudPlayerManager().getCachedCloudPlayer(uniqueId)
        cloudPlayer?.let {
            cloudPlayer as CloudPlayer
            cloudPlayer.setOffline()
            CloudAPI.instance.getCloudPlayerManager().delete(cloudPlayer)
            //send update that the player is now offline
            val connection = CloudPlugin.instance.connectionToManager
            CloudAPI.instance.getCloudPlayerManager().sendDeleteToConnection(cloudPlayer, connection)
                .awaitUninterruptibly()
        }

        subtractOneFromThisServiceOnlineCount()
    }


    fun handleServerPreConnect(
        uniqueId: UUID,
        serverNameFrom: String?,
        serverNameTo: String,
        cancelEvent: (String, CancelType) -> Unit
    ) {
        if (serverNameFrom == serverNameTo)
            return

        val cloudService = CloudAPI.instance.getCloudServiceManager().getCloudServiceByName(serverNameTo)
        if (cloudService == null) {
            val message = CloudAPI.instance.getLanguageManager().getMessage("ingame.service-not-registered")
            cancelEvent(message, CancelType.KICK)
            return
        }
        val serviceGroup = cloudService.getServiceGroup()
        val cloudPlayer: ICloudPlayer? = CloudAPI.instance.getCloudPlayerManager().getCachedCloudPlayer(uniqueId)

        if (cloudPlayer == null) {
            cancelEvent("§cUnable to find cloud player.", CancelType.KICK)
            return
        }

        if (serviceGroup.isInMaintenance()) {
            if (!cloudPlayer.hasPermissionSync("cloud.maintenance.join")) {
                val message = CloudAPI.instance.getLanguageManager().getMessage("ingame.service-in-maintenance")
                cancelEvent(message, CancelType.MESSAGE)
                return
            }
        }

        if (serviceGroup is ICloudServerGroup) {
            val permission = serviceGroup.getPermission()
            if (permission != null && !cloudPlayer.hasPermissionSync(permission)) {
                val message = CloudAPI.instance.getLanguageManager().getMessage("ingame.no-permission")
                cancelEvent(message, CancelType.MESSAGE)
                return
            }
        }

        if (cloudService.getState() == ServiceState.STARTING) {
            val message = CloudAPI.instance.getLanguageManager().getMessage("ingame.server-still-starting")
            cancelEvent(message, CancelType.MESSAGE)
            return
        }

        CloudPlugin.instance.connectionToManager.sendUnitQuery(PacketOutPlayerConnectToServer(uniqueId, serverNameTo))
            .awaitUninterruptibly()
            .addFailureListener {
                cancelEvent("§cCan't connect to server: " + it.message, CancelType.MESSAGE)
            }


        val playerUpdater = cloudPlayer.getUpdater()
        playerUpdater as CloudPlayerUpdater
        playerUpdater.setServerConnectState(PlayerServerConnectState.CONNECTING)
        playerUpdater.update().awaitUninterruptibly()
    }


    fun handleServerConnect(uniqueId: UUID, serverName: String, cancelEvent: (String) -> Unit) {
        val service = CloudAPI.instance.getCloudServiceManager().getCloudServiceByName(serverName)
        if (service == null) {
            cancelEvent("§cService does not exist.")
            return
        }

        val cloudPlayer = CloudAPI.instance.getCloudPlayerManager().getCachedCloudPlayer(uniqueId)
        cloudPlayer ?: return
        val playerUpdater = cloudPlayer.getUpdater()
        playerUpdater as CloudPlayerUpdater
        playerUpdater.setConnectedServerName(service.getName())
        playerUpdater.setServerConnectState(PlayerServerConnectState.CONNECTED)
        playerUpdater.update().awaitUninterruptibly()
    }

    fun handleServerKick(
        cloudPlayer: ICloudPlayer,
        kickReasonString: String,
        serverName: String,
        cancelEvent: (String, CancelType) -> Unit
    ) {
        if (kickReasonString.isNotEmpty() && kickReasonString.contains("Outdated server") || kickReasonString.contains("Outdated client")) {
            val cloudService = CloudAPI.instance.getCloudServiceManager().getCloudServiceByName(serverName)
            if (cloudService == null || cloudService.isLobby()) {
                cancelEvent("§cYou are using an unsupported version.", CancelType.KICK)
                return
            }
        }
        val playerUpdater = cloudPlayer.getUpdater()
        playerUpdater as CloudPlayerUpdater
        if (playerUpdater.getConnectedServerName() != null) {
            playerUpdater.setServerConnectState(PlayerServerConnectState.CONNECTED)
            playerUpdater.update().awaitUninterruptibly()
        }
    }

    fun handleTabComplete(uuid: UUID, rawCommand: String): ICommunicationPromise<Array<String>> {
        val commandString = rawCommand.replace("/", "")
        if (commandString.isEmpty()) return CommunicationPromise.of(emptyArray())

        return CloudPlugin.instance.connectionToManager.sendQuery(
            PacketOutGetTabSuggestions(
                uuid,
                commandString
            )
        )
    }

    private fun subtractOneFromThisServiceOnlineCount() {
        val service = CloudPlugin.instance.thisService()
        service.setOnlineCount(service.getOnlineCount() - 1)
        service.update().awaitUninterruptibly()
    }

}
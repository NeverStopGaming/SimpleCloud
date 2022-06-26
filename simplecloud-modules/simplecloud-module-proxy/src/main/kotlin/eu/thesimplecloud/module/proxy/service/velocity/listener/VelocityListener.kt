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

package eu.thesimplecloud.module.proxy.service.velocity.listener

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import eu.thesimplecloud.module.proxy.extensions.mapToLowerCase
import eu.thesimplecloud.module.proxy.service.ProxyHandler
import eu.thesimplecloud.module.proxy.service.velocity.VelocityPluginMain
import eu.thesimplecloud.plugin.startup.CloudPlugin
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 16.05.2020
 * Time: 23:48
 */
class VelocityListener(val plugin: VelocityPluginMain) {

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        val player = event.player

        val config = ProxyHandler.configHolder.getValue()
        val proxyConfiguration = ProxyHandler.getProxyConfiguration() ?: return

        if (CloudPlugin.instance.thisService().getServiceGroup().isInMaintenance()) {
            if (!player.hasPermission(ProxyHandler.JOIN_MAINTENANCE_PERMISSION) &&
                !proxyConfiguration.whitelist.mapToLowerCase().contains(player.username.toLowerCase())
            ) {
                player.disconnect(ProxyHandler.getHexColorComponent(ProxyHandler.replaceString(config.maintenanceKickMessage)))
                event.result = ServerPreConnectEvent.ServerResult.denied()
                return
            }
        }


        val maxPlayers = CloudPlugin.instance.thisService().getServiceGroup().getMaxPlayers()



        if (ProxyHandler.getOnlinePlayers() < maxPlayers)
            return

        if (player.hasPermission(ProxyHandler.JOIN_FULL_PERMISSION) &&
            proxyConfiguration.whitelist.mapToLowerCase().contains(player.username.toLowerCase())
        )
            return

        player.disconnect(ProxyHandler.getHexColorComponent(ProxyHandler.replaceString(config.fullProxyKickMessage)))
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }

    @Subscribe
    fun on(event: ServerConnectedEvent) {
        val player = event.player
        val tablistConfiguration = ProxyHandler.getCurrentTablistConfiguration() ?: return
        plugin.sendHeaderAndFooter(player, tablistConfiguration)
    }

    @Subscribe
    fun handle(event: ProxyPingEvent) {
        val proxyConfiguration = ProxyHandler.getProxyConfiguration() ?: return
        val motdConfiguration = if (CloudPlugin.instance.thisService().getServiceGroup().isInMaintenance())
            proxyConfiguration.maintenanceMotds else proxyConfiguration.motds

        val motdBuilder = StringBuilder();

        if (motdConfiguration.firstLines.isNotEmpty())
            motdBuilder.append(motdConfiguration.firstLines.random()) else motdBuilder.append(" ")

        motdBuilder.append("\n")

        if (motdConfiguration.secondLines.isNotEmpty())
            motdBuilder.append(motdConfiguration.secondLines.random()) else motdBuilder.append(" ")

        val motd = ProxyHandler.getHexColorComponent(ProxyHandler.replaceString(motdBuilder.toString()))

        val ping = event.ping
        var protocol: ServerPing.Version = ping.version
        var players: ServerPing.Players? = if (ping.players.isPresent) ping.players.get() else null
        val favicon = if (ping.favicon.isPresent) ping.favicon.get() else null
        val modinfo = if (ping.modinfo.isPresent) ping.modinfo.get() else null

        val playerInfo = motdConfiguration.playerInfo
        val onlinePlayers = ProxyHandler.getOnlinePlayers()

        val versionName = motdConfiguration.versionName

        if (versionName != null && versionName.isNotEmpty()) {
            val versionColorComponent = ProxyHandler.getHexColorComponent(ProxyHandler.replaceString(versionName))
            protocol = ServerPing.Version(
                -1,
                LegacyComponentSerializer.legacy('§').serialize(versionColorComponent)
            )
        }

        val maxPlayers = CloudPlugin.instance.thisService().getServiceGroup().getMaxPlayers()

        val playerSamples = if (playerInfo != null && playerInfo.isNotEmpty()) {
            val playerInfoString = ProxyHandler.replaceString(playerInfo.joinToString("\n"))
            val playerInfoColorComponent = ProxyHandler.getHexColorComponent(playerInfoString)
            listOf(
                ServerPing.SamplePlayer(
                    LegacyComponentSerializer.legacy('§').serialize(playerInfoColorComponent),
                    UUID.randomUUID()
                )
            )
        } else {
            emptyList()
        }

        players = ServerPing.Players(onlinePlayers, maxPlayers, playerSamples)
        event.ping = ServerPing(protocol, players, motd, favicon, modinfo)
    }

}
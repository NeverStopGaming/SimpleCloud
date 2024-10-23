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

package eu.thesimplecloud.base.manager.commands

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.template.impl.DefaultTemplate
import eu.thesimplecloud.base.manager.setup.ServiceVersionSetup
import eu.thesimplecloud.base.manager.setup.WrapperSetup
import eu.thesimplecloud.base.manager.setup.groups.*
import eu.thesimplecloud.launcher.config.java.JavaVersion
import eu.thesimplecloud.launcher.console.command.CommandType
import eu.thesimplecloud.launcher.console.command.ICommandHandler
import eu.thesimplecloud.launcher.console.command.annotations.Command
import eu.thesimplecloud.launcher.console.command.annotations.CommandArgument
import eu.thesimplecloud.launcher.console.command.annotations.CommandSubPath
import eu.thesimplecloud.launcher.startup.Launcher

@Command("create", CommandType.CONSOLE, "cloud.command.create")
class CreateCommand : ICommandHandler {

    val templateManager = CloudAPI.instance.getTemplateManager()


    @CommandSubPath("lobbygroup", "Creates a lobby group")
    fun createLobbyGroup() {
        if (JavaVersion.paths.versions.isEmpty()) {
            Launcher.instance.setupManager.queueSetup(LobbyGroupSetup())
            return
        }
        Launcher.instance.setupManager.queueSetup(LobbyGroupSetupWithJava())
    }

    @CommandSubPath("proxygroup", "Creates a proxy group")
    fun createProxyGroup() {
        Launcher.instance.setupManager.queueSetup(ProxyGroupSetup())
    }

    @CommandSubPath("servergroup", "Creates a server group")
    fun createServerGroup() {
        if (JavaVersion.paths.versions.isEmpty()) {
            Launcher.instance.setupManager.queueSetup(ServerGroupSetup())
            return
        }
        Launcher.instance.setupManager.queueSetup(ServerGroupSetupWithJava())
    }

    @CommandSubPath("wrapper", "Creates a wrapper")
    fun createWrapper() {
        Launcher.instance.setupManager.queueSetup(WrapperSetup())
    }

    @CommandSubPath("serviceVersion", "Creates a Service Version")
    fun createServiceVersion() {
        Launcher.instance.setupManager.queueSetup(ServiceVersionSetup())
    }

    @CommandSubPath("template <name>", "Creates a template")
    fun createTemplate(@CommandArgument("name") name: String) {
        if (name.length > 16) {
            Launcher.instance.consoleSender.sendProperty("manager.command.create.template.name-too-long")
        }
        if (templateManager.getTemplateByName(name) != null) {
            Launcher.instance.consoleSender.sendProperty("manager.command.create.template.already-exist", name)
            return
        }
        val split = name.split(" ")
        val templateName = split.joinToString("-")
        val template = DefaultTemplate(templateName)
        templateManager.update(template)
        Launcher.instance.consoleSender.sendProperty("manager.command.create.template.success", name)

    }
}
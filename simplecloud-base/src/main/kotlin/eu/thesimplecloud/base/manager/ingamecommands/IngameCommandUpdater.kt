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

package eu.thesimplecloud.base.manager.ingamecommands

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.eventapi.CloudEventHandler
import eu.thesimplecloud.api.eventapi.IListener
import eu.thesimplecloud.base.manager.startup.Manager
import eu.thesimplecloud.launcher.event.command.CommandRegisteredEvent
import eu.thesimplecloud.launcher.startup.Launcher

class IngameCommandUpdater : IListener {

    private var ingameCommandList: Collection<String> = emptyList()

    init {
        CloudAPI.instance.getGlobalPropertyHolder()
            .setProperty("simplecloud-ingamecommands", this.ingameCommandList.toTypedArray())
        CloudAPI.instance.getEventManager().registerListener(Manager.instance, this)
    }


    @CloudEventHandler
    fun on(event: CommandRegisteredEvent) {
        this.ingameCommandList = Launcher.instance.commandManager.getAllIngameCommandPrefixes()
        CloudAPI.instance.getGlobalPropertyHolder()
            .setProperty("simplecloud-ingamecommands", this.ingameCommandList.toTypedArray())
    }


}
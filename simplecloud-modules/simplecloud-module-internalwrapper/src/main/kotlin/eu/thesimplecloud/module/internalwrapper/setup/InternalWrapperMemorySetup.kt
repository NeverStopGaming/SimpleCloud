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

package eu.thesimplecloud.module.internalwrapper.setup

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.wrapper.impl.DefaultWrapperInfo
import eu.thesimplecloud.launcher.config.launcher.LauncherConfig
import eu.thesimplecloud.launcher.console.setup.ISetup
import eu.thesimplecloud.launcher.console.setup.annotations.SetupCancelled
import eu.thesimplecloud.launcher.console.setup.annotations.SetupFinished
import eu.thesimplecloud.launcher.console.setup.annotations.SetupQuestion
import eu.thesimplecloud.launcher.startup.Launcher

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 17.06.2020
 * Time: 12:06
 */
class InternalWrapperMemorySetup(private val config: LauncherConfig) : ISetup {

    private var memory: Int = 2048

    @SetupQuestion(0, "internalwrapper.setup.memory.question.name")
    fun memorySetup(memory: Int): Boolean {
        this.memory = memory
        Launcher.instance.consoleSender.sendPropertyInSetup("internalwrapper.setup.memory.question.memory.success")
        return true
    }

    @SetupFinished
    @SetupCancelled
    fun finishedOrCancelled() {
        val wrapperInfo = DefaultWrapperInfo("InternalWrapper", config.host, 2, this.memory)
        CloudAPI.instance.getWrapperManager().update(wrapperInfo)
        Launcher.instance.consoleSender.sendProperty("internalwrapper.setup.memory.finished")
    }

}
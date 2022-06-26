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

package eu.thesimplecloud.module.notify

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.external.ICloudModule
import eu.thesimplecloud.jsonlib.JsonLib
import eu.thesimplecloud.module.notify.config.Config
import eu.thesimplecloud.module.notify.config.DefaultConfig
import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 13.04.2020
 * Time: 16:26
 */
class NotifyModule : ICloudModule {

    private val configFile = File("modules/notify", "config.json")
    lateinit var config: Config

    override fun onEnable() {
        loadConfig()
        CloudAPI.instance.getEventManager().registerListener(this, CloudListener(this))
    }

    override fun onDisable() {
    }

    fun loadConfig() {
        if (!configFile.exists()) {
            val config = DefaultConfig.get()
            saveConfig(config)
            this.config = config
        }

        val config = JsonLib.fromJsonFile(configFile)!!.getObject(Config::class.java)
        this.config = config
    }

    fun saveConfig(config: Config) {
        JsonLib.fromObject(config).saveAsFile(configFile)
    }

    fun saveConfig() {
        saveConfig(config)
    }

}
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

package eu.thesimplecloud.launcher.startup

import eu.thesimplecloud.api.directorypaths.DirectoryPaths
import eu.thesimplecloud.api.external.ICloudModule
import eu.thesimplecloud.jsonlib.JsonLib
import eu.thesimplecloud.launcher.application.ApplicationStarter
import eu.thesimplecloud.launcher.application.CloudApplicationType
import eu.thesimplecloud.launcher.application.ICloudApplication
import eu.thesimplecloud.launcher.config.java.JavaVersion
import eu.thesimplecloud.launcher.config.java.JavaVersionConfigLoader
import eu.thesimplecloud.launcher.config.launcher.LauncherConfig
import eu.thesimplecloud.launcher.config.launcher.LauncherConfigLoader
import eu.thesimplecloud.launcher.console.ConsoleManager
import eu.thesimplecloud.launcher.console.ConsoleSender
import eu.thesimplecloud.launcher.console.command.CommandManager
import eu.thesimplecloud.launcher.console.setup.SetupManager
import eu.thesimplecloud.launcher.language.LanguageFileLoader
import eu.thesimplecloud.launcher.logging.LoggerProvider
import eu.thesimplecloud.launcher.screens.IScreenManager
import eu.thesimplecloud.launcher.screens.ScreenManagerImpl
import eu.thesimplecloud.launcher.setups.AutoIpSetup
import eu.thesimplecloud.launcher.setups.LanguageSetup
import eu.thesimplecloud.launcher.setups.StartSetup
import eu.thesimplecloud.launcher.updater.RunnerUpdater
import eu.thesimplecloud.launcher.updater.UpdateExecutor
import eu.thesimplecloud.loader.dependency.DependencyLoader
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess


/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 06.09.2019
 * Time: 21:31
 */
class Launcher(val launcherStartArguments: LauncherStartArguments) {

    companion object {
        @JvmStatic
        lateinit var instance: Launcher
            private set
    }

    private val launcherCloudModule = object : ICloudModule {
        override fun onEnable() {
        }

        override fun onDisable() {
        }
    }

    @Volatile
    var isBaseLoaded = false
        private set

    var activeApplication: ICloudApplication? = null
    val screenManager: IScreenManager = ScreenManagerImpl()
    val logger = LoggerProvider(screenManager)
    val commandManager: CommandManager
    val consoleSender = ConsoleSender()
    val consoleManager: ConsoleManager
    val setupManager = SetupManager(this)
    private val launcherConfigLoader = LauncherConfigLoader()
    private val javaVersionConfigLoader = JavaVersionConfigLoader()
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    val currentClassLoader: ClassLoader = Thread.currentThread().contextClassLoader
    var launcherConfig: LauncherConfig
        private set
    private val startedTime = System.currentTimeMillis()

    init {
        instance = this
        if (System.getProperty("simplecloud.version") == null)
            System.setProperty("simplecloud.version", Launcher::class.java.`package`.implementationVersion)
        Thread.setDefaultUncaughtExceptionHandler { _, cause ->
            try {
                this.logger.exception(cause)
            } catch (e: Exception) {
                println("An error occurred logging an exception")
                println("Exception while logging:")
                e.printStackTrace()
                println("Exception to log:")
                cause.printStackTrace()
            }
        }
        System.setProperty("user.language", "en")
        migrateOldLauncherFile()

        this.launcherConfig = this.launcherConfigLoader.loadConfig()
        DirectoryPaths.paths = launcherConfig.directoryPaths
        JavaVersion.paths = javaVersionConfigLoader.loadConfig()
        this.commandManager = CommandManager()
        this.consoleManager = ConsoleManager(this.commandManager, this.consoleSender)
    }

    fun start() {
        DependencyLoader.INSTANCE.disableLogger()
        clearConsole()
        if (!launcherStartArguments.disableAutoUpdater) {
            if (executeUpdateIfAvailable()) {
                return
            }
        } else {
            this.logger.warning("Auto updater is disabled.")
        }
        this.commandManager.registerAllCommands(
            launcherCloudModule,
            this.currentClassLoader,
            "eu.thesimplecloud.launcher.commands"
        )
        this.consoleManager.startThread()

        if (LanguageFileLoader.isFirstStart) {
            this.setupManager.queueSetup(LanguageSetup())
        }
        if (!this.launcherConfigLoader.doesConfigFileExist())
            this.setupManager.queueSetup(AutoIpSetup())
        if (this.launcherStartArguments.startApplication == null)
            this.setupManager.queueSetup(StartSetup())

        this.setupManager.waitForAllSetups()
        this.launcherStartArguments.startApplication?.let { startApplication(it) }
    }

    private fun migrateOldLauncherFile() {
        val jsonLib = JsonLib.fromJsonFile(File("launcher.json")) ?: return
        if (jsonLib.getInt("startServicePort") == null) {
            jsonLib.append("startServicePort", 50000)
        }
        if (jsonLib.getString("uploadDumpFileUrl") == null) {
            jsonLib.append("uploadDumpFileUrl", "https://haste.simplecloud.app")
        }
        jsonLib.saveAsFile("launcher.json")
    }

    private fun executeUpdateIfAvailable(): Boolean {
        val updater = RunnerUpdater(this.launcherStartArguments.updateChannel)
        if (updater.isUpdateAvailable()) {
            this.consoleSender.sendMessage("Found a new runner version: " + updater.getVersionToInstall()!!)
            UpdateExecutor().executeUpdate(updater)
            return true
        } else {
            this.consoleSender.sendMessage("You are running on the latest version of SimpleCloud.")
        }
        return false
    }

    fun startApplication(cloudApplicationType: CloudApplicationType) {
        this.isBaseLoaded = true
        clearConsole()
        logger.info("Starting ${cloudApplicationType.getApplicationName()}...")
        ApplicationStarter().startApplication(cloudApplicationType)
    }

    fun clearConsole() {
        if (isWindows()) {
            try {
                ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } else {
            print("\u001b[H\u001b[2J")
            System.out.flush()
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        this.commandManager.handleCommand(command, this.consoleSender)
    }

    fun shutdown() {
        activeApplication?.onDisable()
        exitProcess(0)
    }

    fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("windows")

    fun getLauncherFile(): File {
        if (System.getProperty("simplecloud.launcher.update-mode") != null) {
            return File("launcher-update.jar")
        }
        return File(Launcher::class.java.protectionDomain.codeSource.location.toURI())
    }

    fun getBaseFile(): File {
        return File("storage/base.jar")
    }

    fun getNewClassLoaderWithLauncherAndBase(): URLClassLoader {
        val launcherFileURL = this.getLauncherFile().toURI().toURL()
        val currentContextClassLoader = Thread.currentThread().contextClassLoader

        return if (isBaseLoaded) {
            val baseFileURL = this.getBaseFile().toURI().toURL()
            URLClassLoader(arrayOf(baseFileURL, launcherFileURL), currentContextClassLoader)
        } else {
            URLClassLoader(arrayOf(launcherFileURL), currentContextClassLoader)
        }
    }

    fun isSnapshotBuild(): Boolean {
        return System.getProperty("simplecloud.version").lowercase().contains("snapshot")
    }

    fun getCurrentVersion(): String {
        return System.getProperty("simplecloud.version")
    }

    fun replaceLauncherConfig(config: LauncherConfig) {
        this.launcherConfig = config
        this.launcherConfigLoader.saveConfig(config)
    }

    fun getUpTime(): Long {
        return System.currentTimeMillis() - startedTime
    }

}
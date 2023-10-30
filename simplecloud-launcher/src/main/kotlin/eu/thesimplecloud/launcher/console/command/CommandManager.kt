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

package eu.thesimplecloud.launcher.console.command

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.ICloudAPI
import eu.thesimplecloud.api.command.ICommandSender
import eu.thesimplecloud.api.external.ICloudModule
import eu.thesimplecloud.api.parser.string.StringParser
import eu.thesimplecloud.api.player.ICloudPlayer
import eu.thesimplecloud.api.utils.getEnumValues
import eu.thesimplecloud.launcher.console.ConsoleSender
import eu.thesimplecloud.launcher.console.command.annotations.Command
import eu.thesimplecloud.launcher.console.command.annotations.CommandArgument
import eu.thesimplecloud.launcher.console.command.annotations.CommandSubPath
import eu.thesimplecloud.launcher.console.command.provider.EmptyCommandSuggestionProvider
import eu.thesimplecloud.launcher.event.command.CommandExecuteEvent
import eu.thesimplecloud.launcher.event.command.CommandRegisteredEvent
import eu.thesimplecloud.launcher.event.command.CommandUnregisteredEvent
import eu.thesimplecloud.launcher.exception.CommandRegistrationException
import eu.thesimplecloud.launcher.startup.Launcher
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 30.08.2019
 * Time: 17:41
 */
class CommandManager {


    val commands = CopyOnWriteArrayList<CommandData>()
    private val allowedTypesWithoutCommandArgument = listOf(ICommandSender::class.java, Array<String>::class.java)

    fun handleCommand(readLine: String, commandSender: ICommandSender) {
        val readLine = if (readLine.trim().equals("cloud", true)) "cloud help" else readLine.trim()

        if (readLine.toLowerCase().startsWith("cloud") && commandSender is ICloudPlayer) {
            if (!commandSender.hasPermission("cloud.command.use").getBlocking()) {
                commandSender.sendProperty("command.cloud.no-permission")
                return
            }
        }

        val covertPathFunction: (CommandData) -> String =
            { if (commandSender is ConsoleSender) it.path else it.getPathWithCloudPrefixIfRequired() }
        val matchingCommandData = getMatchingCommandData(readLine)
        if (matchingCommandData == null) {
            val list = getAvailableArgsMatchingCommandData(readLine)
            if (commandSender is ConsoleSender) {
                list.forEach { commandSender.sendMessage(">> ${covertPathFunction(it)} (${it.commandDescription})") }
            } else {
                list.filter { it.commandType != CommandType.CONSOLE }
                    .forEach { commandSender.sendMessage("§8>> §7${covertPathFunction(it)}") }
            }
            if (list.isEmpty()) {
                if (commandSender is ICloudPlayer) {
                    commandSender.sendMessage("This command could not be found! Type \"help\" for help.")
                } else {
                    Launcher.instance.logger.warning("This command could not be found! Type \"help\" for help.")
                }
                return
            }

            return
        }
        if (matchingCommandData.commandType == CommandType.CONSOLE && commandSender !is ConsoleSender) {
            commandSender.sendProperty("commandmanager.onlyconsole")
            return
        }
        if (commandSender is ICloudPlayer) {
            if (matchingCommandData.permission.trim()
                    .isNotEmpty() && !commandSender.hasPermission(matchingCommandData.permission).awaitUninterruptibly()
                    .get()
            ) {
                commandSender.sendProperty("command.player.no-permission")
                return
            }
        }

        val event = CommandExecuteEvent(commandSender, matchingCommandData)
        getCloudAPI()?.getEventManager()?.call(event)
        if (event.isCancelled())
            return

        val list = ArrayList<Any?>()
        val messageArray = readLine.split(" ")
        for (parameterData in matchingCommandData.parameterDataList) {
            if (parameterData.name == null) {
                when (parameterData.type) {
                    ICommandSender::class.java -> list.add(commandSender)
                    Array<String>::class.java -> list.add(
                        messageArray.drop(if (matchingCommandData.hasCloudPrefix()) 2 else 1).toTypedArray()
                    )
                }
                continue
            }

            val parameterName: String = parameterData.name
            val indexOfParameter = matchingCommandData.getIndexOfParameter(parameterName)
            val parameterValue = messageArray[indexOfParameter]

            val obj = try {
                StringParser().parseToObject(parameterValue, parameterData.type)
            } catch (e: Exception) {
                commandSender.sendMessage("§cCan't parse parameter at index $indexOfParameter(\"$parameterValue\") to class ${parameterData.type.simpleName}")
                return
            }

            if (obj == null) {
                commandSender.sendMessage("§cCan't parse parameter at index $indexOfParameter(\"$parameterValue\") to class ${parameterData.type.simpleName}")
                if (parameterData.type.isEnum) {
                    val clazz = parameterData.type as Class<out Enum<*>>
                    val enumValues = clazz.getEnumValues()
                    commandSender.sendMessage("Allowed are: " + enumValues.joinToString(", "))
                }
                return
            }
            list.add(obj)
        }
        try {
            matchingCommandData.method.invoke(matchingCommandData.source, *list.toArray())
        } catch (e: Exception) {
            throw e
        }
    }


    fun getMatchingCommandData(message: String): CommandData? {
        val messageArray = message.split(" ")
        val commandDataList = getCommandDataByArgumentLength(messageArray.size)
        return commandDataList.firstOrNull { commandData ->
            commandData.getAllPathsWithAliases().any {
                val path = it.trim()
                val pathArray = path.split(" ")

                pathArray.withIndex()
                    .all { isParameter(it.value) || it.value.toLowerCase() == messageArray[it.index].toLowerCase() }
            }
        }
    }

    private fun getAvailableArgsMatchingCommandData(message: String): List<CommandData> {
        val messageArray = message.split(" ")
        val dataList = getCommandDataByMinimumArgumentLength(messageArray.size)
        val dataListWithoutLegacies = dataList.filter { !it.isLegacy }
        return dataListWithoutLegacies.filter { commandData ->
            commandData.getAllPathsWithAliases().any {
                val path = it.trim()
                val pathArray = path.split(" ")

                messageArray.withIndex().all {
                    val pathValue = pathArray[it.index]
                    isParameter(pathValue) || it.value.toLowerCase() == pathValue.toLowerCase()
                }
            }
        }
    }

    fun getAvailableTabCompleteArgs(message: String, sender: ICommandSender): List<String> {
        val messageArray = message.split(" ").map { it.trim() }
        val suggestions = HashSet<String>()
        val dataList = getAvailableArgsMatchingCommandData(messageArray.dropLast(1).joinToString(" "))

        dataList.forEach {
            if (sender is ICloudPlayer && it.commandType == CommandType.CONSOLE) {
                return@forEach
            }
            if (sender !is ICloudPlayer && it.commandType == CommandType.INGAME) {
                return@forEach
            }

            val path = it.getPathWithCloudPrefixIfRequired()
            val pathArray = path.split(" ")

            if (pathArray.size == messageArray.lastIndex) {
                return@forEach
            }

            val currentPathValue = pathArray[messageArray.lastIndex]

            val permission = it.permission
            if (permission.isEmpty() || sender.hasPermission(permission).getBlocking()) {
                if (isParameter(currentPathValue)) {
                    val commandParameterData = it.getParameterDataByNameWithBraces(currentPathValue) ?: return@forEach
                    suggestions.addAll(
                        commandParameterData.provider.getSuggestions(
                            sender,
                            message,
                            messageArray.last()
                        )
                    )
                } else {
                    suggestions.add(currentPathValue)
                }
            }
        }

        return suggestions.filter { it.toLowerCase().startsWith(messageArray.last().toLowerCase()) }
    }

    private fun isParameter(s: String) = s.startsWith("<") && s.endsWith(">")

    private fun getCommandDataByMinimumArgumentLength(length: Int) =
        this.commands.filter { it.getPathWithCloudPrefixIfRequired().split(" ").size >= length }
            .union(this.commands.filter { it.isLegacy })

    private fun getCommandDataByArgumentLength(length: Int) =
        this.commands.filter { it.getPathWithCloudPrefixIfRequired().trim().split(" ").size == length }
            .union(this.commands.filter { it.isLegacy })

    fun registerAllCommands(cloudModule: ICloudModule, classLoader: ClassLoader, vararg packages: String) {
        packages.forEach { pack ->
            val baseClassLoader = Launcher.instance.getNewClassLoaderWithLauncherAndBase()
            val reflections = Reflections(
                ConfigurationBuilder()
                    .forPackage(pack, baseClassLoader)
                    .setClassLoaders(arrayOf(baseClassLoader))
            )
            reflections.getSubTypesOf(ICommandHandler::class.java).forEach {
                val classToUse = Class.forName(it.name, true, classLoader) as Class<out ICommandHandler>
                try {
                    registerCommand(cloudModule, classToUse.getDeclaredConstructor().newInstance())
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }

        val size = this.commands.size
        Launcher.instance.logger.success("Loaded $size command" + (if (size == 1) "" else "s"))

    }

    fun registerCommand(cloudModule: ICloudModule, command: ICommandHandler) {
        val commandClass = command::class.java
        val classAnnotation = commandClass.getAnnotation(Command::class.java)
        classAnnotation ?: throw NullPointerException()

        try {
            for (method in commandClass.declaredMethods) {
                val commandSubPath = method.getAnnotation(CommandSubPath::class.java)
                commandSubPath ?: continue
                val path =
                    if (commandSubPath.path.isBlank()) classAnnotation.name else classAnnotation.name + " " + commandSubPath.path
                val isLegacyCommand = method.parameters.map { it.type }.contains(Array<String>::class.java)
                val commandData = CommandData(
                    cloudModule,
                    path,
                    commandSubPath.description,
                    command,
                    method,
                    classAnnotation.commandType,
                    classAnnotation.permission,
                    classAnnotation.aliases,
                    isLegacyCommand
                )
                for (parameter in method.parameters) {
                    val commandArgument = parameter.getAnnotation(CommandArgument::class.java)
                    if (commandArgument == null) {
                        if (!allowedTypesWithoutCommandArgument.contains(parameter.type) || !allowedTypesWithoutCommandArgument.any {
                                it.isAssignableFrom(
                                    parameter.type
                                )
                            }) {
                            throw CommandRegistrationException("Forbidden parameter type without CommandArgument annotation: ${parameter.type.name}")
                        }
                    }
                    commandData.parameterDataList.add(
                        CommandParameterData(
                            parameter.type,
                            commandArgument?.suggestionProvider?.java?.getDeclaredConstructor()?.newInstance()
                                ?: EmptyCommandSuggestionProvider(),
                            commandArgument?.name
                        )
                    )
                }
                commands.add(commandData)
                getCloudAPI()?.getEventManager()?.call(CommandRegisteredEvent(commandData))
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun unregisterCommands(cloudModule: ICloudModule) {
        val moduleCommands = this.commands.filter { it.cloudModule == cloudModule }
        this.commands.removeAll(moduleCommands)
        moduleCommands.forEach {
            getCloudAPI()?.getEventManager()?.call(CommandUnregisteredEvent(it))
        }
    }

    fun getAllIngameCommandPrefixes(): Collection<String> =
        this.commands.filter { it.commandType == CommandType.INGAME }
            .map { it.getAllPathsWithAliases().map { path -> path.split(" ")[0] } }.flatten().toSet()
            .union(listOf("cloud"))

    private fun getCloudAPI(): ICloudAPI? {
        return try {
            CloudAPI.instance
        } catch (_: Exception) {
            return null
        }
    }

}
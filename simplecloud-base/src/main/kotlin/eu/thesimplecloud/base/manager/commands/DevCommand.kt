package eu.thesimplecloud.base.manager.commands

import eu.thesimplecloud.api.command.ICommandSender
import eu.thesimplecloud.launcher.console.command.CommandType
import eu.thesimplecloud.launcher.console.command.annotations.Command
import eu.thesimplecloud.launcher.console.command.annotations.CommandSubPath
import eu.thesimplecloud.launcher.startup.Launcher

@Command("dev", CommandType.CONSOLE_AND_INGAME, "cloud.command.dev")
class DevCommand {

    @CommandSubPath("","The Dev Command")
    fun handleList(commandSender: ICommandSender) {
        commandSender.sendMessage("Test")
    }
}
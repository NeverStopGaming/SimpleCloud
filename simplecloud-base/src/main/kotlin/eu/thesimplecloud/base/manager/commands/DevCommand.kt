package eu.thesimplecloud.base.manager.commands

import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.api.command.ICommandSender
import eu.thesimplecloud.launcher.console.command.CommandType
import eu.thesimplecloud.launcher.console.command.ICommandHandler
import eu.thesimplecloud.launcher.console.command.annotations.Command
import eu.thesimplecloud.launcher.console.command.annotations.CommandSubPath
import eu.thesimplecloud.launcher.startup.Launcher

@Command("dev", CommandType.CONSOLE_AND_INGAME, "cloud.command.dev")
class DevCommand : ICommandHandler {

    @CommandSubPath("ALL","Shutting down ALL servers")
    fun handleDevALL(commandSender: ICommandSender) {

        val Proxy = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Proxy")
        val Lobby = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Lobby")
        val Panda260 = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Panda260")
        val Chaoten = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Chaoten")
        val SteinGaming = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("SteinGaming")

        Proxy!!.shutdownAllServices()
        Lobby!!.shutdownAllServices()
        Panda260!!.shutdownAllServices()
        Chaoten!!.shutdownAllServices()
        SteinGaming!!.shutdownAllServices()

        commandSender.sendMessage("All services have been shut down")
    }

    @CommandSubPath("API","Shuts down the Servers with the API Plugins")
    fun handleDevAPI(commandSender: ICommandSender) {

        val Panda260 = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Panda260")
        val Chaoten = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Chaoten")
        val SteinGaming = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("SteinGaming")

        Panda260!!.shutdownAllServices()
        Chaoten!!.shutdownAllServices()
        SteinGaming!!.shutdownAllServices()

        commandSender.sendMessage("All services have been shut down")
    }

    @CommandSubPath("PROXY","Shutdown the Proxy")
    fun handleDev(commandSender: ICommandSender) {

        val Proxy = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Proxy")
        val Proxy2 = CloudAPI.instance.getCloudServiceGroupManager().getServiceGroupByName("Proxy2")

        Proxy!!.shutdownAllServices()
        Proxy2!!.shutdownAllServices()

        commandSender.sendMessage("All services have been shut down")
    }
}
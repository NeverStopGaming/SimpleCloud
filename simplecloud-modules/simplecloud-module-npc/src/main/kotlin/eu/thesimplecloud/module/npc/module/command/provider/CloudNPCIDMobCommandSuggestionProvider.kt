package eu.thesimplecloud.module.npc.module.command.provider

import eu.thesimplecloud.api.command.ICommandSender
import eu.thesimplecloud.launcher.console.command.provider.ICommandSuggestionProvider
import eu.thesimplecloud.module.npc.module.NPCModule

class CloudNPCIDMobCommandSuggestionProvider: ICommandSuggestionProvider {

    override fun getSuggestions(sender: ICommandSender, fullCommand: String, lastArgument: String): List<String> {
        return NPCModule.instance.npcModuleConfigHandler.load().npcsConfig.npcs.filter { it.isMob }.map { it.id }
    }
}
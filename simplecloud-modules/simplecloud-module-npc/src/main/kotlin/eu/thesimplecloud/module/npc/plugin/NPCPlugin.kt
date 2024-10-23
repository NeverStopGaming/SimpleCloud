package eu.thesimplecloud.module.npc.plugin

import com.cryptomorin.xseries.XMaterial
import eu.thesimplecloud.api.CloudAPI
import eu.thesimplecloud.module.npc.lib.config.NPCModuleConfig
import eu.thesimplecloud.module.npc.lib.type.MaterialType
import eu.thesimplecloud.module.npc.lib.type.MobType
import eu.thesimplecloud.module.npc.plugin.inventory.InventoryHandler
import eu.thesimplecloud.module.npc.plugin.listener.CloudListener
import eu.thesimplecloud.module.npc.plugin.listener.EntityListener
import eu.thesimplecloud.module.npc.plugin.listener.InventoryListener
import eu.thesimplecloud.module.npc.plugin.listener.PlayerConnectionListener
import eu.thesimplecloud.module.npc.plugin.npc.ServerNPCHandler
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin

class NPCPlugin: JavaPlugin() {

    var serverNPCHandler: ServerNPCHandler? = null
    val inventoryHandler = InventoryHandler(this)

    override fun onEnable() {
        instance = this

        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            Bukkit.getLogger().warning("We recommend using the ProtocolLib plugin to ensure that the NPCs function smoothly. You can download the plugin here: https://www.spigotmc.org/resources/protocollib.1997/")
        }

        this.serverNPCHandler = ServerNPCHandler(this)

        CloudAPI.instance.getEventManager().registerListener(CloudAPI.instance.getThisSidesCloudModule(), CloudListener(this))
        val globalPropertyHolder = CloudAPI.instance.getGlobalPropertyHolder()
        globalPropertyHolder.requestProperty<NPCModuleConfig>("npc-config")

        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(PlayerConnectionListener(this), this)
        pluginManager.registerEvents(InventoryListener(this), this)
        pluginManager.registerEvents(EntityListener(this), this)

        if (!globalPropertyHolder.hasProperty("npc-type-list")) {
            globalPropertyHolder.setProperty("npc-type-list", MobType(
                EntityType.values()
                    .filter { it.isSpawnable }
                    .filter { it.isAlive }
                    .map { it.name }.toMutableList())
            )
        }

        if (!globalPropertyHolder.hasProperty("npc-material-list")) {
            globalPropertyHolder.setProperty("npc-material-list", MaterialType(
                XMaterial.values().filter { it.isSupported }.map { it.name }.toMutableList())
            )
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            this.serverNPCHandler!!.serverNPC.forEach {
                it.value.handleHologramUpdating(it.value.config.targetGroup)
            }
        }, 20, 20)
    }

    override fun onDisable() {
        this.serverNPCHandler?.deleteNPCs()
    }

    companion object {
        lateinit var instance: NPCPlugin
    }
}
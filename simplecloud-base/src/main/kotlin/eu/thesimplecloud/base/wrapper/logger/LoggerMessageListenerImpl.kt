package eu.thesimplecloud.base.wrapper.logger

import eu.thesimplecloud.base.wrapper.startup.Wrapper
import eu.thesimplecloud.client.packets.PacketOutScreenMessage
import eu.thesimplecloud.launcher.logging.ILoggerMessageListener
import eu.thesimplecloud.lib.client.CloudClientType

class LoggerMessageListenerImpl : ILoggerMessageListener {

    override fun success(msg: String) {
        sendMessage(msg)
    }

    override fun info(msg: String) {
        sendMessage(msg)
    }

    override fun warning(msg: String) {
        sendMessage(msg)
    }

    override fun severe(msg: String) {
        sendMessage(msg)
    }

    override fun console(msg: String) {
        sendMessage(msg)
    }

    private fun sendMessage(msg: String) {
        if (Wrapper.instance.communicationClient.isOpen() && Wrapper.instance.communicationClient.getPacketIdsSyncPromise().isSuccess)
            Wrapper.instance.communicationClient.sendQuery(PacketOutScreenMessage(CloudClientType.WRAPPER, Wrapper.instance.getThisWrapper(), msg))
    }
}
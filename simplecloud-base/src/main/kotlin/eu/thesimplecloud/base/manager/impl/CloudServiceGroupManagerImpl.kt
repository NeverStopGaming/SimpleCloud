package eu.thesimplecloud.base.manager.impl

import eu.thesimplecloud.api.service.ICloudService
import eu.thesimplecloud.api.service.startconfiguration.IServiceStartConfiguration
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup
import eu.thesimplecloud.api.servicegroup.impl.AbstractCloudServiceGroupManager
import eu.thesimplecloud.base.manager.startup.Manager
import eu.thesimplecloud.clientserverapi.lib.promise.CommunicationPromise
import eu.thesimplecloud.clientserverapi.lib.promise.ICommunicationPromise

class CloudServiceGroupManagerImpl : AbstractCloudServiceGroupManager() {


    override fun createServiceGroup(cloudServiceGroup: ICloudServiceGroup): ICommunicationPromise<ICloudServiceGroup> {
        val promise = CommunicationPromise<ICloudServiceGroup>()
        if (getServiceGroupByName(cloudServiceGroup.getName()) == null) {
            update(cloudServiceGroup)
            promise.trySuccess(cloudServiceGroup)
        } else {
            promise.setFailure(IllegalArgumentException("Name of the specified group is already registered."))
        }
        return promise
    }

    override fun update(value: ICloudServiceGroup, fromPacket: Boolean, isCalledFromDelete: Boolean) {
        super.update(value, fromPacket, isCalledFromDelete)
        Manager.instance.cloudServiceGroupFileHandler.save(value)
    }

    override fun startNewService(serviceStartConfiguration: IServiceStartConfiguration): ICommunicationPromise<ICloudService> {
        val service = try {
            Manager.instance.serviceHandler.startService(serviceStartConfiguration)
        } catch (ex: IllegalArgumentException) {
            //catch IllegalArgumentException. It will be thrown when the service to start is already registered.
            return CommunicationPromise.failed(ex)
        }
        return CommunicationPromise.of(service)
    }

    override fun delete(value: ICloudServiceGroup, fromPacket: Boolean) {
        super.delete(value, fromPacket)
        Manager.instance.cloudServiceGroupFileHandler.delete(value)
    }

}
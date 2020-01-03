package eu.thesimplecloud.api.servicegroup.grouptype

import eu.thesimplecloud.api.service.ServiceType
import eu.thesimplecloud.api.servicegroup.ICloudServiceGroup

interface ICloudProxyGroup : ICloudServiceGroup {

    /**
     * Returns the start port of this proxy group.
     * All services started by this group will be started on this port or if it is already in use the port will be counted up
     */
    fun getStartPort(): Int

    /**
     * Sets the start port of this proxy group.
     */
    fun setStartPort(port: Int)

    override fun getServiceType(): ServiceType = ServiceType.PROXY
}
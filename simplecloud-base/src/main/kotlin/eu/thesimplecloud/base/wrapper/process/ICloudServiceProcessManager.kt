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

package eu.thesimplecloud.base.wrapper.process

import eu.thesimplecloud.api.service.ICloudService

interface ICloudServiceProcessManager {

    /**
     * Registers a service process on this wrapper.
     */
    fun registerServiceProcess(cloudServiceProcess: ICloudServiceProcess)

    /**
     * Registers a service process on this wrapper.
     */
    fun unregisterServiceProcess(cloudServiceProcess: ICloudServiceProcess)

    /**
     * Returns a list of all registered cloud processes
     */
    fun getAllProcesses(): Set<ICloudServiceProcess>

    /**
     * Returns the [ICloudServiceProcess] found by the specified service [name]
     */
    fun getCloudServiceProcessByServiceName(name: String): ICloudServiceProcess? =
        getAllProcesses().firstOrNull { it.getCloudService().getName().equals(name, true) }

    /**
     * Returns the [ICloudServiceProcess] found by the specified service [service]
     */
    fun getCloudServiceProcessByService(service: ICloudService): ICloudServiceProcess? =
        getCloudServiceProcessByServiceName(service.getName())

    /**
     * Stops all registered services.
     */
    fun stopAllServices() = getAllProcesses().forEach { it.shutdown() }

}
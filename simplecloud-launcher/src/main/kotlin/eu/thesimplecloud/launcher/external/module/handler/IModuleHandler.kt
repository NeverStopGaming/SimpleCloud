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

package eu.thesimplecloud.launcher.external.module.handler

import eu.thesimplecloud.api.external.ICloudModule
import eu.thesimplecloud.launcher.external.module.LoadedModule
import eu.thesimplecloud.launcher.external.module.LoadedModuleFileContent
import eu.thesimplecloud.launcher.external.module.ModuleFileContent
import java.io.File
import java.net.URL
import java.net.URLClassLoader

interface IModuleHandler {

    /**
     * Loads only the content of the modules json file.
     */
    fun loadModuleFileContent(file: File, moduleFileName: String = "module.json"): ModuleFileContent

    /**
     * Loads a single module
     */
    fun loadSingleModule(module: LoadedModuleFileContent): LoadedModule {
        return loadModuleList(listOf(module)).first()
    }

    /**
     * Loads a single module from a file
     */
    fun loadSingleModuleFromFile(file: File): LoadedModule {
        return loadModuleListFromFiles(listOf(file)).first()
    }

    fun loadModuleListFromFiles(files: List<File>): List<LoadedModule>

    /**
     * Loads modules
     */
    fun loadModuleList(modulesToLoad: List<LoadedModuleFileContent>): List<LoadedModule>

    /**
     * Loads all unloaded modules form the modules directory.
     */
    fun loadAllUnloadedModules()

    /**
     * Returns the first [LoadedModule] found by the specified [name]
     */
    fun getLoadedModuleByName(name: String): LoadedModule?

    /**
     * Returns the first [LoadedModule] found by the specified [cloudModule]
     */
    fun getLoadedModuleByCloudModule(cloudModule: ICloudModule): LoadedModule?

    /**
     * Unloaded the specified [cloudModule]
     */
    fun unloadModule(cloudModule: ICloudModule)

    /**
     * Unloads all modules
     */
    fun unloadAllModules()

    /**
     * Unloads all reloadable modules
     * @see [ICloudModule.isReloadable]
     */
    fun unloadAllReloadableModules()

    /**
     * Returns a list containing all loaded modules.
     */
    fun getLoadedModules(): List<LoadedModule>

    /**
     * Searches the class with the given name.
     * This methods searches also in classloaders of all modules.
     */
    @Throws(ClassNotFoundException::class)
    fun findModuleClass(name: String): Class<*>

    /**
     * Searches for the give class in modules and in the system.
     */
    @Throws(ClassNotFoundException::class)
    fun findModuleOrSystemClass(name: String): Class<*>

    /**
     * Sets the function to create module class loaders.
     */
    fun setCreateModuleClassLoader(function: (Array<URL>, String) -> URLClassLoader)
}
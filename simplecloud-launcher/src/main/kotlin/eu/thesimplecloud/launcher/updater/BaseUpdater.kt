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

package eu.thesimplecloud.launcher.updater

import eu.thesimplecloud.api.directorypaths.DirectoryPaths
import eu.thesimplecloud.runner.utils.Downloader
import java.io.File

class BaseUpdater : AbstractUpdater(
    "eu.thesimplecloud.simplecloud",
    "simplecloud-base",
    File(DirectoryPaths.paths.storagePath + "base.jar")
) {

    override fun getVersionToInstall(): String? {
        return getCurrentLauncherVersion()
    }

    override fun getCurrentVersion(): String {
        //return "NOT_INSTALLED" because it will be unequal to the newest base version
        if (!this.updateFile.exists()) return "NOT_INSTALLED"
        return getVersionFromManifestFile(this.updateFile)
    }

    override fun downloadJarsForUpdate() {
        val file = File(DirectoryPaths.paths.storagePath + "base.jar")

        val version = getVersionToInstall()
            ?: throw RuntimeException("Cannot perform update. Is the update server down?")

        Downloader().userAgentDownload(
            "https://repo.simplecloud.app/releases/eu/thesimplecloud/simplecloud/simplecloud-base/$version/simplecloud-base-$version-all.jar",
            file
        )
    }

    override fun executeJar() {
        //do nothing
    }
}
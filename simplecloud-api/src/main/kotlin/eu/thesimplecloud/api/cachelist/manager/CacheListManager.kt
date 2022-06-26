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

package eu.thesimplecloud.api.cachelist.manager

import eu.thesimplecloud.api.cachelist.ICacheList
import eu.thesimplecloud.api.cachelist.value.ICacheValue
import eu.thesimplecloud.api.cachelist.value.ICacheValueUpdater
import java.util.concurrent.CopyOnWriteArrayList

class CacheListManager : ICacheListManager {

    private val cacheListList = CopyOnWriteArrayList<ICacheList<ICacheValueUpdater, ICacheValue<ICacheValueUpdater>>>()

    override fun registerCacheList(cacheList: ICacheList<out ICacheValueUpdater, out ICacheValue<out ICacheValueUpdater>>) {
        this.cacheListList.add(cacheList as ICacheList<ICacheValueUpdater, ICacheValue<ICacheValueUpdater>>)
    }

    override fun getCacheListenerByName(name: String): ICacheList<ICacheValueUpdater, ICacheValue<ICacheValueUpdater>>? {
        return this.cacheListList.firstOrNull { it.getUpdateExecutor().getIdentificationName() == name }
    }
}
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

package eu.thesimplecloud.api.template.impl

import eu.thesimplecloud.api.cachelist.AbstractCacheList
import eu.thesimplecloud.api.cachelist.ICacheObjectUpdateExecutor
import eu.thesimplecloud.api.eventapi.IEvent
import eu.thesimplecloud.api.template.ITemplate
import eu.thesimplecloud.api.template.ITemplateManager
import eu.thesimplecloud.api.template.ITemplateUpdater

open class DefaultTemplateManager : AbstractCacheList<ITemplateUpdater, ITemplate>(), ITemplateManager {

    private val updater = object : ICacheObjectUpdateExecutor<ITemplateUpdater, ITemplate> {
        override fun getIdentificationName(): String {
            return "template-cache"
        }

        override fun getCachedObjectByUpdateValue(value: ITemplate): ITemplate? {
            return getTemplateByName(value.getName())
        }

        override fun determineEventsToCall(updater: ITemplateUpdater, cachedValue: ITemplate?): List<IEvent> {
            return emptyList()
        }

        override fun addNewValue(value: ITemplate) {
            values.add(value)
        }

    }

    override fun deleteTemplate(name: String) {
        getTemplateByName(name)?.let { this.delete(it) }
    }

    override fun getUpdateExecutor(): ICacheObjectUpdateExecutor<ITemplateUpdater, ITemplate> {
        return this.updater
    }

}
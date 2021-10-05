/*
 * Copyright (C) 2017-2019 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hollabrowser.meforce.adblock.filter.unified

import android.net.Uri
import com.hollabrowser.meforce.adblock.core.ContentRequest
import com.hollabrowser.meforce.adblock.filter.ContentFilter

abstract class UnifiedFilter(
    override val pattern: String,
    override val contentType: Int,
    override val ignoreCase: Boolean,
    override val domains: DomainMap?,
    override val thirdParty: Int
) : ContentFilter {

    override fun isMatch(request: ContentRequest): Boolean {
        return if ((contentType and request.type) != 0
            && checkThird(request.isThirdParty)
            && checkDomain(request.pageUrl.host)
        ) {
            check(request.url)
        } else {
            false
        }
    }

    internal abstract fun check(url: Uri): Boolean

    private fun checkThird(isThirdParty: Boolean): Boolean {
        if (thirdParty == -1) return true
        return if (isThirdParty) {
            thirdParty == 1
        } else {
            thirdParty == 0
        }
    }

    private fun checkDomain(domain: String?): Boolean {
        if (domain == null) return true

        val domains = domains ?: return true
        return if (domains.include) {
            domains[domain] == true
        } else {
            domains[domain] != false
        }
    }

    protected fun Char.checkSeparator(): Boolean {
        val it = this.code
        return it in 0..0x24 || it in 0x26..0x2c || it == 0x2f || it in 0x3a..0x40 ||
            it in 0x5b..0x5e || it == 0x60 || it in 0x7b..0x7f
    }

    protected fun String.checkIsDomainInSsp(end: Int): Boolean {
        if (end == 0 || end == 2) return true

        for (i in 2 until end - 1) {
            if (this[i] == '/') return false
        }

        return this[end - 1] == '.'
    }

    override val isRegex: Boolean
        get() = false

    override var next: ContentFilter? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnifiedFilter

        if (pattern != other.pattern) return false
        if (contentType != other.contentType) return false
        if (ignoreCase != other.ignoreCase) return false
        if (thirdParty != other.thirdParty) return false
        if (filterType != other.filterType) return false

        // original domain map comparison not working properly (often returns false for same domains)
        // -> do more extensive check
        if (domains == other.domains) return true
        if (domains?.size != other.domains?.size) return false
        if (domains?.include != other.domains?.include) return false
        // now domains must have same size, and are not null -> compare each
        val d1 = mutableSetOf<String>()
        val d2 = mutableSetOf<String>()
        for (i in 0 until domains!!.size) {
            d1.add(domains!!.getKey(i))
            d2.add(other.domains!!.getKey(i))
        }
        if (d1 == d2) return true

        return false
    }

    override fun hashCode(): Int {
        var result = pattern.hashCode()
        result = 31 * result + contentType
        result = 31 * result + ignoreCase.hashCode()
        result = 31 * result + (domains?.hashCode() ?: 0)
        result = 31 * result + thirdParty
        result = 31 * result + filterType
        return result
    }
}

package com.example.privacy

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages user-configured site allowlisting and domain exemptions.
 */
class SiteExceptionManager {

    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()

    fun setAllowedDomains(domains: Set<String>) {
        allowedDomains.clear()
        allowedDomains.addAll(domains.map { it.lowercase().trim() })
    }

    fun isDomainAllowed(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val cleanHost = host.lowercase().trim()
        if (allowedDomains.contains(cleanHost)) return true
        return allowedDomains.any { cleanHost.endsWith(".$it") }
    }

    fun getAllowedDomains(): Set<String> {
        return allowedDomains.toSet()
    }
}

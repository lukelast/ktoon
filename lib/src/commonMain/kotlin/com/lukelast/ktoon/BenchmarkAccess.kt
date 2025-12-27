package com.lukelast.ktoon

import com.lukelast.ktoon.encoding.StringQuoting

/**
 * Provides access to internal APIs for benchmarking purposes only.
 * Do not use this in production code - these APIs are not stable.
 */
object BenchmarkAccess {

    fun needsQuotingForKey(str: String): Boolean =
        StringQuoting.needsQuoting(str, StringQuoting.QuotingContext.OBJECT_KEY)

    fun needsQuotingForValue(str: String): Boolean =
        StringQuoting.needsQuoting(str, StringQuoting.QuotingContext.OBJECT_VALUE)
}

package com.lukelast.ktoon.encoding

import com.lukelast.ktoon.KtoonConfiguration
import com.lukelast.ktoon.KtoonEncodingException

/**
 * SPEC §15: report a documented nesting limit as a normal error instead of recursing until the host
 * stack is gone. Every composite encoder stands for one container [depth] levels below the root
 * (1-based), so the budget is checked once wherever a container is entered.
 */
internal fun KtoonConfiguration.checkEncoderNesting(depth: Int) {
    if (depth > maxNestingDepth) {
        throw KtoonEncodingException("Maximum nesting depth of $maxNestingDepth exceeded")
    }
}

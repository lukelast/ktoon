package com.lukelast.ktoon.rand

import com.lukelast.ktoon.Ktoon
import org.instancio.Instancio
import org.instancio.settings.Keys
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class InstancioRandomTest {
    @Test
    fun random() {
        repeat(100) {
            val randomData =
                Instancio.of(FarmTestData::class.java)
                    .withSetting(Keys.COLLECTION_MIN_SIZE, 20)
                    .withMaxDepth(100)
                    .withSetting(Keys.STRING_NULLABLE, true)
                    .withSetting(Keys.STRING_MAX_LENGTH, 100)
                    .create()
            val ktoon = Ktoon().encodeToString(randomData)
            Assertions.assertNotNull(ktoon)
        }
    }
}

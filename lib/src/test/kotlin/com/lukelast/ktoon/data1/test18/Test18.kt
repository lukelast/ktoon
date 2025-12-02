package com.lukelast.ktoon.data1.test18

import com.lukelast.ktoon.data1.Runner
import com.lukelast.ktoon.rand.CollectionCoop
import com.lukelast.ktoon.rand.FarmAnimal
import com.lukelast.ktoon.rand.FarmTestData
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.settings.Keys
import org.junit.jupiter.api.Disabled

// TODO bug
@Disabled("bug")
class Test18 : Runner() {
    override fun run() = doTest(randomData)
}

private val randomData =
    Instancio.of(FarmTestData::class.java)
        .withSeed(0)
        .set(field(CollectionCoop::class.java, "animalsByPen"), mapOf<Int, List<FarmAnimal>>())
        .withSetting(Keys.COLLECTION_MIN_SIZE, 0)
        .withSetting(Keys.COLLECTION_MAX_SIZE, 2)
        .withMaxDepth(500)
        .withSetting(Keys.STRING_MIN_LENGTH, 1)
        .withSetting(Keys.STRING_MAX_LENGTH, 10)
        .create()

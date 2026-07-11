package com.sleep.snore.manifest

import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test

class AndroidManifestQueriesTest {

    @Test
    fun manifest_declaresXiaomiAndMiuiPackageVisibilityQueries() {
        val manifest = parseManifest()
        val packages = manifest.getElementsByTagName("package").asSequence()
            .mapNotNull { it.attributes.getNamedItem("android:name")?.nodeValue }
            .toSet()

        assertThat(packages).contains("com.xiaomi.wearable")
        assertThat(packages).contains("com.xiaomi.hm.health")
        assertThat(packages).contains("com.miui.securitycenter")
        assertThat(packages).contains("com.miui.powerkeeper")
    }

    @Test
    fun manifest_declaresMiuiIntentVisibilityQueries() {
        val manifest = parseManifest()
        val actions = manifest.getElementsByTagName("action").asSequence()
            .mapNotNull { it.attributes.getNamedItem("android:name")?.nodeValue }
            .toSet()

        assertThat(actions).contains("miui.intent.action.OP_AUTO_START")
        assertThat(actions).contains("miui.intent.action.POWER_HIDE_MODE_APP_LIST")
    }

    private fun parseManifest() = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(File("src/main/AndroidManifest.xml"))

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> {
        return (0 until length).asSequence().map { item(it) }
    }
}

package com.sleep.snore.manifest

import com.google.common.truth.Truth.assertThat
import com.sleep.snore.sleeptrigger.XiaomiSleepCompanionApps
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test

class AndroidManifestQueriesTest {

    private val nonXiaomiSleepCompanionQueryPackages = setOf(
        "com.google.android.apps.healthdata",
        "com.miui.securitycenter",
        "com.miui.powerkeeper"
    )

    @Test
    fun manifest_declaresXiaomiAndMiuiPackageVisibilityQueries() {
        val manifest = parseManifest()
        val packages = manifest.queryPackageNames()

        assertThat(packages).containsAtLeastElementsIn(XiaomiSleepCompanionApps.packageNames)
        assertThat(packages).contains("com.miui.securitycenter")
        assertThat(packages).contains("com.miui.powerkeeper")
    }

    @Test
    fun manifest_xiaomiCompanionPackageQueriesMatchCompanionPackageList() {
        val manifest = parseManifest()
        val companionPackagesInManifest = manifest.queryPackageNames()
            .minus(nonXiaomiSleepCompanionQueryPackages)

        assertThat(companionPackagesInManifest)
            .containsExactlyElementsIn(XiaomiSleepCompanionApps.packageNames)
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

    private fun org.w3c.dom.Document.queryPackageNames(): Set<String> {
        val queries = getElementsByTagName("queries")

        assertThat(queries.length).isEqualTo(1)

        return queries.item(0).childNodes.asSequence()
            .filter { it.nodeName == "package" }
            .mapNotNull { it.attributes.getNamedItem("android:name")?.nodeValue }
            .toSet()
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> {
        return (0 until length).asSequence().map { item(it) }
    }
}

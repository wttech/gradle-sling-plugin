package com.cognifide.gradle.sling.test

import org.junit.Test

class ComposeTaskTest : SlingTest() {

    @Test
    fun shouldComposePackageWithBundleAndContent() {
        buildTask("compose/bundle-and-content", ":slingCompose", {
            val pkg = file("build/distributions/example-1.0.0-SNAPSHOT.zip")

            assertPackage(pkg)
            assertPackageFile(pkg, "jcr_root/apps/example/.content.xml")
            assertPackageFile(pkg, "jcr_root/apps/example/install/com.company.sling.example-1.0.0-SNAPSHOT.jar")
        })
    }

    @Test
    fun shouldComposePackageAssemblyAndSingles() {
        buildTasks("compose/assembly", "slingCompose", {
            val assemblyPkg = file("build/distributions/example-1.0.0-SNAPSHOT.zip")
            assertPackage(assemblyPkg)
            assertPackageFile(assemblyPkg, "jcr_root/apps/example/core/.content.xml")
            assertPackageBundle(assemblyPkg, "jcr_root/apps/example/core/install/com.company.sling.core-1.0.0-SNAPSHOT.jar")
            assertPackageFile(assemblyPkg, "jcr_root/apps/example/common/.content.xml")
            assertPackageBundle(assemblyPkg, "jcr_root/apps/example/common/install/com.company.sling.common-1.0.0-SNAPSHOT.jar")
            assertPackageBundle(assemblyPkg, "jcr_root/apps/example/common/install/kotlin-osgi-bundle-1.2.21.jar")
            assertPackageFile(assemblyPkg, "jcr_root/etc/designs/example/.content.xml")

            val corePkg = file("core/build/distributions/example-core-1.0.0-SNAPSHOT.zip")
            assertPackage(corePkg)
            assertPackageFile(corePkg, "jcr_root/apps/example/core/.content.xml")
            assertPackageBundle(corePkg, "jcr_root/apps/example/core/install/com.company.sling.core-1.0.0-SNAPSHOT.jar")

            val commonPkg = file("common/build/distributions/example-common-1.0.0-SNAPSHOT.zip")
            assertPackage(commonPkg)
            assertPackageFile(commonPkg, "jcr_root/apps/example/common/.content.xml")
            assertPackageBundle(commonPkg, "jcr_root/apps/example/common/install/com.company.sling.common-1.0.0-SNAPSHOT.jar")
            assertPackageFile(commonPkg, "jcr_root/apps/example/common/install/kotlin-osgi-bundle-1.2.21.jar")

            val designPkg = file("design/build/distributions/example-design-1.0.0-SNAPSHOT.zip")
            assertPackage(designPkg)
            assertPackageFile(designPkg, "jcr_root/etc/designs/example/.content.xml")
        })
    }

}
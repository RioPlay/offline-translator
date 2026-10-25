// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // Keep AGP's networking modules aligned on the patched 4.1 line.
        classpath(platform("io.netty:netty-bom:4.1.139.Final"))
        constraints {
            add("classpath", "org.apache.commons:commons-compress:1.28.0") {
                because("Fix archive parser denial-of-service advisories in build tooling")
            }
            add("classpath", "org.bitbucket.b_c:jose4j:0.9.7") {
                because("Bound decompression of compressed JWE content")
            }
            listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach { module ->
                add("classpath", "org.bouncycastle:$module:1.86") {
                    because("Keep build-time crypto libraries aligned with security fixes")
                }
            }
            add("classpath", "org.jdom:jdom2:2.0.6.1") {
                because("Use patched XML parser defaults in build tooling")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

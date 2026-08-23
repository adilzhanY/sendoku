plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

/*
 * The measuring module.
 *
 * It is a separate app that drives the real one over UI Automator, which is the only way to
 * time a cold start honestly: the thing being measured has to be a release build that was not
 * running a moment ago. The same tests write the baseline profile, so the numbers and the
 * optimisation come from one description of what a player actually does.
 */
android {
    namespace = "com.sendoku.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Macrobenchmark needs a build of the app that is optimised like release but readable by
    // the profiler. The baseline profile plugin adds those build types to the app module.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // One device, one variant. Merging profiles from several devices is for apps with more
    // than one screen shape to worry about.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.junit)
}

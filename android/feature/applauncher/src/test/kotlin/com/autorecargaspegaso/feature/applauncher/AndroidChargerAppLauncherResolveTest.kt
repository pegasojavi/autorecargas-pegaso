package com.autorecargaspegaso.feature.applauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.LaunchResult
import com.autorecargaspegaso.domain.ProviderAppInfo
import com.autorecargaspegaso.domain.ProviderDirectory
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifica la regla de resolución multi-app confirmada en CLAUDE.md
 * secciones 0/3/5 (revisión 2026-09-10): una única app candidata instalada
 * gana; si ninguna está instalada, gana el operador nativo; si hay dos o
 * más instaladas a la vez, se devuelve `NeedsDisambiguation` con esas apps
 * instaladas en vez de decidir sola.
 */
class AndroidChargerAppLauncherResolveTest {

    private val nativeProvider = ProviderAppInfo("native", "Native", "com.native.app", null, "com.native.app")
    private val roamingProvider = ProviderAppInfo("roaming", "Roaming", "com.roaming.app", null, "com.roaming.app")

    private val directory = mockk<ProviderDirectory>()

    init {
        every { directory.find("native") } returns nativeProvider
        every { directory.find("roaming") } returns roamingProvider
        every { directory.find("unknown") } returns null
        // MockK no delega en el cuerpo por defecto de ProviderDirectory.findAll()
        // (método default de interfaz) al mockear — se replica aquí explícitamente.
        every { directory.findAll(any()) } answers {
            firstArg<List<String>>().mapNotNull(directory::find)
        }
    }

    private fun launcherWithInstalled(vararg installedPackages: String): AndroidChargerAppLauncher {
        val packageManager = mockk<PackageManager>()
        val allKnownPackages = listOf(nativeProvider.androidPackage, roamingProvider.androidPackage)
        allKnownPackages.forEach { pkg ->
            every { packageManager.getLaunchIntentForPackage(pkg) } returns
                if (pkg in installedPackages) mockk<Intent>() else null
        }
        val context = mockk<Context>()
        every { context.packageManager } returns packageManager
        return AndroidChargerAppLauncher(context, directory)
    }

    private fun charger(native: String, roaming: List<String> = emptyList()) = Charger(
        id = "c1", name = "Test", latitude = 0.0, longitude = 0.0, address = null,
        connectors = emptyList(), nativeProviderId = native, roamingProviderIds = roaming,
    )

    @Test
    fun `unica candidata instalada abre la app`() {
        val launcher = launcherWithInstalled(nativeProvider.androidPackage)
        val result = launcher.resolve(charger(native = "native"))
        assertEquals(LaunchResult.OpenedApp(nativeProvider), result)
    }

    @Test
    fun `unica candidata no instalada va a tienda`() {
        val launcher = launcherWithInstalled(/* ninguna instalada */)
        val result = launcher.resolve(charger(native = "native"))
        assertEquals(LaunchResult.OpenedStore(nativeProvider), result)
    }

    @Test
    fun `multi-app con solo la de roaming instalada abre la de roaming, no la nativa`() {
        val launcher = launcherWithInstalled(roamingProvider.androidPackage)
        val result = launcher.resolve(charger(native = "native", roaming = listOf("roaming")))
        assertEquals(LaunchResult.OpenedApp(roamingProvider), result)
    }

    @Test
    fun `multi-app sin ninguna instalada gana el operador nativo`() {
        val launcher = launcherWithInstalled(/* ninguna */)
        val result = launcher.resolve(charger(native = "native", roaming = listOf("roaming")))
        assertEquals(LaunchResult.OpenedStore(nativeProvider), result)
    }

    @Test
    fun `multi-app con las dos instaladas a la vez devuelve NeedsDisambiguation con ambas`() {
        val launcher = launcherWithInstalled(nativeProvider.androidPackage, roamingProvider.androidPackage)
        val result = launcher.resolve(charger(native = "native", roaming = listOf("roaming")))
        assertEquals(LaunchResult.NeedsDisambiguation(listOf(nativeProvider, roamingProvider)), result)
    }

    @Test
    fun `operador sin datos en el directorio no revienta, devuelve NoProviderInfo`() {
        val launcher = launcherWithInstalled()
        val result = launcher.resolve(charger(native = "unknown"))
        assertEquals(LaunchResult.NoProviderInfo, result)
    }
}

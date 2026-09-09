package com.autorecargaspegaso.feature.chargerdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autorecargaspegaso.domain.Charger
import com.autorecargaspegaso.domain.Connector
import com.autorecargaspegaso.domain.ConnectorType
import com.autorecargaspegaso.ui.theme.AutoRecargasPegasoTheme

/**
 * Ficha rápida de cargador (CLAUDE.md sección 2): puramente presentacional,
 * sin lógica de lanzador — las dos acciones se delegan al llamador
 * ([onOpenApp] usa `ChargerAppLauncher`, [onGetDirections] usa el intent de
 * navegación nativo) para no acoplar este módulo a Android Framework más
 * de lo necesario.
 */
@Composable
fun ChargerDetailContent(
    charger: Charger,
    onOpenApp: () -> Unit,
    onGetDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = charger.name, style = MaterialTheme.typography.titleLarge)
        charger.address?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge)
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            charger.connectors.forEach { connector ->
                Text(text = connectorLabel(connector), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onOpenApp, modifier = Modifier.weight(1f)) {
                Text("Abrir app")
            }
            OutlinedButton(onClick = onGetDirections, modifier = Modifier.weight(1f)) {
                Text("Cómo llegar")
            }
        }
    }
}

private fun connectorLabel(connector: Connector): String {
    val type = when (connector.type) {
        ConnectorType.TYPE_2 -> "Tipo 2"
        ConnectorType.CCS -> "CCS"
        ConnectorType.CHADEMO -> "CHAdeMO"
        ConnectorType.UNKNOWN -> "Conector"
    }
    val power = connector.powerKw?.let { " · ${it.toInt()} kW" }.orEmpty()
    return type + power
}

@Preview(showBackground = true)
@Composable
private fun ChargerDetailContentPreview() {
    AutoRecargasPegasoTheme {
        ChargerDetailContent(
            charger = Charger(
                id = "1",
                name = "IONITY — Área de servicio A-2",
                latitude = 40.0,
                longitude = -3.0,
                address = "Autovía A-2, km 45",
                connectors = listOf(Connector(ConnectorType.CCS, 350.0)),
                nativeProviderId = "ionity",
            ),
            onOpenApp = {},
            onGetDirections = {},
        )
    }
}

package com.example.agendapersonal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.agendapersonal.alarm.PermisoAlarmasExactas

// Aviso discreto que aparece mientras falte el permiso de alarmas exactas.
// Se vuelve a comprobar cada vez que la pantalla pasa a primer plano, por
// ejemplo al regresar de los ajustes del sistema.
@Composable
fun AvisoRecordatoriosDesactivados(
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permisoConcedido by remember {
        mutableStateOf(PermisoAlarmasExactas.estaConcedido(contexto))
    }

    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                permisoConcedido =
                    PermisoAlarmasExactas.estaConcedido(contexto)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observador)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observador)
        }
    }

    if (permisoConcedido) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Los recordatorios están desactivados: las alarmas no sonarán.",
                fontSize = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )

            TextButton(
                onClick = {
                    PermisoAlarmasExactas.abrirAjustes(contexto)
                }
            ) {
                Text(text = "Activar")
            }
        }
    }
}

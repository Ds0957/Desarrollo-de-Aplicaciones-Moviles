package com.example.agendapersonal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.agendapersonal.ui.navigation.AppNavigation
import com.example.agendapersonal.ui.theme.AgendaPersonalTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.agendapersonal.alarm.ActividadAlarmaReceiver
import com.example.agendapersonal.alarm.ActividadAlarmaSonidoService
import com.example.agendapersonal.alarm.PermisoAlarmasExactas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {

    private var actividadIdPendiente by mutableStateOf<Int?>(null)

    // Controla el diálogo que explica el permiso de alarmas exactas.
    // Se guarda en onSaveInstanceState para que no desaparezca al girar.
    private var mostrarDialogoAlarmas by mutableStateOf(false)

    private val solicitudNotificaciones =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            solicitarPermisoAlarmasExactas()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Al recrearse la Activity (rotación, cambio de tema, etc.) Android
        // entrega de nuevo el intent original con sus extras. Solo se procesa
        // la primera vez para no reabrir la edición ni detener otra alarma.
        // También se ignora si la app se abrió desde la lista de recientes.
        val esIntentNuevo = savedInstanceState == null &&
                (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0

        if (esIntentNuevo) {
            procesarIntentDeNotificacion(intent)
        }

        mostrarDialogoAlarmas =
            savedInstanceState?.getBoolean(ESTADO_DIALOGO_ALARMAS) ?: false

        // Los permisos se piden al abrir la app, no al recrear la Activity.
        if (savedInstanceState == null) {
            solicitarPermisos()
        }

        setContent {
            AgendaPersonalTheme {
                if (mostrarDialogoAlarmas) {
                    DialogoPermisoAlarmas(
                        onIrAAjustes = {
                            mostrarDialogoAlarmas = false
                            PermisoAlarmasExactas.abrirAjustes(this)
                        },
                        onAhoraNo = {
                            mostrarDialogoAlarmas = false
                            PermisoAlarmasExactas.marcarPospuesto(this)
                        },
                        onCerrar = {
                            mostrarDialogoAlarmas = false
                        }
                    )
                }

                AppNavigation(
                    actividadIdParaEditar = actividadIdPendiente,
                    onActividadAbierta = {
                        // Permite que una nueva notificación de la misma
                        // actividad vuelva a abrir la edición.
                        actividadIdPendiente = null
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ESTADO_DIALOGO_ALARMAS, mostrarDialogoAlarmas)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntentDeNotificacion(intent)
    }

    // Atiende los extras que envía la notificación de una actividad:
    // detiene el sonido personalizado y abre la edición. Los extras se
    // eliminan después de usarlos para no procesarlos dos veces.
    private fun procesarIntentDeNotificacion(intent: Intent) {
        detenerAlarmaPersonalizada(intent)

        val actividadId = intent
            .getIntExtra(ActividadAlarmaReceiver.EXTRA_ABRIR_ACTIVIDAD_ID, 0)
            .takeIf { it > 0 }

        intent.removeExtra(ActividadAlarmaReceiver.EXTRA_ABRIR_ACTIVIDAD_ID)

        if (actividadId != null) {
            actividadIdPendiente = actividadId
        }
    }

    private fun solicitarPermisos() {
        // Solicita permiso para mostrar notificaciones en Android 13 o superior.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            solicitudNotificaciones.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            solicitarPermisoAlarmasExactas()
        }
    }

    private fun detenerAlarmaPersonalizada(intent: Intent?) {
        if (intent?.getBooleanExtra(ActividadAlarmaReceiver.EXTRA_DETENER_ALARMA, false) == true) {
            ActividadAlarmaSonidoService.detener(this)
            intent.removeExtra(ActividadAlarmaReceiver.EXTRA_DETENER_ALARMA)
        }
    }

    private fun solicitarPermisoAlarmasExactas() {
        // El acceso a alarmas exactas se concede desde los ajustes del sistema.
        // Antes de enviar al usuario allí se le explica para qué sirve, salvo
        // que ya haya elegido "Ahora no".
        if (
            !PermisoAlarmasExactas.estaConcedido(this) &&
            !PermisoAlarmasExactas.fuePospuesto(this)
        ) {
            mostrarDialogoAlarmas = true
        }
    }

    private companion object {
        const val ESTADO_DIALOGO_ALARMAS = "mostrar_dialogo_alarmas"
    }
}

// Explica por qué la app necesita el permiso "Alarmas y recordatorios"
// antes de abrir los ajustes del sistema.
@Composable
private fun DialogoPermisoAlarmas(
    onIrAAjustes: () -> Unit,
    onAhoraNo: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(text = "Activar recordatorios")
        },
        text = {
            Text(
                text = "Para que la alarma de cada actividad suene a la hora " +
                        "exacta, Android necesita que permitas \"Alarmas y " +
                        "recordatorios\" para Agenda Personal. Sin este permiso " +
                        "las actividades se guardan, pero no sonarán."
            )
        },
        confirmButton = {
            TextButton(onClick = onIrAAjustes) {
                Text(text = "Ir a ajustes")
            }
        },
        dismissButton = {
            TextButton(onClick = onAhoraNo) {
                Text(text = "Ahora no")
            }
        }
    )
}

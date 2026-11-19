package com.example.agendapersonal.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

// Consulta y solicita el permiso especial "Alarmas y recordatorios".
//
// Desde Android 12 las alarmas exactas necesitan este permiso, que el usuario
// concede en los ajustes del sistema. En Android 14 o superior viene denegado
// por defecto. Sin él, las actividades se guardan pero su alarma no suena.
object PermisoAlarmasExactas {

    private const val PREFERENCIAS = "permisos"
    private const val CLAVE_POSPUESTO = "alarmas_exactas_pospuesto"

    // Indica si la app puede programar alarmas exactas.
    fun estaConcedido(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        return alarmManager.canScheduleExactAlarms()
    }

    // Abre la pantalla de ajustes donde se concede el permiso.
    fun abrirAjustes(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    // Indica si el usuario eligió "Ahora no" en el diálogo de inicio.
    // En ese caso ya no se le pregunta al abrir la app; el aviso de la
    // pantalla Día sigue permitiendo activarlo.
    fun fuePospuesto(context: Context): Boolean =
        preferencias(context).getBoolean(CLAVE_POSPUESTO, false)

    fun marcarPospuesto(context: Context) {
        preferencias(context)
            .edit()
            .putBoolean(CLAVE_POSPUESTO, true)
            .apply()
    }

    private fun preferencias(context: Context) =
        context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
}

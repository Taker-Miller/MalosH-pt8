package com.seba.malosh.fragments.metas

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.seba.malosh.R
import com.seba.malosh.activities.BienvenidaActivity
import com.seba.malosh.fragments.progresos.listaLogros // Asegúrate de que listaLogros esté accesible
import com.seba.malosh.fragments.progresos.Logro
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

class ResumenFragment : Fragment() {

    private lateinit var volverButton: Button
    private lateinit var comenzarPlanButton: Button
    private lateinit var periodoSeleccionadoTextView: TextView
    private lateinit var habitoSeleccionadoTextView: TextView
    private var fechaInicio: String? = null
    private var fechaFin: String? = null
    private var habitos: ArrayList<String>? = null

    companion object {
        private const val FECHA_INICIO_KEY = "fecha_inicio"
        private const val FECHA_FIN_KEY = "fecha_fin"
        private const val HABITOS_KEY = "habitos"
        private const val CHANNEL_ID = "logros_channel"

        // Método para crear una nueva instancia del fragmento con los datos necesarios
        fun newInstance(fechaInicio: String, fechaFin: String, habitos: ArrayList<String>): ResumenFragment {
            val fragment = ResumenFragment()
            val bundle = Bundle()
            bundle.putString(FECHA_INICIO_KEY, fechaInicio)
            bundle.putString(FECHA_FIN_KEY, fechaFin)
            bundle.putStringArrayList(HABITOS_KEY, habitos)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_resumen, container, false)

        volverButton = view.findViewById(R.id.volverButton)
        comenzarPlanButton = view.findViewById(R.id.comenzarPlanButton)
        periodoSeleccionadoTextView = view.findViewById(R.id.periodoSeleccionado)
        habitoSeleccionadoTextView = view.findViewById(R.id.habitoSeleccionado)

        // Obtener los argumentos pasados al fragmento
        fechaInicio = arguments?.getString(FECHA_INICIO_KEY)
        fechaFin = arguments?.getString(FECHA_FIN_KEY)
        habitos = arguments?.getStringArrayList(HABITOS_KEY)

        // Mostrar las fechas y los hábitos seleccionados
        periodoSeleccionadoTextView.text = "Periodo: $fechaInicio - $fechaFin"
        habitoSeleccionadoTextView.text = "Hábitos seleccionados: ${habitos?.joinToString(", ")}"

        // Acción para el botón "Volver"
        volverButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Acción para el botón "Comenzar Plan"
        comenzarPlanButton.setOnClickListener {
            guardarMetaEnProgreso()
            verificarDesbloqueoLogros()

            // Aquí llamamos al método de BienvenidaActivity para marcar el plan como iniciado
            (activity as? BienvenidaActivity)?.comenzarPlan()
            Toast.makeText(context, "¡El plan ha comenzado!", Toast.LENGTH_SHORT).show()

            // Redirigir al menú principal o pantalla de bienvenida
            requireActivity().supportFragmentManager.popBackStack(null, 1)
        }

        return view
    }

    private fun guardarMetaEnProgreso() {
        // Almacenar la meta en SharedPreferences como activa y guardar las fechas como Long
        val sharedPreferences = requireContext().getSharedPreferences("MetaPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("meta_en_progreso", true)

        // Convertir las fechas a Long
        val fechaInicioLong = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fechaInicio)?.time
        val fechaFinLong = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fechaFin)?.time

        editor.putLong("fecha_inicio_meta", fechaInicioLong ?: 0L) // Guarda la fecha de inicio como Long
        editor.putLong("fecha_fin_meta", fechaFinLong ?: 0L) // Guarda la fecha de fin como Long
        editor.apply()
    }

    private fun verificarDesbloqueoLogros() {
        // Verificar si se ha cumplido algún logro al comenzar el plan
        val sharedPreferences = requireContext().getSharedPreferences("LogrosPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Desbloquear el logro de definir la primera meta si no está desbloqueado
        val logroPrimerMeta = listaLogros.firstOrNull { it.id == 1 }
        if (logroPrimerMeta != null && !logroPrimerMeta.desbloqueado) {
            logroPrimerMeta.desbloqueado = true
            editor.putBoolean("logro_${logroPrimerMeta.id}", true)
            mostrarNotificacionLogro(logroPrimerMeta)
        }

        // Guardar los cambios en SharedPreferences
        editor.apply()
    }

    private fun mostrarNotificacionLogro(logro: Logro) {
        // Crear un canal de notificación si es necesario (para API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Logros Desbloqueados"
            val descriptionText = "Notificación de logros desbloqueados"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Registrar el canal en el sistema
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Crear la notificación
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_desbloqueado) // Icono de logro desbloqueado
            .setContentTitle("¡Logro Desbloqueado!")
            .setContentText(logro.titulo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Mostrar la notificación
        with(NotificationManagerCompat.from(requireContext())) {
            notify(logro.id, builder.build())
        }
    }
}

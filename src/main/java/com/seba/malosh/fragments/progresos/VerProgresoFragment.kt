package com.seba.malosh.fragments.progresos

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.seba.malosh.R
import com.seba.malosh.activities.BienvenidaActivity
import java.util.concurrent.TimeUnit

class VerProgresoFragment : Fragment() {

    private lateinit var progresoDescripcion: TextView
    private lateinit var progresoChecklist: LinearLayout
    private lateinit var desafioCompletadoButton: Button
    private lateinit var tiempoRestanteTextView: TextView
    private var nombreDesafio: String? = null
    private val handler = Handler()
    private var tiempoRestante: Long = 0L
    private var tiempoEntrePasos: Long = 3600000 // 1 hora (3600000 ms)
    private val tiempoDesafio: Long = 86400000 // 24 horas en milisegundos

    companion object {
        private const val DESAFIO_NOMBRE_KEY = "desafio_nombre"
        private const val HORA_INICIO_KEY = "hora_inicio"
        private const val CHECKBOX_STATE_KEY = "checkbox_state"

        fun newInstance(desafioNombre: String): VerProgresoFragment {
            val fragment = VerProgresoFragment()
            val args = Bundle()
            args.putString(DESAFIO_NOMBRE_KEY, desafioNombre)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ver_progreso, container, false)

        progresoDescripcion = view.findViewById(R.id.progresoDescripcion)
        progresoChecklist = view.findViewById(R.id.progresoChecklist)
        desafioCompletadoButton = view.findViewById(R.id.desafioCompletadoButton)
        tiempoRestanteTextView = view.findViewById(R.id.tiempoRestanteTextView)

        nombreDesafio = arguments?.getString(DESAFIO_NOMBRE_KEY)
        progresoDescripcion.text = nombreDesafio ?: "No hay un desafío en progreso"

        desafioCompletadoButton.isEnabled = false

        agregarCheckboxes()
        calcularTiempoRestante()
        configurarDesafioCompletadoButton()

        return view
    }

    private fun agregarCheckboxes() {
        val pasos = listOf("Paso 1: Iniciar", "Paso 2: En progreso", "Paso 3: Casi completo", "Paso 4: Completo")
        val sharedPreferences = requireActivity().getSharedPreferences("ProgresoPrefs", Context.MODE_PRIVATE)

        for ((index, paso) in pasos.withIndex()) {
            val checkBox = CheckBox(context).apply {
                text = paso
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.black))
                isEnabled = false
                isChecked = sharedPreferences.getBoolean(CHECKBOX_STATE_KEY + index, false)
            }
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean(CHECKBOX_STATE_KEY + index, isChecked).apply()
                verificarCheckboxes()
            }
            progresoChecklist.addView(checkBox)
        }
    }

    private fun calcularTiempoRestante() {
        val sharedPreferences = requireContext().getSharedPreferences("ProgresoPrefs", Context.MODE_PRIVATE)
        val horaInicio = sharedPreferences.getLong(HORA_INICIO_KEY, System.currentTimeMillis())

        val runnable = object : Runnable {
            override fun run() {
                val tiempoActual = System.currentTimeMillis()
                tiempoRestante = tiempoEntrePasos * progresoChecklist.childCount - (tiempoActual - horaInicio)

                if (tiempoRestante > 0) {
                    val horas = TimeUnit.MILLISECONDS.toHours(tiempoRestante)
                    val minutos = TimeUnit.MILLISECONDS.toMinutes(tiempoRestante) % 60
                    val segundos = TimeUnit.MILLISECONDS.toSeconds(tiempoRestante) % 60
                    tiempoRestanteTextView.text = String.format("Tiempo restante: %02d:%02d:%02d", horas, minutos, segundos)
                    handler.postDelayed(this, 1000)
                } else {
                    tiempoRestanteTextView.text = "¡Desafío completado!"
                    verificarCheckboxes()
                }
            }
        }

        handler.post(runnable)
    }

    private fun verificarCheckboxes() {
        val todosMarcados = progresoChecklist.children
            .filterIsInstance<CheckBox>()
            .all { it.isChecked }

        if (todosMarcados) {
            desafioCompletadoButton.isEnabled = true
        }
    }

    private fun configurarDesafioCompletadoButton() {
        desafioCompletadoButton.setOnClickListener {
            Toast.makeText(context, "¡Desafío Completado!", Toast.LENGTH_SHORT).show()
            desbloquearLogro(requireContext(), "Desafío Diario Completado")
            iniciarTemporizador24Horas()
        }
    }

    private fun iniciarTemporizador24Horas() {
        val sharedPreferences = requireContext().getSharedPreferences("ProgresoPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(HORA_INICIO_KEY, System.currentTimeMillis()).apply()
        calcularTiempoRestante() // Reiniciar el temporizador para el próximo desafío
    }

    private fun desbloquearLogro(context: Context, logro: String) {
        val sharedPreferences = context.getSharedPreferences("logros_prefs", Context.MODE_PRIVATE)
        val logroDesbloqueado = sharedPreferences.getBoolean(logro, false)
        if (!logroDesbloqueado) {
            Toast.makeText(context, "¡Logro Desbloqueado: $logro!", Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putBoolean(logro, true).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}

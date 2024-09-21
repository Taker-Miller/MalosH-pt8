package com.seba.malosh.fragments.desafios

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.seba.malosh.R
import com.seba.malosh.activities.BienvenidaActivity
import com.seba.malosh.fragments.progresos.VerProgresoFragment
import java.util.*

class DesafiosDiariosFragment : Fragment() {

    private lateinit var contenedorDesafios: LinearLayout
    private lateinit var volverButton: Button
    private lateinit var aceptarDesafioButton: Button
    private lateinit var cancelarDesafioButton: Button
    private lateinit var verProgresoButton: Button
    private lateinit var desafioDescripcion: TextView

    private val desafiosList = mutableListOf<String>()
    private var currentDesafio: String? = null
    private var desafioEnProgreso = false
    private lateinit var registeredHabits: ArrayList<String>

    companion object {
        private const val HABITOS_KEY = "habitos_registrados"
        private const val DESAFIO_EN_PROGRESO_KEY = "desafio_en_progreso"

        fun newInstance(habits: ArrayList<String>): DesafiosDiariosFragment {
            val fragment = DesafiosDiariosFragment()
            val bundle = Bundle()
            bundle.putStringArrayList(HABITOS_KEY, habits)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_desafios_diarios, container, false)

        contenedorDesafios = view.findViewById(R.id.contenedorDesafios)
        volverButton = view.findViewById(R.id.volverButton)
        aceptarDesafioButton = view.findViewById(R.id.aceptarDesafioButton)
        cancelarDesafioButton = view.findViewById(R.id.cancelarDesafioButton)
        verProgresoButton = view.findViewById(R.id.verProgresoButton)
        desafioDescripcion = view.findViewById(R.id.desafioDescripcion)

        // Obtener los hábitos registrados del bundle
        registeredHabits = arguments?.getStringArrayList(HABITOS_KEY) ?: arrayListOf()

        // Verificar si hay un desafío en progreso
        val desafioGuardado = obtenerDesafioEnProgreso(requireContext())
        if (desafioGuardado != null) {
            currentDesafio = desafioGuardado
            desafioEnProgreso = true
            aceptarDesafioButton.isEnabled = false
            cancelarDesafioButton.visibility = View.VISIBLE
            verProgresoButton.visibility = View.VISIBLE
            desafioDescripcion.text = "Desafío en progreso: $currentDesafio"
        } else {
            // Generar nuevos desafíos si no hay un desafío en progreso
            generarDesafios(registeredHabits)
            mostrarDesafio()
            cancelarDesafioButton.visibility = View.GONE // Ocultar el botón si no hay desafío en progreso
            verProgresoButton.visibility = View.GONE // Ocultar botón de ver progreso si no hay desafío en progreso
        }

        aceptarDesafioButton.setOnClickListener {
            if (desafioEnProgreso) {
                Toast.makeText(
                    context,
                    "Ya tienes un desafío en progreso. Finaliza o cancela el desafío actual primero.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                desafioEnProgreso = true
                guardarDesafioEnProgreso(requireContext(), currentDesafio ?: "")
                Toast.makeText(context, "¡Desafío aceptado!", Toast.LENGTH_SHORT).show()

                // Obtener la duración del desafío
                val duracionDesafio = obtenerDuracionDesafio(currentDesafio ?: "")
                val sharedPreferences = requireContext().getSharedPreferences("desafio_prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putLong("hora_inicio", System.currentTimeMillis())
                    .putLong("duracion_desafio", duracionDesafio)
                    .apply()

                cancelarDesafioButton.visibility = View.VISIBLE // Mostrar el botón de cancelar
                verProgresoButton.visibility = View.VISIBLE // Mostrar el botón de ver progreso
            }
        }

        cancelarDesafioButton.setOnClickListener {
            cancelarDesafio()
        }

        verProgresoButton.setOnClickListener {
            // Redirigir al fragmento de ver progreso
            val fragment = VerProgresoFragment.newInstance(currentDesafio ?: "")
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        volverButton.setOnClickListener {
            (activity as? BienvenidaActivity)?.mostrarElementosUI()
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    // Método para obtener la duración de cada desafío en milisegundos
    private fun obtenerDuracionDesafio(desafio: String): Long {
        return when (desafio) {
            // Duraciones en milisegundos para desafíos con límites de horas
            "No fumes durante las próximas 3 horas." -> 3 * 60 * 60 * 1000 // 3 horas
            "No tomes café en las próximas 2 horas." -> 2 * 60 * 60 * 1000 // 2 horas
            "No consumas alcohol por 2 horas." -> 2 * 60 * 60 * 1000 // 2 horas
            "No consumas más de un vaso de alcohol durante 4 horas." -> 4 * 60 * 60 * 1000 // 4 horas
            "No fumes en las próximas 5 horas." -> 5 * 60 * 60 * 1000 // 5 horas
            "No comas nada durante las próximas 3 horas." -> 3 * 60 * 60 * 1000 // 3 horas

            // Desafíos que no tienen límite de horas explícito duran 24 horas
            else -> 24 * 60 * 60 * 1000 // 24 horas por defecto
        }
    }

    private fun generarDesafios(habitos: List<String>) {
        desafiosList.clear()

        for (habito in habitos) {
            val habitLowerCase = habito.lowercase().trim()
            Log.d("HABITO_REGISTRADO", "Comparando hábito registrado: $habitLowerCase")
            when (habitLowerCase) {
                "cafeína", "consumo de cafeína" -> desafiosList.addAll(generarDesafiosCafeina())
                "dormir mal", "dormir a deshoras" -> desafiosList.addAll(generarDesafiosDormirMal())
                "mala alimentación" -> desafiosList.addAll(generarDesafiosMalaAlimentacion())
                "comer a deshoras" -> desafiosList.addAll(generarDesafiosComerADeshoras())
                "poco ejercicio" -> desafiosList.addAll(generarDesafiosPocoEjercicio())
                "alcohol" -> desafiosList.addAll(generarDesafiosAlcohol())
                "fumar" -> desafiosList.addAll(generarDesafiosFumar())
                "mala higiene" -> desafiosList.addAll(generarDesafiosMalaHigiene())
                "interrumpir", "interrumpir a otros" -> desafiosList.addAll(generarDesafiosInterrumpir())
                "no beber agua" -> desafiosList.addAll(generarDesafiosNoBeberAgua())
                else -> Log.w(
                    "HABITO_NO_RECONOCIDO",
                    "No se encontraron desafíos para el hábito: $habitLowerCase"
                )
            }
        }

        desafiosList.shuffle()
    }

    private fun mostrarDesafio() {
        if (desafiosList.isNotEmpty()) {
            currentDesafio = desafiosList.first()
            contenedorDesafios.removeAllViews()
            val textView = TextView(context).apply {
                text = currentDesafio
                textSize = 18f
                setTextColor(resources.getColor(android.R.color.white))
            }
            contenedorDesafios.addView(textView)
        } else {
            Toast.makeText(context, "No hay desafíos disponibles.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelarDesafio() {
        // Limpiar el desafío guardado y resetear la interfaz
        val sharedPreferences = requireContext().getSharedPreferences("desafio_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(DESAFIO_EN_PROGRESO_KEY).apply()

        desafioEnProgreso = false
        currentDesafio = null
        aceptarDesafioButton.isEnabled = true
        cancelarDesafioButton.visibility = View.GONE // Ocultar el botón de cancelar
        verProgresoButton.visibility = View.GONE // Ocultar el botón de ver progreso
        desafioDescripcion.text = "No tienes ningún desafío en progreso. Acepta uno para comenzar."
        generarDesafios(registeredHabits) // Generar nuevos desafíos
        mostrarDesafio()
    }

    private fun guardarDesafioEnProgreso(context: Context, desafio: String) {
        val sharedPreferences =
            context.getSharedPreferences("desafio_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(DESAFIO_EN_PROGRESO_KEY, desafio).apply()
    }

    private fun obtenerDesafioEnProgreso(context: Context): String? {
        val sharedPreferences =
            context.getSharedPreferences("desafio_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString(DESAFIO_EN_PROGRESO_KEY, null)
    }

    // Métodos para generar los desafíos de cada categoría
    private fun generarDesafiosCafeina(): List<String> {
        return listOf(
            "No tomes café en las próximas 2 horas.",
            "Reemplaza el café de la tarde con agua.",
            "No consumas cafeína después del mediodía.",
            "Evita el café mientras trabajas hoy.",
            "Reduce tu ingesta de café a una taza al día.",
            "Reemplaza el café con té durante la mañana.",
            "No tomes bebidas energéticas hoy."
        )
    }

    private fun generarDesafiosDormirMal(): List<String> {
        return listOf(
            "No duermas durante el día.",
            "Duerme al menos 7 horas esta noche.",
            "Apaga tus dispositivos electrónicos 30 minutos antes de dormir.",
            "Evita tomar café después de las 6 p.m.",
            "Realiza una rutina de relajación antes de dormir.",
            "Acuéstate antes de las 11 p.m.",
            "Despiértate a la misma hora mañana."
        )
    }

    private fun generarDesafiosMalaAlimentacion(): List<String> {
        return listOf(
            "Come una comida casera hoy.",
            "Incluye verduras en tu almuerzo.",
            "Evita la comida rápida durante todo el día.",
            "Come 3 comidas balanceadas hoy.",
            "Reemplaza los snacks poco saludables por frutas.",
            "Reduce el consumo de azúcares en tu próxima comida.",
            "Come sin distracciones como la TV o el teléfono."
        )
    }

    private fun generarDesafiosComerADeshoras(): List<String> {
        return listOf(
            "No comas después de las 10 p.m.",
            "Establece horarios regulares para tus comidas.",
            "No comas nada entre comidas durante las próximas 3 horas.",
            "Desayuna dentro de la primera hora de despertar.",
            "Evita comer snacks después de la cena.",
            "Come tus tres comidas a la misma hora todos los días.",
            "No comas nada durante las próximas 2 horas."
        )
    }

    private fun generarDesafiosPocoEjercicio(): List<String> {
        return listOf(
            "Realiza una caminata de 30 minutos hoy.",
            "Haz 15 minutos de estiramientos en casa.",
            "Realiza 10 flexiones en tu descanso.",
            "Sube las escaleras en lugar de usar el ascensor.",
            "Realiza una rutina rápida de ejercicios al levantarte.",
            "Haz al menos 20 sentadillas hoy.",
            "Camina en lugar de conducir si es posible hoy."
        )
    }

    private fun generarDesafiosAlcohol(): List<String> {
        return listOf(
            "No beber alcohol por 2 horas.",
            "No consumir alcohol durante todo el día.",
            "Evita tomar más de un vaso de alcohol durante 4 horas.",
            "No consumas bebidas alcohólicas hasta la noche.",
            "No tomes alcohol mientras estás en una reunión social.",
            "Reemplaza el alcohol con agua en tu siguiente comida.",
            "No consumas bebidas alcohólicas hoy."
        )
    }

    private fun generarDesafiosFumar(): List<String> {
        return listOf(
            "No fumes durante las próximas 3 horas.",
            "Evita fumar un cigarrillo después del almuerzo.",
            "Intenta reducir tu consumo de cigarrillos a la mitad hoy.",
            "No fumes en las próximas 5 horas.",
            "Fuma solo la mitad de tu cigarrillo en tu siguiente descanso.",
            "Evita fumar mientras trabajas.",
            "No fumes en espacios cerrados durante todo el día."
        )
    }

    private fun generarDesafiosMalaHigiene(): List<String> {
        return listOf(
            "Cepilla tus dientes después de cada comida hoy.",
            "Lávate las manos antes y después de cada comida.",
            "Dedica 10 minutos a limpiar tu espacio personal.",
            "Toma una ducha antes de acostarte.",
            "Lávate la cara cada mañana.",
            "Lávate las manos cada vez que salgas del baño.",
            "Realiza una limpieza rápida de tu habitación."
        )
    }

    private fun generarDesafiosInterrumpir(): List<String> {
        return listOf(
            "Evita interrumpir a alguien durante una conversación.",
            "Escucha activamente sin interrumpir a nadie hoy.",
            "No hables hasta que alguien haya terminado de hablar.",
            "Intenta no interrumpir durante una reunión hoy.",
            "Deja que los demás hablen primero en una conversación.",
            "Espera 5 segundos antes de responder a alguien.",
            "Haz un esfuerzo por dejar que otros expresen sus ideas primero."
        )
    }

    private fun generarDesafiosNoBeberAgua(): List<String> {
        return listOf(
            "Bebe un vaso de agua en los próximos 30 minutos.",
            "Toma al menos 8 vasos de agua hoy.",
            "Bebe agua en lugar de jugos o refrescos.",
            "Comienza cada comida con un vaso de agua.",
            "Bebe agua antes de cada comida.",
            "Llena tu botella de agua y bébela completamente hoy.",
            "Bebe un vaso de agua justo antes de dormir."
        )
    }
}

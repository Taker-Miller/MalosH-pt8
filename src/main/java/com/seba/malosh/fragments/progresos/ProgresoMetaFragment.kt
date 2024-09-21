package com.seba.malosh.fragments.progresos

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.seba.malosh.R
import java.text.SimpleDateFormat
import java.util.*

class ProgresoMetaFragment : Fragment() {

    private lateinit var calendarioMeta: CalendarView
    private lateinit var estadoDiaTextView: TextView
    private lateinit var mesSpinner: Spinner
    private lateinit var habitos: ArrayList<String>
    private var fechaInicio: Long = 0
    private var fechaFin: Long = 0
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // Formato para el Spinner de meses
    private var mesActual: Int = Calendar.getInstance().get(Calendar.MONTH) // Mes actual del dispositivo
    private var añoActual: Int = Calendar.getInstance().get(Calendar.YEAR) // Año actual del dispositivo
    private var mesSeleccionado: Int = mesActual // Mes seleccionado por el usuario
    private var añoSeleccionado: Int = añoActual // Año seleccionado por el usuario

    // Usaremos SharedPreferences para almacenar el estado de los días
    private val PREF_NAME = "ProgresoMetaPrefs"
    private val DIA_COMPLETADO_KEY = "dia_completado"

    companion object {
        private const val FECHA_INICIO_KEY = "fecha_inicio"
        private const val FECHA_FIN_KEY = "fecha_fin"
        private const val HABITOS_KEY = "habitos"

        fun newInstance(fechaInicio: Long, fechaFin: Long, habitos: ArrayList<String>): ProgresoMetaFragment {
            val fragment = ProgresoMetaFragment()
            val args = Bundle()
            args.putLong(FECHA_INICIO_KEY, fechaInicio)
            args.putLong(FECHA_FIN_KEY, fechaFin)
            args.putStringArrayList(HABITOS_KEY, habitos)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progreso_meta, container, false)

        calendarioMeta = view.findViewById(R.id.calendarioMeta)
        estadoDiaTextView = view.findViewById(R.id.estadoDiaTextView)
        mesSpinner = view.findViewById(R.id.mesSpinner)

        // Obtener las fechas y hábitos desde los argumentos
        fechaInicio = arguments?.getLong(FECHA_INICIO_KEY) ?: 0L
        fechaFin = arguments?.getLong(FECHA_FIN_KEY) ?: 0L
        habitos = arguments?.getStringArrayList(HABITOS_KEY) ?: arrayListOf()

        configurarCalendario()
        configurarMesesSpinner()

        return view
    }

    // Configura el calendario
    private fun configurarCalendario() {
        // Configurar la fecha mínima y máxima en el calendario
        calendarioMeta.minDate = fechaInicio
        calendarioMeta.maxDate = fechaFin

        // Marcar automáticamente los días pasados como "Fallido"
        marcarDiasPasados()

        // Listener de selección de fecha: solo permite marcar el día actual si está en el mes actual
        calendarioMeta.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val today = Calendar.getInstance()

            val fechaSeleccionada = dateFormat.format(selectedDate.time)

            // Solo permitir marcar el día si es el día actual y el mes actual está seleccionado
            if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                mesSeleccionado == mesActual && añoSeleccionado == añoActual) {
                mostrarDialogoEstadoDia(fechaSeleccionada)
            } else {
                Toast.makeText(context, "Solo puedes marcar el día actual en el mes actual.", Toast.LENGTH_SHORT).show()
            }
        }

        // Mostrar los días guardados
        mostrarEstadoDiasGuardados()
    }

    // Configura el Spinner con los meses
    private fun configurarMesesSpinner() {
        val mesesList = obtenerMesesDentroDeRango()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mesesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mesSpinner.adapter = adapter

        // Listener para actualizar el estado de los días según el mes seleccionado
        mesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val mesSeleccionadoNombre = mesesList[position]
                val calendar = Calendar.getInstance()

                calendar.timeInMillis = fechaInicio
                val mes = obtenerMesDesdeString(mesSeleccionadoNombre)

                calendar.set(Calendar.MONTH, mes)
                añoSeleccionado = calendar.get(Calendar.YEAR)
                mesSeleccionado = mes

                mostrarEstadoDiasParaMes(mesSeleccionadoNombre)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada si no se selecciona ningún mes
            }
        }
    }

    // Mostrar el estado de los días para el mes seleccionado
    private fun mostrarEstadoDiasParaMes(mesSeleccionado: String) {
        val calendar = Calendar.getInstance()
        val estados = StringBuilder()

        // Iterar sobre los días del mes seleccionado
        var fechaActual = fechaInicio
        while (fechaActual <= fechaFin) {
            calendar.timeInMillis = fechaActual
            val fechaFormateada = dateFormat.format(calendar.time)
            val mesFormateado = monthFormat.format(calendar.time)

            if (mesFormateado == mesSeleccionado) {
                val estado = obtenerEstadoDia(fechaFormateada)
                if (estado != null) {
                    estados.append("Día: $fechaFormateada - Estado: $estado\n")
                }
            }

            // Avanzar un día
            fechaActual += 24 * 60 * 60 * 1000 // Sumar 1 día en milisegundos
        }

        estadoDiaTextView.text = estados.toString()
    }

    // Obtener la lista de meses dentro del rango de fechas
    private fun obtenerMesesDentroDeRango(): List<String> {
        val mesesList = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        // Iterar sobre los meses dentro del rango
        var fechaActual = fechaInicio
        while (fechaActual <= fechaFin) {
            calendar.timeInMillis = fechaActual
            val mesFormateado = monthFormat.format(calendar.time)
            if (!mesesList.contains(mesFormateado)) {
                mesesList.add(mesFormateado)
            }

            // Avanzar un mes
            calendar.add(Calendar.MONTH, 1)
            fechaActual = calendar.timeInMillis
        }

        return mesesList
    }

    // Mostrar un diálogo para marcar el día como completado o fallido
    private fun mostrarDialogoEstadoDia(fecha: String) {
        val opciones = arrayOf("Completado", "Fallido")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("¿Cómo te fue el $fecha?")
        builder.setItems(opciones) { _, which ->
            val estado = if (which == 0) "Completado" else "Fallido"

            // Guardar el estado del día en SharedPreferences
            guardarEstadoDia(fecha, estado)

            Toast.makeText(context, "Día $fecha marcado como $estado", Toast.LENGTH_SHORT).show()

            // Actualizar la vista con el estado del día
            mostrarEstadoDiasGuardados()
        }
        builder.show()
    }

    // Marcar automáticamente los días pasados como "Fallido"
    private fun marcarDiasPasados() {
        val calendar = Calendar.getInstance()
        var fechaActual = fechaInicio
        val hoy = Calendar.getInstance().timeInMillis

        // Iterar sobre los días desde la fecha de inicio hasta hoy
        while (fechaActual < hoy && fechaActual <= fechaFin) {
            calendar.timeInMillis = fechaActual
            val fechaFormateada = dateFormat.format(calendar.time)

            if (obtenerEstadoDia(fechaFormateada) == null) {
                guardarEstadoDia(fechaFormateada, "Fallido")
            }

            fechaActual += 24 * 60 * 60 * 1000 // Avanzar un día
        }
    }

    // Guardar el estado del día en SharedPreferences
    private fun guardarEstadoDia(fecha: String, estado: String) {
        val sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("$DIA_COMPLETADO_KEY-$fecha", estado)
        editor.apply()
    }

    // Obtener el estado del día de SharedPreferences
    private fun obtenerEstadoDia(fecha: String): String? {
        val sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString("$DIA_COMPLETADO_KEY-$fecha", null)
    }

    // Mostrar el estado de los días guardados
    private fun mostrarEstadoDiasGuardados() {
        val calendar = Calendar.getInstance()
        val estados = StringBuilder()

        // Iterar sobre las fechas dentro del rango de fechas
        var fechaActual = fechaInicio
        while (fechaActual <= fechaFin) {
            calendar.timeInMillis = fechaActual
            val fechaFormateada = dateFormat.format(calendar.time)
            val estado = obtenerEstadoDia(fechaFormateada)

            if (estado != null) {
                estados.append("Día: $fechaFormateada - Estado: $estado\n")
            }

            fechaActual += 24 * 60 * 60 * 1000 // Avanzar un día
        }

        estadoDiaTextView.text = estados.toString()
    }

    // Método para obtener el mes seleccionado en formato entero desde el nombre del mes
    private fun obtenerMesDesdeString(mes: String): Int {
        return when (mes) {
            "Enero" -> Calendar.JANUARY
            "Febrero" -> Calendar.FEBRUARY
            "Marzo" -> Calendar.MARCH
            "Abril" -> Calendar.APRIL
            "Mayo" -> Calendar.MAY
            "Junio" -> Calendar.JUNE
            "Julio" -> Calendar.JULY
            "Agosto" -> Calendar.AUGUST
            "Septiembre" -> Calendar.SEPTEMBER
            "Octubre" -> Calendar.OCTOBER
            "Noviembre" -> Calendar.NOVEMBER
            "Diciembre" -> Calendar.DECEMBER
            else -> Calendar.JANUARY
        }
    }
}

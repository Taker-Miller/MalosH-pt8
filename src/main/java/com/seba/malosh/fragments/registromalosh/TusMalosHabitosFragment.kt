package com.seba.malosh.fragments.registromalosh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.seba.malosh.R
import com.seba.malosh.activities.BienvenidaActivity

class TusMalosHabitosFragment : Fragment() {

    private lateinit var malosHabitosTextView: TextView
    private lateinit var volverButton: Button

    companion object {
        private const val HABITOS_KEY = "habitos"

        // Método para crear una nueva instancia del fragmento con los hábitos registrados
        fun newInstance(habitos: ArrayList<String>): TusMalosHabitosFragment {
            val fragment = TusMalosHabitosFragment()
            val bundle = Bundle()
            bundle.putStringArrayList(HABITOS_KEY, habitos)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tus_malos_habitos, container, false)

        malosHabitosTextView = view.findViewById(R.id.malosHabitosTextView)
        volverButton = view.findViewById(R.id.volverButton)

        // Obtener los hábitos pasados al fragmento
        val habitos = arguments?.getStringArrayList(HABITOS_KEY)

        // Mostrar los hábitos registrados
        malosHabitosTextView.text = habitos?.joinToString(separator = "\n") ?: "No has registrado hábitos aún."

        // Acción para el botón Volver
        volverButton.setOnClickListener {
            (activity as? BienvenidaActivity)?.mostrarElementosUI() // Mostrar el logo, descripción y sugerencia
            requireActivity().supportFragmentManager.popBackStack() // Regresar a la pantalla anterior
        }

        return view
    }
}

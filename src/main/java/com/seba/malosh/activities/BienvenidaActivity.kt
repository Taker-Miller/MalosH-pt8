package com.seba.malosh.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentTransaction
import com.seba.malosh.fragments.desafios.DesafiosDiariosFragment
import com.seba.malosh.fragments.metas.MetasFragment
import com.seba.malosh.receivers.PlanInicioReceiver
import com.seba.malosh.fragments.progresos.ProgresoFragment
import com.seba.malosh.R
import com.seba.malosh.fragments.registromalosh.SeleccionarHabitosFragment
import com.seba.malosh.fragments.registromalosh.TusMalosHabitosFragment
import java.util.*

class BienvenidaActivity : AppCompatActivity() {

    // Lista de hábitos registrados
    private val registeredHabits = ArrayList<String>()
    private var planEnProgreso = false // Variable para controlar si ya hay un plan en progreso

    // Referencias a los elementos UI que se ocultarán
    private lateinit var logoImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var notificationAdviceTextView: TextView // Nuevo TextView para el mensaje sobre las notificaciones

    // Registro para manejar la actividad de configuración de alarmas exactas
    private val scheduleExactAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isExactAlarmPermissionGranted()) {
            Toast.makeText(this, "Permiso de alarma exacta otorgado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de alarma exacta denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bienvenida)

        // Configurar la Toolbar como ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Menú"

        // Referenciar los elementos que se van a ocultar
        logoImageView = findViewById(R.id.logoImageView)
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        notificationAdviceTextView = findViewById(R.id.notificationAdviceTextView)
    }

    // Inflar el menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_navegacion, menu)
        return true
    }

    // Manejar las acciones del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_registrar_habitos -> {
                if (registeredHabits.size >= 4) {
                    Toast.makeText(this, "Ya has registrado el máximo de hábitos permitidos", Toast.LENGTH_SHORT).show()
                } else {
                    ocultarElementosUI()
                    val maxHabitosPermitidos = 4 - registeredHabits.size
                    val fragment = SeleccionarHabitosFragment.newInstance(
                        registeredHabits,
                        maxHabitosPermitidos
                    )
                    val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
                true
            }
            R.id.menu_item_metas -> {
                if (planEnProgreso) {
                    Toast.makeText(this, "Debes completar la meta actual antes de definir una nueva.", Toast.LENGTH_SHORT).show()
                } else if (registeredHabits.size < 2) {
                    Toast.makeText(this, "Debes registrar al menos 2 hábitos para definir metas.", Toast.LENGTH_SHORT).show()
                } else {
                    ocultarElementosUI()
                    val fragment = MetasFragment.newInstance(registeredHabits)
                    val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
                true
            }
            R.id.menu_item_desafios -> {
                if (registeredHabits.isEmpty()) {
                    Toast.makeText(this, "Primero debes registrar tus malos hábitos para comenzar los desafíos diarios.", Toast.LENGTH_LONG).show()
                } else {
                    ocultarElementosUI()
                    val fragment = DesafiosDiariosFragment.newInstance(registeredHabits)
                    val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
                true
            }
            R.id.menu_item_progreso -> {
                // Mostrar la sección de progreso
                ocultarElementosUI()
                val fragment = ProgresoFragment() // Asegúrate de que este fragment esté correctamente implementado
                val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
                true
            }
            R.id.menu_item_tus_habitos -> {
                ocultarElementosUI()
                val fragment = TusMalosHabitosFragment.newInstance(registeredHabits)
                val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
                true
            }
            R.id.menu_item_logout -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Ocultar los elementos UI principales
    private fun ocultarElementosUI() {
        logoImageView.visibility = ImageView.GONE
        titleTextView.visibility = TextView.GONE
        descriptionTextView.visibility = TextView.GONE
        notificationAdviceTextView.visibility = TextView.GONE
    }

    // Mostrar los elementos UI principales
    fun mostrarElementosUI() {
        logoImageView.visibility = ImageView.VISIBLE
        titleTextView.visibility = TextView.VISIBLE
        descriptionTextView.visibility = TextView.VISIBLE
        notificationAdviceTextView.visibility = TextView.VISIBLE
    }

    // Método para actualizar la lista de hábitos registrados
    fun updateRegisteredHabits(newHabits: List<String>, fechaInicio: Calendar?) {
        registeredHabits.addAll(newHabits) // Agregar los nuevos hábitos a la lista
        fechaInicio?.let {
            if (isExactAlarmPermissionGranted()) {
                programarNotificacion(it)
            } else {
                requestExactAlarmPermission()
            }
        }
        mostrarElementosUI()
    }

    // Método para marcar que un plan ha comenzado
    fun comenzarPlan() {
        planEnProgreso = true // Marcar que un plan está en progreso
        mostrarElementosUI() // Mostrar el logo, la descripción y la sugerencia
    }

    // Método para finalizar el plan y permitir que el usuario defina nuevas metas
    fun finalizarPlan() {
        planEnProgreso = false // Desbloquear la opción para definir nuevas metas
        mostrarElementosUI() // Mostrar el logo y la descripción nuevamente
    }

    override fun onBackPressed() {
        mostrarElementosUI()
        super.onBackPressed()
    }

    private fun programarNotificacion(fechaInicio: Calendar) {
        val alarmManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        } else {
            null // Agrega el manejo para versiones inferiores si es necesario
        }
        val intent = Intent(this, PlanInicioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, fechaInicio.timeInMillis, pendingIntent)
        Toast.makeText(this, "Notificación programada para la fecha seleccionada", Toast.LENGTH_SHORT).show()
    }

    private fun isExactAlarmPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }


    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            scheduleExactAlarmLauncher.launch(intent)
        }
    }
}

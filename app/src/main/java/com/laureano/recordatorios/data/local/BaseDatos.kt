package com.laureano.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Versión 2: se agregó el uid de la cuenta y se quitó el id del servicio REST de la Actividad 3
@Database(entities = [RecordatorioEntity::class], version = 2, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var instancia: BaseDatos? = null

        // Una única instancia para toda la aplicación
        fun obtener(context: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatos::class.java,
                    "recordatorios.db"
                )
                    // Los datos de la versión 1 no tenían dueño: se descartan y se bajan de la nube
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}

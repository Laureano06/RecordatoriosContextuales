package com.laureano.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordatorioEntity::class], version = 1, exportSchema = false)
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
                ).build().also { instancia = it }
            }
    }
}

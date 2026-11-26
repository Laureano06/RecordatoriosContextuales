package com.laureano.recordatorios

import com.laureano.recordatorios.data.aDominio
import com.laureano.recordatorios.data.aDto
import com.laureano.recordatorios.data.aEntityOrNull
import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.RecordatorioDto
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.EstadoSync
import com.laureano.recordatorios.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeosTest {

    private val entidad = RecordatorioEntity(
        id = 7,
        uuid = "abc",
        remotoId = 20,
        titulo = "Pagar la luz",
        categoriaId = 4,
        prioridad = "ALTA",
        estado = "CANCELADO",
        motivoCancelacion = "Ya se pagó",
        actualizadoEn = 1000,
        estadoSync = EstadoSyncLocal.PENDIENTE_ACTUALIZAR
    )

    @Test
    fun entityADominio() {
        val dominio = entidad.aDominio()
        assertEquals("Hogar", dominio.categoria.nombre)
        assertEquals(Prioridad.Alta, dominio.prioridad)
        assertEquals(EstadoRecordatorio.Cancelado("Ya se pagó"), dominio.estado)
        assertEquals(EstadoSync.Pendiente, dominio.estadoSync)
    }

    @Test
    fun ida_y_vuelta_entity_dto_conserva_los_datos() {
        val vuelta = entidad.aDto().aEntityOrNull()!!
        assertEquals(entidad.copy(id = 0, estadoSync = EstadoSyncLocal.SINCRONIZADO), vuelta)
    }

    @Test
    fun dtoIncompletoODesconocidoSeDescarta() {
        val valido = entidad.aDto()
        assertNull(valido.copy(uuid = null).aEntityOrNull())
        assertNull(valido.copy(id = null).aEntityOrNull())
        assertNull(valido.copy(prioridad = "URGENTE").aEntityOrNull())
        assertNull(valido.copy(categoriaId = 99).aEntityOrNull())
        assertNull(RecordatorioDto().aEntityOrNull())
    }

    @Test
    fun conErrorSeMuestraComoError() {
        val conError = entidad.copy(ultimoError = "HTTP 400").aDominio()
        assertTrue(conError.estadoSync == EstadoSync.ConError)
    }
}

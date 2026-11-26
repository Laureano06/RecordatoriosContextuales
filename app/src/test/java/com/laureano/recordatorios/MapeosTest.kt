package com.laureano.recordatorios

import com.laureano.recordatorios.data.aDominio
import com.laureano.recordatorios.data.aEntityOrNull
import com.laureano.recordatorios.data.aRemoto
import com.laureano.recordatorios.data.local.EstadoSyncLocal
import com.laureano.recordatorios.data.local.RecordatorioEntity
import com.laureano.recordatorios.data.remote.CodigoFirestore
import com.laureano.recordatorios.data.remote.FalloRemoto
import com.laureano.recordatorios.data.remote.aMapa
import com.laureano.recordatorios.data.remote.falloDesdeCodigo
import com.laureano.recordatorios.data.remote.remotoDesdeMapa
import com.laureano.recordatorios.domain.EstadoRecordatorio
import com.laureano.recordatorios.domain.EstadoSync
import com.laureano.recordatorios.domain.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapeosTest {

    private val entidad = RecordatorioEntity(
        id = 7,
        uid = "ana",
        uuid = "abc",
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
    fun idaYVueltaPorFirestoreConservaLosDatos() {
        val documento = entidad.aRemoto().aMapa()
        val vuelta = remotoDesdeMapa(documento)!!.aEntityOrNull("ana")!!
        assertEquals(entidad.copy(id = 0, estadoSync = EstadoSyncLocal.SINCRONIZADO), vuelta)
    }

    @Test
    fun documentoIncompletoODeOtraCuentaSeDescarta() {
        val documento = entidad.aRemoto().aMapa()
        assertNull(remotoDesdeMapa(documento - "uuid"))
        assertNull(remotoDesdeMapa(documento + ("categoriaId" to "cuatro")))
        assertNull(remotoDesdeMapa(documento)!!.aEntityOrNull("bruno"))
        assertNull(remotoDesdeMapa(documento + ("prioridad" to "URGENTE"))!!.aEntityOrNull("ana"))
    }

    @Test
    fun codigosDeFirestoreSeTraducen() {
        assertEquals(FalloRemoto.SinPermiso, falloDesdeCodigo(CodigoFirestore.PERMISSION_DENIED))
        assertEquals(FalloRemoto.SinConexion, falloDesdeCodigo(CodigoFirestore.UNAVAILABLE))
        assertEquals(FalloRemoto.Timeout, falloDesdeCodigo(CodigoFirestore.DEADLINE_EXCEEDED))
        assertEquals(FalloRemoto.SesionInvalida, falloDesdeCodigo(CodigoFirestore.UNAUTHENTICATED))
    }
}

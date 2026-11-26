package com.laureano.recordatorios.data

import com.laureano.recordatorios.domain.ResultadoValidacion
import com.laureano.recordatorios.domain.validar
import java.time.LocalDateTime

// Una sola instancia compartida por todas las pantallas
object Dependencias {
    private val enMemoria = RecordatorioRepositoryEnMemoria(
        iniciales = DatosSimulados.recordatorios.filter {
            it.validar(LocalDateTime.now()) is ResultadoValidacion.Valido
        },
        categorias = DatosSimulados.categorias.toList()
    )

    val repositorio: RecordatorioRepository = enMemoria
    val simulador: ControlSimulacion = enMemoria
}

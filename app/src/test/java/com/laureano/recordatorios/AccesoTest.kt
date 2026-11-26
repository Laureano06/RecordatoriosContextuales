package com.laureano.recordatorios

import com.laureano.recordatorios.ui.acceso.ModoAcceso
import com.laureano.recordatorios.ui.acceso.validarContrasenia
import com.laureano.recordatorios.ui.acceso.validarEmail
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccesoTest {

    @Test
    fun emailValidoEInvalido() {
        assertNull(validarEmail("ana@ejemplo.com"))
        assertNotNull(validarEmail("ana@ejemplo"))
        assertNotNull(validarEmail("ana ejemplo.com"))
    }

    @Test
    fun alRegistrarseLaContraseniaNecesitaSeisCaracteres() {
        assertNotNull(validarContrasenia("12345", ModoAcceso.Registro))
        assertNull(validarContrasenia("123456", ModoAcceso.Registro))
        // Al iniciar sesión no se da pistas: decide Firebase
        assertNull(validarContrasenia("12345", ModoAcceso.IniciarSesion))
    }
}

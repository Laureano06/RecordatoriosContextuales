package com.laureano.recordatorios.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.laureano.recordatorios.ContenedorApp
import com.laureano.recordatorios.RecordatoriosApp

// Los ViewModels obtienen los repositorios del contenedor de la aplicación
fun CreationExtras.contenedor(): ContenedorApp =
    (checkNotNull(this[APPLICATION_KEY]) as RecordatoriosApp).contenedor

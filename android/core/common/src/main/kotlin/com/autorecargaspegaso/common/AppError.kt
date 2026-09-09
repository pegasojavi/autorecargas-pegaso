package com.autorecargaspegaso.common

/**
 * Errores de dominio comunes a todos los módulos (CLAUDE.md sección 9:
 * "Result<T>/sealed interface de dominio, nunca excepciones sin capturar
 * cruzando capas").
 */
sealed interface AppError {
    data object NoConnectivity : AppError
    data class Network(val code: Int?, val message: String?) : AppError
    data class Unexpected(val cause: Throwable) : AppError
}

typealias AppResult<T> = Result<T>

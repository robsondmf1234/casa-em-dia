package br.com.casaemdia.domain.repository

/** Runs a block of repository operations atomically (all succeed or none are applied). */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}

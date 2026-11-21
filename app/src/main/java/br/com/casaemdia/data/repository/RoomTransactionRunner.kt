package br.com.casaemdia.data.repository

import androidx.room.withTransaction
import br.com.casaemdia.data.local.AppDatabase
import br.com.casaemdia.domain.repository.TransactionRunner

class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = db.withTransaction { block() }
}

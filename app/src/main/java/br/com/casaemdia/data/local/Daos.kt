package br.com.casaemdia.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * O Room é só uma cópia local do Firestore: o app grava no Firestore e a sincronização
 * ([br.com.casaemdia.data.remote.FirestoreSync]) substitui estas tabelas a cada mudança.
 */
@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun observeById(id: String): Flow<InventoryItemEntity?>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY purchased, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getById(id: String): ShoppingItemEntity?

    @Query("SELECT id FROM shopping_items WHERE purchased = 1")
    suspend fun purchasedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingItemEntity>)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_category` ON `inventory_items` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_expirationEpochDay` ON `inventory_items` (`expirationEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_inventoryItemId` ON `shopping_items` (`inventoryItemId`)")
    }
}

/**
 * Versão 3: ids passam a ser os ids de texto do Firestore e o estoque ganha `createdAt`.
 * Os dados locais antigos são descartados de propósito (o Firestore é a fonte da verdade e o
 * usuário optou por começar do zero); as tabelas são recriadas vazias.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `inventory_items`")
        db.execSQL("DROP TABLE IF EXISTS `shopping_items`")
        MIGRATION_3_CREATE_SQL.forEach(db::execSQL)
    }
}

@Database(
    entities = [InventoryItemEntity::class, ShoppingItemEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun shoppingDao(): ShoppingDao
}

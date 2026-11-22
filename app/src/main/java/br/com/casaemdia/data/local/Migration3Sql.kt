package br.com.casaemdia.data.local

/** SQL das tabelas na versão 3, copiado do schema gerado pelo Room em `app/schemas/.../3.json`. */
internal val MIGRATION_3_CREATE_SQL = listOf(
    "CREATE TABLE IF NOT EXISTS `inventory_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `minQuantity` REAL NOT NULL, `expirationEpochDay` INTEGER, `location` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_inventory_items_category` ON `inventory_items` (`category`)",
    "CREATE INDEX IF NOT EXISTS `index_inventory_items_expirationEpochDay` ON `inventory_items` (`expirationEpochDay`)",
    "CREATE TABLE IF NOT EXISTS `shopping_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `purchased` INTEGER NOT NULL, `inventoryItemId` TEXT, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_shopping_items_inventoryItemId` ON `shopping_items` (`inventoryItemId`)",
)

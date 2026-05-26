package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bass_compre_mais_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.productDao())
                }
            }
        }

        suspend fun populateDatabase(productDao: ProductDao) {
            val initialProducts = listOf(
                ProductEntity(
                    title = "Relógio Inteligente Smartwatch Sport HD",
                    category = "Eletrônicos",
                    description = "Monitore batimentos cardíacos, passos diários, calorias, sono e receba notificações direto no seu pulso. Tela sensível ao toque de alta definição.",
                    price = 69.90,
                    originalPrice = 199.00,
                    discountPercentage = 65,
                    shopeeUrl = "https://shopee.com.br/Rel%C3%B3gio-Inteligente-Smartwatch-i_123456",
                    imageUrl = "https://images.unsplash.com/photo-1542496658-e33a6d0d50f6?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 1240,
                    likesCount = 512,
                    isFeatured = true,
                    couponCode = "BASSFIT"
                ),
                ProductEntity(
                    title = "Fone de Ouvido Bluetooth Premium Noise Cancelling",
                    category = "Eletrônicos",
                    description = "Áudio de alta fidelidade com graves profundos e cancelamento de ruído ativo. Case de carregamento inteligente e bateria com duração de até 24 horas.",
                    price = 49.90,
                    originalPrice = 129.00,
                    discountPercentage = 61,
                    shopeeUrl = "https://shopee.com.br/Fone-Bluetooth-Sem-Fio-i_123457",
                    imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 540,
                    likesCount = 235,
                    isFeatured = true,
                    couponCode = "SHOPSOM"
                ),
                ProductEntity(
                    title = "Umidificador de Ar Ultrassônico Flame Glow",
                    category = "Casa & Cozinha",
                    description = "Lindo difusor de aromas ultrassônico com efeito de chamas LED realísticas. Melhora a humidade do ar, decora o ambiente e serve de luminária de cabeceira.",
                    price = 34.90,
                    originalPrice = 79.90,
                    discountPercentage = 56,
                    shopeeUrl = "https://shopee.com.br/Umidificador-Ultrass%C3%B4nico-LED-i_123458",
                    imageUrl = "https://images.unsplash.com/photo-1519183071298-a2962feb14f4?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 812,
                    likesCount = 420,
                    isFeatured = true,
                    couponCode = "FLAME10"
                ),
                ProductEntity(
                    title = "Organizador Acrílico de Cosméticos Giratório 360",
                    category = "Beleza",
                    description = "Mantenha seus cremes, batons e maquiagens organizados de forma prática e estilosa. Base giratória de alta resistência com múltiplos compartimentos ajustáveis.",
                    price = 39.90,
                    originalPrice = 89.90,
                    discountPercentage = 55,
                    shopeeUrl = "https://shopee.com.br/Organizador-Cosmeticos-360-i_123459",
                    imageUrl = "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 210,
                    likesCount = 156,
                    isFeatured = false,
                    couponCode = "BASSBELEZA"
                ),
                ProductEntity(
                    title = "Mini Processador de Alimentos USB Inteligente",
                    category = "Casa & Cozinha",
                    description = "Triture temperos, alhos, cebolas e legumes instantaneamente com apenas um clique. Recarregável via cabo USB, lâminas em aço inox ultra afiadas.",
                    price = 24.99,
                    originalPrice = 49.90,
                    discountPercentage = 50,
                    shopeeUrl = "https://shopee.com.br/Mini-Processador-Alimentos-El%C3%A9trico-i_123460",
                    imageUrl = "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 198,
                    likesCount = 142,
                    isFeatured = false,
                    couponCode = "EASYCOOK"
                ),
                ProductEntity(
                    title = "Garrafa de Água Motivacional Pastel 2L",
                    category = "Outros",
                    description = "Acompanhe sua hidratação com marcadores de horários inspiradores. Inclui bico de silicone antivazamento, alça reforçada de transporte e kit de adesivos 3D fofos.",
                    price = 15.90,
                    originalPrice = 39.90,
                    discountPercentage = 60,
                    shopeeUrl = "https://shopee.com.br/Garrafa-Motivacional-2L-i_123461",
                    imageUrl = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?q=80&w=400&fit=crop",
                    isLiked = false,
                    clicksCount = 779,
                    likesCount = 341,
                    isFeatured = false,
                    couponCode = "HYDRO"
                )
            )
            productDao.insertProducts(initialProducts)
        }
    }
}

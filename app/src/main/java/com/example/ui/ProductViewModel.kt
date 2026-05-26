package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.AppDatabase
import com.example.data.ProductEntity
import com.example.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ProductRepository(database.productDao())

    // Blogger Comments Simulator by Product ID
    private val _comments = MutableStateFlow<Map<Int, List<Pair<String, String>>>>(emptyMap())
    val comments: StateFlow<Map<Int, List<Pair<String, String>>>> = _comments.asStateFlow()

    init {
        // Prepopulate with elegant, localized comments of varying authors
        val mockComments = mapOf(
            1 to listOf(
                "Amanda Oliveira" to "Meninas, comprei esse relógio e chegou em menos de uma semana! O monitor de passos é super preciso! 😍💃",
                "Carlos Eduardo" to "Uso pra fazer meus treinos diários e a bateria dura uns 5 dias direto. Recomendo muito pelo preço!",
                "Beatriz Santos" to "Consegui usar o cupom BASSFIT e ganhei mais desconto ainda! Já quero comprar outro pro meu namorado."
            ),
            2 to listOf(
                "Luana Faria" to "O cancelamento de ruído cancela o mundo real mesmo, perfeito pra estudar! 🎧✨",
                "Marcos Vinicius" to "Grave muito forte e não machuca a orelha. Pelo preço achei que não fosse tão bom, mas me surpreendeu dms.",
                "Juliana Paes" to "Comprem sem medo meninas! É o mesmo fone que as blogueiras gringas no TikTok usam kkkkk amei"
            ),
            3 to listOf(
                "Mariana Costa" to "O efeito de luz imitando fogo é maravilhoso à noite, deixa o quarto super aconchegante 🔥❤️",
                "Felipe Alencar" to "Coloquei essência de capim limão e a casa toda tá cheirosa. Virou meu item de decoração favorito.",
                "Renata Souza" to "Cupom FLAME10 funcionando certinho, cupom milagroso rsrs"
            ),
            4 to listOf(
                "Tati Pinheiro" to "Coube TODAS as minhas maquiagens e sobrou espaço. O fato de girar facilita demais na correria pra se arrumar.",
                "Sofia Martins" to "Gira perfeitamente bem, material bem resistente e grosso. Nota 10!"
            )
        )
        _comments.value = mockComments
    }

    fun addComment(productId: Int, author: String, text: String) {
        if (text.isBlank()) return
        val current = _comments.value.toMutableMap()
        val list = current[productId]?.toMutableList() ?: mutableListOf()
        list.add(author.ifBlank { "Anônimo" } to text)
        current[productId] = list
        _comments.value = current
    }

    // SharedPreferences for Affiliate settings
    private val sharedPrefs = application.getSharedPreferences("achadinhos_prefs", Context.MODE_PRIVATE)

    private val _affiliateId = MutableStateFlow(sharedPrefs.getString("affiliate_id", "18373230066") ?: "18373230066")
    val affiliateId: StateFlow<String> = _affiliateId.asStateFlow()

    private val _siteUrl = MutableStateFlow(
        sharedPrefs.getString("site_url", "https://ais-pre-adu5zcz2fdfkvc2kry65eq-393917310392.us-west2.run.app") 
        ?: "https://ais-pre-adu5zcz2fdfkvc2kry65eq-393917310392.us-west2.run.app"
    )
    val siteUrl: StateFlow<String> = _siteUrl.asStateFlow()

    fun updateAffiliateId(id: String) {
        _affiliateId.value = id
        sharedPrefs.edit().putString("affiliate_id", id).apply()
    }

    fun updateSiteUrl(url: String) {
        _siteUrl.value = url
        sharedPrefs.edit().putString("site_url", url).apply()
    }

    fun formatShopeeUrlWithAffiliateId(url: String, affiliateId: String): String {
        if (affiliateId.isBlank()) return url
        if (!url.contains("shopee.com") && !url.contains("shope.ee")) return url
        
        val separator = if (url.contains("?")) "&" else "?"
        return if (url.contains("sub_id=")) {
            url.replace(Regex("sub_id=[^&]*"), "sub_id=$affiliateId")
        } else {
            "$url${separator}sub_id=$affiliateId"
        }
    }

    // All registered products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection states
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered Products State
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchCategory = when (category) {
                "Todos" -> true
                "Favoritos" -> product.isLiked
                "Destaques" -> product.isFeatured
                else -> product.category.equals(category, ignoreCase = true)
            }
            val matchQuery = if (query.isEmpty()) {
                true
            } else {
                product.title.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true) ||
                        product.category.contains(query, ignoreCase = true)
            }
            matchCategory && matchQuery
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI states
    private val _isGeneratingCopy = MutableStateFlow<Int?>(null) // holds productId currently generating copy for
    val isGeneratingCopy: StateFlow<Int?> = _isGeneratingCopy.asStateFlow()

    private val _generatedCopys = MutableStateFlow<Map<Int, String>>(emptyMap())
    val generatedCopys: StateFlow<Map<Int, String>> = _generatedCopys.asStateFlow()

    // UI actions
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLike(productId: Int) {
        viewModelScope.launch {
            repository.toggleLike(productId)
        }
    }

    fun incrementClick(productId: Int) {
        viewModelScope.launch {
            repository.incrementClick(productId)
        }
    }

    fun addProduct(
        title: String,
        category: String,
        description: String,
        price: Double,
        originalPrice: Double,
        shopeeUrl: String,
        imageUrl: String,
        couponCode: String = "",
        isFeatured: Boolean = false
    ) {
        viewModelScope.launch {
            val discount = if (originalPrice > price) {
                (((originalPrice - price) / originalPrice) * 100).toInt()
            } else {
                0
            }
            val cleanUrl = if (shopeeUrl.isBlank()) {
                "https://shopee.com.br"
            } else if (!shopeeUrl.startsWith("http://") && !shopeeUrl.startsWith("https://")) {
                "https://$shopeeUrl"
            } else {
                shopeeUrl
            }
            val newProduct = ProductEntity(
                title = title,
                category = category,
                description = description,
                price = price,
                originalPrice = originalPrice,
                discountPercentage = discount,
                shopeeUrl = cleanUrl,
                imageUrl = if (imageUrl.isBlank()) {
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?q=80&w=400&fit=crop"
                } else {
                    imageUrl
                },
                couponCode = couponCode.uppercase(),
                isFeatured = isFeatured,
                likesCount = (10..150).random(),
                clicksCount = (15..300).random()
            )
            repository.insert(newProduct)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.delete(product)
        }
    }

    fun generateSalesCopyForProduct(product: ProductEntity) {
        viewModelScope.launch {
            _isGeneratingCopy.value = product.id
            val affiliateLink = formatShopeeUrlWithAffiliateId(product.shopeeUrl, _affiliateId.value)
            val copy = GeminiApiClient.generateSalesCopy(
                title = product.title,
                category = product.category,
                price = product.price,
                coupon = product.couponCode,
                link = affiliateLink
            )
            _generatedCopys.update { current ->
                current + (product.id to copy)
            }
            _isGeneratingCopy.value = null
        }
    }

    fun clearCopy(productId: Int) {
        _generatedCopys.update { current ->
            current - productId
        }
    }
}

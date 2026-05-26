package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ProductEntity
import com.example.ui.ProductViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    AchadinhosDashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchadinhosDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProductsList by viewModel.allProducts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // Dashboard Statistics (from all available items)
    val totalLikes = allProductsList.sumOf { it.likesCount + if (it.isLiked) 1 else 0 }
    val totalClicks = allProductsList.sumOf { it.clicksCount }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            BrandHeroHeader(
                totalLikes = totalLikes,
                totalClicks = totalClicks,
                viewModel = viewModel
            )

            // Search Bar Component
            ProductSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Category Bar Selector
            CategorySelectorBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Products Content
            if (products.isEmpty()) {
                EmptyStateLayout(
                    isFilterActive = searchQuery.isNotEmpty() || selectedCategory != "Todos",
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.selectCategory("Todos")
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("products_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val currentAffiliateId by viewModel.affiliateId.collectAsStateWithLifecycle()
                        ProductCard(
                            product = product,
                            viewModel = viewModel,
                            onProductClick = {
                                viewModel.incrementClick(product.id)
                                try {
                                    val finalUrl = viewModel.formatShopeeUrlWithAffiliateId(product.shopeeUrl, currentAffiliateId)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Link inválido ou indisponível", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add Product Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_achadinho_fab")
                .border(2.dp, ShopeeLightOrange, RoundedCornerShape(16.dp)),
            containerColor = ShopeeOrange,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar Achadinho",
                modifier = Modifier.size(28.dp)
            )
        }

        // Input Modal Dialog
        if (showAddDialog) {
            AddAchadinhoDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, category, desc, price, origPrice, link, img, coupon, featured ->
                    viewModel.addProduct(title, category, desc, price, origPrice, link, img, coupon, featured)
                    showAddDialog = false
                    Toast.makeText(context, "Achadinho adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun BrandHeroHeader(
    totalLikes: Int,
    totalClicks: Int,
    viewModel: ProductViewModel
) {
    val context = LocalContext.current
    val affiliateId by viewModel.affiliateId.collectAsStateWithLifecycle()
    val siteUrl by viewModel.siteUrl.collectAsStateWithLifecycle()
    
    var showEditPrefsDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(DarkSurface, DarkSurfaceElevated)
                )
            )
            .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ShopeeOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BASS COMPRE MAIS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "ACHADINHOS DA SHOPEE",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ShopeeOrange,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEditPrefsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar Link & ID",
                            tint = ShopeeOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShopeeDarkOrange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ShopeeOrange.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "PARCEIRO",
                            color = ShopeeOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    imageVector = Icons.Default.Favorite,
                    color = HeartRed,
                    label = "$totalLikes curtidas"
                )
                StatusBadge(
                    imageVector = Icons.Default.ThumbUp,
                    color = AccentAmber,
                    label = "$totalClicks cliques oficiais"
                )
                // ID Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShopeeOrange.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ShopeeOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { showEditPrefsDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ShopeeOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ID: $affiliateId",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful mini-banner to share Web Site
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SoftGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SITE DE DIVULGAÇÃO ATIVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seu parceiro Shopee em formato de site!",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open Site button
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "URL inválida", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Abrir Site",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Copy Site URL Button
                        Button(
                            onClick = {
                                val textToCopy = "Olá! Conheça a minha vitrine oficial de achadinhos da Shopee com ofertas incríveis selecionadas por mim! Acesse agora: $siteUrl"
                                copyToClipboard(context, textToCopy, "Link da Vitrine copiado para divulgação! 🚀")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Divulgar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Preferences / ID Configuration Dialog
    if (showEditPrefsDialog) {
        var tempId by remember { mutableStateOf(affiliateId) }
        var tempUrl by remember { mutableStateOf(siteUrl) }

        Dialog(
            onDismissRequest = { showEditPrefsDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ShopeeOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configuração do Site & Afiliado",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = ShopeeOrange
                    )

                    Text(
                        text = "Insira o seu ID da Shopee para atualizar automaticamente os links e o endereço do site que você deseja divulgar.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = tempId,
                        onValueChange = { tempId = it },
                        label = { Text("Seu ID de Afiliado Shopee") },
                        placeholder = { Text("Ex: 18373230066") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Link da Vitrine (Seu Site)") },
                        placeholder = { Text("Seu link de divulgação do site") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditPrefsDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                viewModel.updateAffiliateId(tempId)
                                viewModel.updateSiteUrl(tempUrl)
                                showEditPrefsDialog = false
                                Toast.makeText(context, "Configurações salvas e aplicadas! 🚀", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = ShopeeOrange),
                            enabled = tempId.isNotBlank() && tempUrl.isNotBlank()
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProductSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("search_bar_input")
            .border(1.dp, DarkCardBorder, RoundedCornerShape(28.dp)),
        placeholder = {
            Text(
                text = "Buscar achadinhos...",
                color = TextMuted,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar busca",
                        tint = TextSecondary
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = ShopeeOrange,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp)
    )
}

@Composable
fun CategorySelectorBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Todos", "Favoritos", "Eletrônicos", "Casa & Cozinha", "Beleza", "Outros")

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            val isFavoritoTab = category == "Favoritos"

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> if (isFavoritoTab) HeartRed else ShopeeOrange
                        else -> DarkSurfaceElevated
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        isSelected -> Color.Transparent
                        isFavoritoTab -> HeartRed.copy(alpha = 0.4f)
                        else -> DarkCardBorder
                    }
                ),
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
                    .testTag("category_tab_$category")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isFavoritoTab) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else HeartRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    viewModel: ProductViewModel,
    onProductClick: () -> Unit
) {
    val context = LocalContext.current
    var isDescExpanded by remember { mutableStateOf(false) }

    val isGeneratingCopyMap by viewModel.isGeneratingCopy.collectAsStateWithLifecycle()
    val generatedCopysMap by viewModel.generatedCopys.collectAsStateWithLifecycle()

    val isAIActive = isGeneratingCopyMap == product.id
    val readyAICopy = generatedCopysMap[product.id]

    // Local animated scale for Liking heart
    var iconClickedState by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (iconClickedState) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { iconClickedState = false }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (product.isFeatured) ShopeeOrange.copy(alpha = 0.4f) else DarkCardBorder)
    ) {
        Column {
            // Product Image & Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // Handle image load error fallback safely 
                    error = painterResource(android.R.drawable.stat_notify_error),
                    fallback = painterResource(android.R.drawable.ic_menu_gallery)
                )

                // Featured/Glow backdrop overlay on featured items
                if (product.isFeatured) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                }

                // Discount Badge overlay
                if (product.discountPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(ShopeeOrange, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${product.discountPercentage}% OFF",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                // Category tag overlay top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Product Details and Copy
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title and Featured Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (product.isFeatured) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Popular",
                            tint = AccentAmber,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Description with toggle expanded
                Text(
                    text = product.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isDescExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { isDescExpanded = !isDescExpanded }
                        .animateContentSize()
                )

                Text(
                    text = if (isDescExpanded) "Ler menos" else "Ler completo...",
                    color = ShopeeOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clickable { isDescExpanded = !isDescExpanded }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pricing Row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (product.originalPrice > product.price) {
                            Text(
                                text = "De R$ ${String.format("%.2f", product.originalPrice)}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Por R$",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp, end = 4.dp)
                            )
                            Text(
                                text = String.format("%.2f", product.price),
                                color = ShopeeOrange,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Coupon display
                    if (product.couponCode.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SoftGreen.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, SoftGreen.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        copyToClipboard(context, product.couponCode, "Cupom copiado: ${product.couponCode}")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Copiar Cupom",
                                    tint = SoftGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "CUPOM ATIVO",
                                        fontSize = 8.sp,
                                        color = SoftGreen,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = product.couponCode,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = DarkCardBorder, thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Action controls row (Like, AI generator, Copy details, and Redirect button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Likes and Clicks summary
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Like heart button
                        IconButton(
                            onClick = {
                                iconClickedState = true
                                viewModel.toggleLike(product.id)
                            },
                            modifier = Modifier.testTag("like_button_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (product.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Curtir",
                                tint = if (product.isLiked) HeartRed else TextSecondary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .size(24.dp * heartScale) // spring anim scaling
                            )
                        }

                        // Copy Link / Offer Button
                        val currentAffiliateId by viewModel.affiliateId.collectAsStateWithLifecycle()
                        IconButton(
                            onClick = {
                                val affiliateLink = viewModel.formatShopeeUrlWithAffiliateId(product.shopeeUrl, currentAffiliateId)
                                val messageStr = "🔥 CORRE! ${product.title} por apenas R$ ${String.format("%.2f", product.price)} na Shopee! Garanta o seu aqui: $affiliateLink"
                                copyToClipboard(context, messageStr, "Link do Achadinho copiado para compartilhar!")
                            },
                            modifier = Modifier.testTag("share_button_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = TextSecondary
                            )
                        }

                        // AI Marketing Assistant Action Button
                        IconButton(
                            onClick = {
                                if (readyAICopy == null) {
                                    viewModel.generateSalesCopyForProduct(product)
                                } else {
                                    viewModel.clearCopy(product.id)
                                }
                            },
                            modifier = Modifier.testTag("ai_helper_button_${product.id}")
                        ) {
                            if (isAIActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = ShopeeOrange
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Gerador de Copy IA",
                                    tint = if (readyAICopy != null) ShopeeOrange else TextSecondary
                                )
                            }
                        }

                        // Delete button (admin/user customization option)
                        IconButton(
                            onClick = {
                                viewModel.deleteProduct(product)
                                Toast.makeText(context, "Achadinho removido", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Achadinho",
                                tint = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Official SHOPEE click to buy item button
                    Button(
                        onClick = onProductClick,
                        modifier = Modifier
                            .testTag("buy_button_${product.id}")
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ShopeeOrange),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ir pro Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // AI Copy Box slide expansion if generating/ready
                AnimatedVisibility(
                    visible = isAIActive || readyAICopy != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, ShopeeOrange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ShopeeOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BASS COPYWRITER IA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = ShopeeOrange,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                if (readyAICopy != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "COPIAR COPY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier
                                                .clickable {
                                                    copyToClipboard(
                                                        context,
                                                        readyAICopy,
                                                        "Copy de vendas copiada! Só postar e faturar. 🚀"
                                                    )
                                                }
                                                .background(
                                                    Color.White.copy(alpha = 0.05f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )

                                        Text(
                                            text = "ESCUTAR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier
                                                .clickable {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Gerador de voz/TTS ativo em modelo PRO. Toque copiar para compartilhar!",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                                .background(
                                                    Color.White.copy(alpha = 0.05f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isAIActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 1.5.dp,
                                        color = ShopeeOrange
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Criando redação persuasiva oficial...",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (readyAICopy != null) {
                                Text(
                                    text = readyAICopy,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.testTag("ai_copy_text")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout(
    isFilterActive: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ShopeeOrange.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFilterActive) Icons.Default.Search else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = ShopeeOrange,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isFilterActive) "Nenhum resultado encontrado" else "Nenhum achadinho cadastrado",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFilterActive) {
                    "Tente redefinir suas palavras ou mudar de categoria no seletor acima."
                } else {
                    "Toque no botão '+' abaixo para cadastrar suas próprias ofertas da Shopee!"
                },
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (isFilterActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = ShopeeOrange)
                ) {
                    Text("Limpar Filtros / Ver Todos")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAchadinhoDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        category: String,
        description: String,
        price: Double,
        originalPrice: Double,
        link: String,
        imageUrl: String,
        coupon: String,
        featured: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Eletrônicos") }
    var priceStr by remember { mutableStateOf("") }
    var originalPriceStr by remember { mutableStateOf("") }
    var shopeeUrl by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf("") }
    var isFeatured by remember { mutableStateOf(false) }

    val categories = listOf("Eletrônicos", "Casa & Cozinha", "Beleza", "Outros")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_achadinho_modal")
                .border(1.dp, ShopeeOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Novo Achadinho BASS",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = ShopeeOrange
                    )
                    Text(
                        text = "Cadastre a sua oferta com link de afiliado",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = DarkCardBorder, thickness = 1.dp)
                }

                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome do Produto") },
                        modifier = Modifier.fillMaxWidth().testTag("add_title_input"),
                        colors = outlinedTextFieldColors(),
                        singleLine = true
                    )
                }

                // Category Buttons Row
                item {
                    Text("Categoria", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = cat == category
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) ShopeeOrange else DarkSurfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { category = cat },
                                border = BorderStroke(1.dp, if (isSel) Color.Transparent else DarkCardBorder)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .align(Alignment.CenterHorizontally),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição / Copys curtas") },
                        modifier = Modifier.fillMaxWidth().testTag("add_desc_input"),
                        colors = outlinedTextFieldColors(),
                        maxLines = 3
                    )
                }

                // Prices
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Preço Promo (Ex: 49.90)") },
                            modifier = Modifier.weight(1f).testTag("add_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = outlinedTextFieldColors(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = originalPriceStr,
                            onValueChange = { originalPriceStr = it },
                            label = { Text("Preço Cheio (Ex: 129.00)") },
                            modifier = Modifier.weight(1f).testTag("add_orig_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = outlinedTextFieldColors(),
                            singleLine = true
                        )
                    }
                }

                // Shopee Affiliate URL
                item {
                    OutlinedTextField(
                        value = shopeeUrl,
                        onValueChange = { shopeeUrl = it },
                        label = { Text("Link de Vendas (Shopee)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_link_input"),
                        colors = outlinedTextFieldColors(),
                        singleLine = true
                    )
                }

                // Image URL
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Link da Imagem (Opcional)") },
                        placeholder = { Text("Deixe em branco para usar padrão") },
                        modifier = Modifier.fillMaxWidth().testTag("add_image_input"),
                        colors = outlinedTextFieldColors(),
                        singleLine = true
                    )
                }

                // Coupon Code / Featured Product Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponCode,
                            onValueChange = { couponCode = it },
                            label = { Text("Cupom Ativo (Ex: COMPRA5)") },
                            modifier = Modifier.weight(1f).testTag("add_coupon_input"),
                            colors = outlinedTextFieldColors(),
                            singleLine = true
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(0.8f)
                                .clickable { isFeatured = !isFeatured }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = isFeatured,
                                onCheckedChange = { isFeatured = it },
                                colors = CheckboxDefaults.colors(checkedColor = ShopeeOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Destaque 🔥",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons (Dismiss or Save)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                                val finalOriginalPrice = originalPriceStr.toDoubleOrNull() ?: finalPrice
                                if (title.isBlank() || shopeeUrl.isBlank() || finalPrice <= 0) {
                                    // Just fail silently or keep fields red. Let's register standard inputs
                                } else {
                                    onSave(
                                        title,
                                        category,
                                        description.ifBlank { "Adquira este incrível achado no link da Shopee!" },
                                        finalPrice,
                                        finalOriginalPrice,
                                        shopeeUrl,
                                        imageUrl,
                                        couponCode,
                                        isFeatured
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("save_product_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ShopeeOrange),
                            enabled = title.isNotBlank() && shopeeUrl.isNotBlank() && (priceStr.toDoubleOrNull() != null)
                        ) {
                            Text("Salvar Oferta")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ShopeeOrange,
    unfocusedBorderColor = DarkCardBorder,
    focusedLabelColor = ShopeeOrange,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = ShopeeOrange
)

fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("BASS Oferta", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}

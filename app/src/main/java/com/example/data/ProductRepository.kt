package com.example.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun getProductById(id: Int): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun insert(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun delete(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun update(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun toggleLike(productId: Int) {
        productDao.toggleLikeProduct(productId)
    }

    suspend fun incrementClick(productId: Int) {
        productDao.incrementClickCount(productId)
    }

    suspend fun insertProductsIfEmpty(products: List<ProductEntity>) {
        productDao.insertProducts(products)
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val description: String,
    val price: Double,
    val originalPrice: Double,
    val discountPercentage: Int,
    val shopeeUrl: String,
    val imageUrl: String,
    val isLiked: Boolean = false,
    val clicksCount: Int = 0,
    val likesCount: Int = 0,
    val isFeatured: Boolean = false,
    val couponCode: String = ""
)

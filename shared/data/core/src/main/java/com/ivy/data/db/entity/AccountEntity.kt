package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Suppress("DataClassDefaultValues")
@Keep
@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @SerialName("name")
    val name: String,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("color")
    val color: Int,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("orderNum")
    val orderNum: Double = 0.0,
    @SerialName("includeInBalance")
    val includeInBalance: Boolean = true,
    @SerialName("creditLimit")
    val creditLimit: Double? = null,
    @SerialName("creditCardGroupId")
    @Serializable(with = KSerializerUUID::class)
    val creditCardGroupId: UUID? = null,
    @SerialName("creditLimitShared")
    @ColumnInfo(defaultValue = "0")
    val creditLimitShared: Boolean = false,
    @SerialName("creditExchangeRate")
    val creditExchangeRate: Double? = null,
    /** Day of month (1..31) the card statement closes; null when not set. */
    @SerialName("creditStatementDay")
    val creditStatementDay: Int? = null,
    /** Day of month (1..31) the statement payment is due; null when not set. */
    @SerialName("creditDueDay")
    val creditDueDay: Int? = null,

    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isSynced")
    val isSynced: Boolean = false,
    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isDeleted")
    val isDeleted: Boolean = false,

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID()
)

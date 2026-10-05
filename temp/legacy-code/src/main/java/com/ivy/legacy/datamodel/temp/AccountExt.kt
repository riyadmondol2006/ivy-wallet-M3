package com.ivy.legacy.datamodel.temp

import com.ivy.data.db.entity.AccountEntity
import com.ivy.legacy.datamodel.Account

fun AccountEntity.toLegacyDomain(): Account = Account(
    name = name,
    currency = currency,
    color = color,
    icon = icon,
    orderNum = orderNum,
    includeInBalance = includeInBalance,
    creditLimit = creditLimit,
    creditCardGroupId = creditCardGroupId,
    creditLimitShared = creditLimitShared,
    creditExchangeRate = creditExchangeRate,
    creditStatementDay = creditStatementDay,
    creditDueDay = creditDueDay,
    isSynced = isSynced,
    isDeleted = isDeleted,
    id = id
)

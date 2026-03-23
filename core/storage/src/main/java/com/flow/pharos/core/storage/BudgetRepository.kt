package com.flow.pharos.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flow.pharos.core.model.BudgetPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "budget_settings")

class BudgetRepository(private val context: Context) {

    private object Keys {
        val DAILY_LIMIT = floatPreferencesKey("daily_limit_usd")
        val USE_PAID_UNTIL_LIMIT = booleanPreferencesKey("use_paid_until_daily_limit")
        val CHEAP_FIRST = booleanPreferencesKey("cheap_first")
    }

    val budgetPolicy: Flow<BudgetPolicy> = context.dataStore.data.map { prefs ->
        BudgetPolicy(
            dailyLimitUsd = prefs[Keys.DAILY_LIMIT] ?: 5f,
            usePaidUntilDailyLimit = prefs[Keys.USE_PAID_UNTIL_LIMIT] ?: true,
            cheapFirst = prefs[Keys.CHEAP_FIRST] ?: true,
        )
    }

    suspend fun update(policy: BudgetPolicy) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_LIMIT] = policy.dailyLimitUsd
            prefs[Keys.USE_PAID_UNTIL_LIMIT] = policy.usePaidUntilDailyLimit
            prefs[Keys.CHEAP_FIRST] = policy.cheapFirst
        }
    }
}

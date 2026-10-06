package com.example.rent_due.data.repository

import com.example.rent_due.data.local.HouseJsonAdapter
import com.example.rent_due.data.local.RentDao
import com.example.rent_due.data.model.House
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RentRepository(private val rentDao: RentDao) {

    val housesFlow: Flow<List<House>> = rentDao.getAllHousesFlow().map { entities ->
        entities.map { HouseJsonAdapter.toHouse(it) }
    }

    suspend fun getHouse(id: String): House? {
        val entity = rentDao.getHouseById(id) ?: return null
        return HouseJsonAdapter.toHouse(entity)
    }

    suspend fun saveHouse(house: House) {
        val entity = HouseJsonAdapter.toEntity(house)
        rentDao.insertHouse(entity)
    }

    suspend fun deleteHouse(house: House) {
        val entity = HouseJsonAdapter.toEntity(house)
        rentDao.deleteHouse(entity)
    }

    suspend fun deleteHouseById(id: String) {
        rentDao.deleteHouseById(id)
    }

    suspend fun clearAll() {
        rentDao.deleteAll()
    }
}

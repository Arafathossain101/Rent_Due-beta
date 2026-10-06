package com.example.rent_due.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import com.example.rent_due.data.model.Bill
import com.example.rent_due.data.model.Floor
import com.example.rent_due.data.model.House
import com.example.rent_due.data.model.RentLog
import com.example.rent_due.data.model.Room

@Entity(tableName = "houses")
data class HouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imagePath: String?,
    val numberOfStories: Int,
    val floorsJson: String
)

@Dao
interface RentDao {
    @Query("SELECT * FROM houses ORDER BY id ASC")
    fun getAllHousesFlow(): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE id = :id LIMIT 1")
    suspend fun getHouseById(id: String): HouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouse(house: HouseEntity)

    @Update
    suspend fun updateHouse(house: HouseEntity)

    @Delete
    suspend fun deleteHouse(house: HouseEntity)

    @Query("DELETE FROM houses WHERE id = :id")
    suspend fun deleteHouseById(id: String)

    @Query("DELETE FROM houses")
    suspend fun deleteAll()
}

@Database(entities = [HouseEntity::class], version = 1, exportSchema = false)
abstract class RentDatabase : RoomDatabase() {
    abstract fun rentDao(): RentDao

    companion object {
        @Volatile
        private var INSTANCE: RentDatabase? = null

        fun getDatabase(context: Context): RentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RentDatabase::class.java,
                    "rent_record.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// JSON Serializers for structured persistence
object HouseJsonAdapter {
    fun serializeFloors(floors: List<Floor>): String {
        val root = JSONArray()
        for (floor in floors) {
            val floorObj = JSONObject()
            floorObj.put("floorNumber", floor.floorNumber)
            val roomsArray = JSONArray()
            for (room in floor.rooms) {
                val roomObj = JSONObject()
                roomObj.put("id", room.id)
                roomObj.put("name", room.name)
                roomObj.put("imagePath", room.imagePath ?: JSONObject.NULL)
                roomObj.put("renterName", room.renterName)
                roomObj.put("renterIdImagePath", room.renterIdImagePath ?: JSONObject.NULL)
                roomObj.put("baseRentAmount", room.baseRentAmount)
                roomObj.put("previousDueAmount", room.previousDueAmount)

                val billsArray = JSONArray()
                for (bill in room.bills) {
                    val billObj = JSONObject()
                    billObj.put("id", bill.id)
                    billObj.put("name", bill.name)
                    billObj.put("amount", bill.amount)
                    billObj.put("paidAmount", bill.paidAmount)
                    billObj.put("isRent", bill.isRent)
                    billObj.put("targetMonthKey", bill.targetMonthKey)
                    billsArray.put(billObj)
                }
                roomObj.put("bills", billsArray)

                val logsArray = JSONArray()
                for (log in room.logs) {
                    val logObj = JSONObject()
                    logObj.put("id", log.id)
                    logObj.put("timestamp", log.timestamp)
                    logObj.put("description", log.description)
                    logsArray.put(logObj)
                }
                roomObj.put("logs", logsArray)

                roomsArray.put(roomObj)
            }
            floorObj.put("rooms", roomsArray)
            root.put(floorObj)
        }
        return root.toString()
    }

    fun deserializeFloors(json: String): List<Floor> {
        if (json.isBlank()) return emptyList()
        val result = mutableListOf<Floor>()
        try {
            val root = JSONArray(json)
            for (i in 0 until root.length()) {
                val floorObj = root.getJSONObject(i)
                val floorNumber = floorObj.optInt("floorNumber", i + 1)
                val roomsArray = floorObj.optJSONArray("rooms") ?: JSONArray()
                val rooms = mutableListOf<Room>()

                for (j in 0 until roomsArray.length()) {
                    val roomObj = roomsArray.getJSONObject(j)
                    val id = roomObj.optString("id", System.currentTimeMillis().toString() + "_$j")
                    val name = roomObj.optString("name", "Room")
                    val imagePath = if (roomObj.isNull("imagePath")) null else roomObj.optString("imagePath", null)
                    val renterName = roomObj.optString("renterName", "")
                    val renterIdImagePath = if (roomObj.isNull("renterIdImagePath")) null else roomObj.optString("renterIdImagePath", null)
                    val baseRentAmount = roomObj.optDouble("baseRentAmount", 0.0)
                    val previousDueAmount = roomObj.optDouble("previousDueAmount", 0.0)

                    val billsArray = roomObj.optJSONArray("bills") ?: JSONArray()
                    val bills = mutableListOf<Bill>()
                    for (k in 0 until billsArray.length()) {
                        val billObj = billsArray.getJSONObject(k)
                        bills.add(
                            Bill(
                                id = billObj.optString("id", "$k"),
                                name = billObj.optString("name", "Bill"),
                                amount = billObj.optDouble("amount", 0.0),
                                paidAmount = billObj.optDouble("paidAmount", 0.0),
                                isRent = billObj.optBoolean("isRent", false),
                                targetMonthKey = billObj.optString("targetMonthKey", "")
                            )
                        )
                    }

                    val logsArray = roomObj.optJSONArray("logs") ?: JSONArray()
                    val logs = mutableListOf<RentLog>()
                    for (k in 0 until logsArray.length()) {
                        val logObj = logsArray.getJSONObject(k)
                        logs.add(
                            RentLog(
                                id = logObj.optString("id", "$k"),
                                timestamp = logObj.optLong("timestamp", System.currentTimeMillis()),
                                description = logObj.optString("description", "")
                            )
                        )
                    }

                    rooms.add(
                        Room(
                            id = id,
                            name = name,
                            imagePath = imagePath,
                            renterName = renterName,
                            renterIdImagePath = renterIdImagePath,
                            baseRentAmount = baseRentAmount,
                            previousDueAmount = previousDueAmount,
                            bills = bills,
                            logs = logs
                        )
                    )
                }
                result.add(Floor(floorNumber = floorNumber, rooms = rooms))
            }
        } catch (_: Exception) { }
        return result
    }

    fun toHouse(entity: HouseEntity): House {
        return House(
            id = entity.id,
            name = entity.name,
            imagePath = entity.imagePath,
            numberOfStories = entity.numberOfStories,
            floors = deserializeFloors(entity.floorsJson)
        )
    }

    fun toEntity(house: House): HouseEntity {
        return HouseEntity(
            id = house.id,
            name = house.name,
            imagePath = house.imagePath,
            numberOfStories = house.numberOfStories,
            floorsJson = serializeFloors(house.floors)
        )
    }
}

package be.ap.edu.mapsaver

import java.sql.Timestamp
import java.time.LocalDateTime

data class Car(
    val carId:String?=null,
    val location:String?=null,
    val lat: Double?=null,
    val lon: Double?=null,
    val time: Int?=null,
    val person: Person? =null
)

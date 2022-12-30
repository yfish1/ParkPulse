package be.ap.edu.mapsaver

import android.util.Log
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint

 class MapEventsReceiverImpl:MapEventsReceiver {
     override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
         Log.d("singleTapConfirmedHelper", "${p?.latitude} - ${p?.longitude}")
         return true
     }

     override fun longPressHelper(p: GeoPoint?): Boolean {
        Log.d("longPressHelper", "${p?.latitude} - ${p?.longitude}")
        return false
    }

}
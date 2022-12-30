package be.ap.edu.mapsaver

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : Activity() {
    private lateinit var dbRef: DatabaseReference
    private lateinit var mMapView: MapView
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    private var items = ArrayList<OverlayItem>()

    private val urlNominatim = "https://nominatim.openstreetmap.org/"
    private var notificationManager: NotificationManager? = null
    private var mChannel: NotificationChannel? = null

    //UI Elements
    private var addParking: FloatingActionButton? = null
    private var logoutfButton : FloatingActionButton? = null
    private var searchField: EditText? = null
    private var searchButton: Button? = null
    private var timeField: EditText? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        // Map Theme
        setContentView(R.layout.activity_main)
        mMapView = findViewById(R.id.mapview)
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS);

        // Live location
        val myLocationoverlay = MyLocationNewOverlay(mMapView)
        myLocationoverlay.enableFollowLocation()
        myLocationoverlay.enableMyLocation()
        mMapView.overlays.add(myLocationoverlay)


        // Easier navigation through LongPress
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Toast.makeText(
                    baseContext,
                    p.latitude.toString() + " - " + p.longitude,
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                Toast.makeText(
                    baseContext,
                    p.latitude.toString() + " - " + p.longitude,
                    Toast.LENGTH_LONG
                ).show()
                mMapView?.controller?.setZoom(17.0)
                setCenter(GeoPoint(p.latitude, p.longitude), "Live location")
                return false
            }
        }
        mMapView.getOverlays().add(MapEventsOverlay(mReceive))


        // Add spot
        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton?.setOnClickListener {
            val url = URL(urlNominatim + "search?q=" + URLEncoder.encode(searchField?.text.toString(), "UTF-8") + "&format=json")
            it.hideKeyboard()
            getAddressOrLocation(url)
        }

         // Permissions
        if (hasPermissions()) {
            initMap()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        // Notifications
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mChannel = NotificationChannel("my_channel_01","My Channel", NotificationManager.IMPORTANCE_HIGH)
        mChannel?.setShowBadge(true)
        //mChannel?.enableLights(true)
        //mChannel?.enableVibration(true)
        //mChannel?.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(mChannel!!)

        // Logout
        logoutfButton = findViewById(R.id.logout_fButton)
        logoutfButton?.setOnClickListener{
             val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        // Testing new feature where you could add a spot through an Alertdialog  //devfase
        addParking = findViewById(R.id.addParking)
        addParking?.setOnClickListener{
           /* val txtUrl = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Add parking spot")
                .setView(txtUrl)
                .setPositiveButton("Add",
                    DialogInterface.OnClickListener { dialog, whichButton ->
                        val url = txtUrl.text.toString()
                        val abc = URL(urlNominatim + "search?q=" + URLEncoder.encode(url.toString(), "UTF-8") + "&format=json")
                        //val task = MyAsyncTask()
                        //task.execute(url)
                        getAddressOrLocation(abc)
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, whichButton -> })
                .show()*/
            //showDialog()
        }
    }

    // Show a model
    private fun showDialog(){
        val alert: AlertDialog.Builder = AlertDialog.Builder(this)
        alert.setTitle("Add a parking spot")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        alert.setView(input)
        alert.setPositiveButton("Add") { dialog, whichButton ->
            val value: Editable? = input.text
            val url = URL(urlNominatim + "search?q=" + URLEncoder.encode(value.toString(), "UTF-8") + "&format=json")
            getAddressOrLocation(url)
        }

        alert.setNegativeButton(
            "Cancel"
        ) { dialog, whichButton ->
            // Canceled.
        }
        alert.show()
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (hasPermissions()) {
                initMap()
            } else {
                finish()
            }
        }
    }

    // Initialize map
    private fun initMap() {
        mMapView?.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        // add receiver to get location from tap
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                val url = URL(urlNominatim + "reverse?lat=" + p.latitude.toString() + "&lon=" + p.longitude.toString() + "&format=json")
                getAddressOrLocation(url)
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        mMapView?.overlays?.add(MapEventsOverlay(mReceive))

         // MiniMap
        //val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
        //this.mMapView?.overlays?.add(miniMapOverlay)
        mMapView?.controller?.setZoom(17.0)
        //setCenter(GeoPoint(51.229885, 4.413941), "Live location")
        getDataFromDb()
    }

    // Add marker through overlayitem
    private fun addMarker(geoPoint: GeoPoint, name: String) {
        items.add(OverlayItem(name, name, geoPoint))
        mMyLocationOverlay = ItemizedIconOverlay(items, null, applicationContext)
        mMapView?.overlays?.add(mMyLocationOverlay)
    }

    // Add marker (the better ay)
    private fun addM(geoPoint: GeoPoint, name: String, carId: String, timem: Int?, personName: String?) {
        //Bug: Marker moves + incorrect location //https://github.com/osmdroid/osmdroid/issues/1349
        mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        val marker = Marker(mMapView)
        marker.position = geoPoint
        // marker.icon = resources.getDrawable(R.drawable.carpinresized, resources.newTheme()) //
        marker.icon = ContextCompat.getDrawable(this, R.drawable.carpinres) //KEEP THIS AFTER POSITION
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Your car"
        mMapView?.overlays?.add(marker)
        timeField = findViewById(R.id.time_txtView)

        // Should be 'real' time
        var time =0
        if(timeField?.text.isNullOrEmpty()){
            if (timem != null) {
                time = timem
            }
        }else{
             time = timeField?.text.toString().toInt()
        }

        val person = intent.getSerializableExtra("person") as Person?
        var name = personName
        if(personName.isNullOrEmpty()){
             name = person?.name
        }
        mMapView?.controller?.setZoom(17.0)
        setCenter(geoPoint, "New Loc")

        // To constantly update the marker
        object : CountDownTimer(TimeUnit.MINUTES.toMillis(time.toLong()), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //time_txtView.setText("seconds remaining: " + millisUntilFinished / 1000)
                if((millisUntilFinished / 1000)>60){
                    marker.title = "Car from " + name + "\nLeaving in: " +  millisUntilFinished / 1000 / 60 +":"+ millisUntilFinished / 1000 % 60
                }else{
                    marker.title = "Car from " + name + "\nLeaving in: " + millisUntilFinished / 1000 +"s"
                }
                if((millisUntilFinished / 1000)>180){
                    marker.icon = resources.getDrawable(R.drawable.carred, resources.newTheme())
                }else if(((millisUntilFinished / 1000)<180) && ((millisUntilFinished / 1000)>120)){
                    marker.icon = resources.getDrawable(R.drawable.carorange, resources.newTheme())
                }else if(((millisUntilFinished / 1000)<120) && ((millisUntilFinished / 1000)>60)){
                    marker.icon = resources.getDrawable(R.drawable.carblue, resources.newTheme())
                }else{
                    marker.icon = resources.getDrawable(R.drawable.cargreen, resources.newTheme())
                }
            }
            // Deletes from DB and Map
            override fun onFinish() {
                marker.remove(mMapView)
                dbRef = FirebaseDatabase.getInstance("https://carapp-2fa29-default-rtdb.europe-west1.firebasedatabase.app").getReference("parking")
                dbRef.child(carId).removeValue()
            }
        }.start()
    }

    private fun setCenter(geoPoint: GeoPoint, name: String) {
        mMapView?.controller?.setCenter(geoPoint)
        //addMarker(geoPoint, name)
    }

    // Notifications, currently no use case
    fun createNotification(iconRes: Int, title: String, body: String, channelId: String) {
        notificationManager?.createNotificationChannel(mChannel!!)
        val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(body)
                .build()

        notificationManager?.notify(0, notification)
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    private fun getAddressOrLocation(url : URL) {
        var searchReverse = false
        Thread(Runnable {
            searchReverse = (url.toString().indexOf("reverse", 0, true) > -1)
            val client = OkHttpClient()
            val response: Response
            val request = Request.Builder()
                .url(url)
                .build()
            response = client.newCall(request).execute()
            val result = response.body!!.string()

            runOnUiThread {
                val jsonString = StringBuilder(result!!)
                val parser: Parser = Parser.default()
                if (searchReverse) {
                    val obj = parser.parse(jsonString) as JsonObject
                    /*createNotification(R.drawable.ic_menu_compass,
                        "Reverse lookup result",
                        obj.string("display_name")!!,
                        "my_channel_01")*/
                }
                else {
                    val array = parser.parse(jsonString) as JsonArray<JsonObject>
                    if (array.size > 0) {
                        val obj = array[0]
                        // mapView center must be updated here and not in doInBackground because of UIThread exception
                        val geoPoint = GeoPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble())
                        //setCenter(geoPoint, obj.string("display_name")!!)

                        //Get data
                        val name = obj.string("display_name")
                        //val name = "testStraat"
                        val lat = obj.string("lat")!!.toDouble()
                        val lon = obj.string("lon")!!.toDouble()

                        //time_txtView
                        timeField = findViewById(R.id.time_txtView)
                        val time = timeField?.text.toString().toInt()

                        //User
                        val intent = intent
                        //val username = intent.getStringExtra("username")
                        val person = intent.getSerializableExtra("person") as Person?

                        //ADD TO FIREBASE //Specify instance or else error
                        dbRef = FirebaseDatabase.getInstance("https://carapp-2fa29-default-rtdb.europe-west1.firebasedatabase.app").getReference("parking")
                        val carId = obj.int("place_id").toString()
                        val car= Car(carId,name,lat,lon, time,person)
                        if (carId != null) {
                            dbRef.child(carId).setValue(car)
                                .addOnCompleteListener{
                                    Toast.makeText(this,"Car saved",Toast.LENGTH_LONG).show()
                                    searchField?.text?.clear()

                                }.addOnFailureListener { err ->
                                    Toast.makeText(this,"Error ${err.message}",Toast.LENGTH_LONG).show()
                                }
                        }
                        addM(geoPoint, obj.string(("display_name"))!!,carId,null, null)
                    }

                    else {
                        Toast.makeText(applicationContext, "Address not found", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }).start()

    }


    private fun getDataFromDb(){
        dbRef = FirebaseDatabase.getInstance("https://carapp-2fa29-default-rtdb.europe-west1.firebasedatabase.app").getReference("parking")
        dbRef.get().addOnSuccessListener {

            val result = org.json.JSONObject(it.value as Map<*, *>)

            //your starting JSON object
            val startingJsonObj = JSONObject(result.toString())

            //initialize the array
            val resultJsonArray = JSONArray()

            //loop through all the startingJsonObj keys to get the value
            for (key in startingJsonObj.keys()) {
                val resultObj = JSONObject()
                val value = startingJsonObj.opt(key)
                //put values in individual json object
                resultObj.put(key, value)
                //put json object into the final array
                resultJsonArray.put(resultObj)
            }



            //Log.d("jsonObject",result.toString())
            //val data = JSONArray(it.value)
            //Log.d("JsonString",result.toString())
//            val jsonString = StringBuilder(result!!)
//            Log.d("JsonString",jsonString.toString())
//            val parser: Parser = Parser.default()
//            val array = parser.parse(jsonString)
//            val data = JSONObject(jsonString) as JSONObject
//            Log.d("jsonObject",array.toString())
            // val array = parser.parse(jsonString) as JsonArray<JsonObject>

            if (resultJsonArray.length()>0) {
                for (i in 0 until resultJsonArray.length()) {
                    val item = resultJsonArray.getJSONObject(i)
                    
                    var dd = item.names()[0].toString()
                    var items = item.getJSONObject(dd)



                    var lat = items.getDouble("lat")
                    var lon = items.getDouble("lon")
                    var carId = items.getString("carId")
                    var location =  items.getString("location")
                    var time =  items.getString("time")
                    var a = items.getJSONObject("person").getString("name")

                   val geoPoint = GeoPoint(lat!!.toDouble(), lon!!.toDouble())
                    //setCenter(geoPoint, obj.string("display_name")!!)
                    addM(geoPoint, location!!, carId!!, time.toInt(), a)
                    // Your code here
                }
                //val obj = resultJsonArray[0]

                //Log.d("object",obj.toString())
//                // mapView center must be updated here and not in doInBackground because of UIThread exception
//                val geoPoint = GeoPoint(obj.("lat")!!.toDouble(), obj.string("lon")!!.toDouble())
//
//                Log.d("firebasedataxx", "$obj")
//                //setCenter(geoPoint, obj.string("display_name")!!)
//                //addM(geoPoint, obj.string(("display_name"))!!)
            }


        }
    }


    // AsyncTask inner class
    /*@SuppressLint("StaticFieldLeak")
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {

        private var searchReverse = false

        override fun doInBackground(vararg params: URL?): String {

            searchReverse = (params[0]!!.toString().indexOf("reverse", 0, true) > -1)
            val client = OkHttpClient()
            val response: Response
            val request = Request.Builder()
                    .url(params[0]!!)
                    .build()
            response = client.newCall(request).execute()

            return response.body!!.string()
        }

        // vararg : variable number of arguments
        // * : spread operator, unpacks an array into the list of values from the array
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val jsonString = StringBuilder(result!!)
            Log.d("be.ap.edu.mapsaver", jsonString.toString())

            val parser: Parser = Parser.default()

            if (searchReverse) {
                val obj = parser.parse(jsonString) as JsonObject

                createNotification(R.drawable.ic_menu_compass,
                        "Reverse lookup result",
                        obj.string("display_name")!!,
                        "my_channel_01")
            }
            else {
                val array = parser.parse(jsonString) as JsonArray<JsonObject>

                if (array.size > 0) {
                    val obj = array[0]
                    // mapView center must be updated here and not in doInBackground because of UIThread exception
                    val geoPoint = GeoPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble())
                    setCenter(geoPoint, obj.string("display_name")!!)
                }
                else {
                    Toast.makeText(applicationContext, "Address not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }*/

    /*
       clearButton = findViewById(R.id.clear_button)

       clearButton?.setOnClickListener {
           mMapView?.overlays?.clear() // Keep the live location
           // Redraw map
           mMapView?.invalidate()
       }*/
}

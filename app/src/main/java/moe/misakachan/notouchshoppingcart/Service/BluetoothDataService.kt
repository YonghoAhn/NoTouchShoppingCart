package moe.misakachan.notouchshoppingcart.Service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_STEP_DETECTOR
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.annotation.BoolRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import moe.misakachan.notouchshoppingcart.MainActivity
import moe.misakachan.notouchshoppingcart.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class BluetoothDataService : Service(), SensorEventListener, LocationListener{
    val handlerState = 0 //used to identify handler message
    var bluetoothIn: Handler? = null
    private var btAdapter: BluetoothAdapter? = null
    private var mConnectingThread: ConnectingThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var stopThread = false
    private val recDataString = StringBuilder()
    private var macAddress = "YOUR:MAC:ADDRESS:HERE"
    private val mBinder: IBinder = BluetoothDataServiceBinder()

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mMagnetometer: Sensor? = null
    private val mLastAccelerometer = FloatArray(3)
    private val mLastMagnetometer = FloatArray(3)
    private var mLastAccelerometerSet = false
    private var mLastMagnetometerSet = false
    private val mR = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var mCurrentDegree = 0f
    private var mStep = 0
    private var mStepDetector: Sensor? = null
    private var isGPSEnabled = false
    private val locationManager: LocationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private var lastKnownLocation: Location? = null
    private var nowKnownLocation: Location? = null

    var isAutoMode = true
    set(value) {
        if(value)
            registerReceiver(manualControlReceiver, IntentFilter("ACTION_MANUAL_CONTROL"))
        else
            unregisterReceiver(manualControlReceiver)
        field = value
    }

    private val modeChangeReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val mode = intent?.getBooleanExtra("mode" ,true)
            if (mode != null) {
                isAutoMode = mode
            }
        }
    }

    private val manualControlReceiver = object :BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
            //U/D/L/R
            val direction = intent?.getStringExtra("direction")
            if(direction!=null && mConnectedThread!=null)
                mConnectedThread!!.write("MOVE\n$direction\n")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BT SERVICE", "SERVICE CREATED")
        stopThread = false
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mStepDetector = mSensorManager!!.getDefaultSensor(TYPE_STEP_DETECTOR)
        if(mStepDetector != null)
            Log.d("MisakaMOE", mStepDetector!!.name)
        registerReceiver(modeChangeReceiver, IntentFilter("ACTION_CONTROL_MODE_CHANGE"))
        //If GPS blocked, stop Service.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BT SERVICE", "SERVICE STARTED")
        mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_NORMAL)

        macAddress = intent?.getStringExtra("MAC").toString()
        bluetoothIn = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                Log.d("DEBUG", "handleMessage")
                if (msg.what == handlerState) { //if message is what we want
                    val readMessage =
                        msg.obj as String // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage)
                    Log.d("RECORDED", recDataString.toString())
                    // Do stuff here with your data, like adding it to the database
                    val localBroadcastManager =
                        LocalBroadcastManager.getInstance(applicationContext)
                    val msgIntent = Intent("ACTION_BT_SERIAL_RECEIVE")
                    msgIntent.putExtra("msg", recDataString.toString())
                    localBroadcastManager.sendBroadcast(msgIntent)
                }
                recDataString.delete(0, recDataString.length) //clear all string data
            }
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter() // get Bluetooth adapter
        checkBTState()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothIn?.removeCallbacksAndMessages(null)
        stopThread = true
        if (mConnectedThread != null) {
            mConnectedThread!!.closeStreams()
        }
        if (mConnectingThread != null) {
            mConnectingThread!!.closeSocket()
        }

        mSensorManager!!.unregisterListener(this, mAccelerometer)
        mSensorManager!!.unregisterListener(this, mMagnetometer)
        mSensorManager!!.unregisterListener(this, mStepDetector)
        locationManager.removeUpdates(this)
        unregisterReceiver(modeChangeReceiver)
        Log.d("SERVICE", "onDestroy")
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private fun checkBTState() {
        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE")
            stopSelf()
        } else {
            if (btAdapter!!.isEnabled) {
                Log.d(
                    "DEBUG BT",
                    "BT ENABLED! BT ADDRESS : " + btAdapter!!.address + " , BT NAME : " + btAdapter!!.name
                )
                try {
                    val device =
                        btAdapter!!.getRemoteDevice(macAddress)
                    Log.d(
                        "DEBUG BT",
                        "ATTEMPTING TO CONNECT TO REMOTE DEVICE : $macAddress"
                    )
                    mConnectingThread = ConnectingThread(device)
                    mConnectingThread!!.start()
                } catch (e: IllegalArgumentException) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : $e")
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE")
                    stopSelf()
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE")
                stopSelf()
            }
        }
    }

    // New Class for Connecting Thread
    private inner class ConnectingThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice
        override fun run() {
            super.run()
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN")
            // Establish the Bluetooth socket connection.
// Cancelling discovery as it may slow down connection
            btAdapter!!.cancelDiscovery()
            try {
                mmSocket!!.connect()
                Log.d("DEBUG BT", "BT SOCKET CONNECTED")
                mConnectedThread = ConnectedThread(mmSocket)
                mConnectedThread!!.start()
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED")
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                mConnectedThread!!.write("x")
            } catch (e: IOException) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : $e")
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE")
                    mmSocket!!.close()
                    stopSelf()
                } catch (e2: IOException) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :$e2")
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE")
                    stopSelf()
                    //insert code to deal with this
                }
            } catch (e: IllegalStateException) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : $e")
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        fun closeSocket() {
            try { //Don't leave Bluetooth sockets open when leaving activity
                mmSocket!!.close()
            } catch (e2: IOException) { //insert code to deal with this
                Log.d("DEBUG BT", e2.toString())
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        init {
            Log.d("DEBUG BT", "IN CONNECTING THREAD")
            mmDevice = device
            var temp: BluetoothSocket? = null
            Log.d("DEBUG BT", "MAC ADDRESS : $macAddress")
            Log.d("DEBUG BT", "BT UUID : $BTMODULEUUID")
            try {
                temp =
                    mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID)
                Log.d("DEBUG BT", "SOCKET CREATED : $temp")
            } catch (e: IOException) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :$e")
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE")
                stopSelf()
            }
            mmSocket = temp
        }
    }

    // New Class for Connected Thread
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN")
            val buffer = ByteArray(256)
            var bytes: Int
            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = mmInStream!!.read(buffer) //read bytes from input buffer
                    val readMessage = String(buffer, 0, bytes)
                    Log.d("DEBUG BT PART", "CONNECTED THREAD $readMessage")
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn!!.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget()
                } catch (e: IOException) {
                    Log.d("DEBUG BT", e.toString())
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                    stopSelf()
                    break
                }
            }
        }

        //write method
        fun write(input: String) {
            val msgBuffer =
                input.toByteArray() //converts entered String into bytes
            try {
                mmOutStream!!.write(msgBuffer) //write bytes over BT connection via outstream
            } catch (e: IOException) { //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE $e")
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                stopSelf()
            }
        }

        fun closeStreams() {
            try { //Don't leave Bluetooth sockets open when leaving activity
                mmInStream!!.close()
                mmOutStream!!.close()
            } catch (e2: IOException) { //insert code to deal with this
                Log.d("DEBUG BT", e2.toString())
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE")
                stopSelf()
            }
        }

        //creation of the connect thread
        init {
            Log.d("DEBUG BT", "IN CONNECTED THREAD")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try { //Create I/O streams for connection
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.d("DEBUG BT", e.toString())
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE")
                stopSelf()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val builder: NotificationCompat.Builder
        builder = if (Build.VERSION.SDK_INT >= 26) {
            val channelId = "NoTouchShoppingCart_Channel"
            val channel = NotificationChannel(
                channelId,
                "NoTouchShoppingCart Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            if(!getSharedPreferences("SETTING", Context.MODE_PRIVATE).getBoolean("isNotificationCreated",false)) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                getSharedPreferences("SETTING", Context.MODE_PRIVATE).edit().putBoolean("isNotificationCreated",true).apply()
            }
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }
        builder.setSmallIcon(R.drawable.cartcircle)
            .setContentTitle("Bluetooth Communication")
            .setContentText("Bluetooth Connected with Cart.")
            .setContentIntent(pendingIntent)
        startForeground(1, builder.build())
    }

    companion object {
        // SPP UUID service - this should work for most devices
        private val BTMODULEUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private fun distance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val theta = lon1 - lon2
            var dist = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * cos(deg2rad(theta))
            dist = acos(dist)
            dist = rad2deg(dist)
            dist *= 60 * 1.1515
            dist *= 1.609344 * 1000 //mile to meter
            return dist
        }

        // This function converts decimal degrees to radians
        private fun deg2rad(deg: Double): Double {
            return deg * Math.PI / 180.0
        }

        // This function converts radians to decimal degrees
        private fun rad2deg(rad: Double): Double {
            return rad * 180 / Math.PI
        }
    }

    inner class BluetoothDataServiceBinder : Binder() {
        fun getService(): BluetoothDataService = this@BluetoothDataService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if(isAutoMode) {
            if (event!!.sensor == mAccelerometer) {
                Log.d("MisakaMOE", "Accel")
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.size)
                mLastAccelerometerSet = true
            } else if (event.sensor == mMagnetometer) {
                Log.d("MisakaMOE", "Magneto")

                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.size)
                mLastMagnetometerSet = true
            } else if (event.sensor.type == TYPE_STEP_DETECTOR) {
                Log.d("MisakaMOE", "STEP")

                if (event.values[0] == 1.0f) {
                    //call GPS Code
                    if (GPSLocation > 0.03) {
                        mStep++

                        mConnectedThread?.write("STEP\n")
                        mConnectedThread?.write(mStep.toString()+"\n")
                        mConnectedThread?.write("ANGLE\n")
                        mConnectedThread?.write(mCurrentDegree.toString()+"\n")

                        val localBroadcastManager =
                            LocalBroadcastManager.getInstance(applicationContext)
                        val msgIntent = Intent("ACTION_STEP_DETECTED")
                        msgIntent.putExtra("step", mStep)
                        localBroadcastManager.sendBroadcast(msgIntent)
                    }
                }
            }

            if (mLastAccelerometerSet && mLastMagnetometerSet) {
                SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer)
                val azimuthDegrees = (Math.toDegrees(
                    SensorManager.getOrientation(mR, mOrientation)[0].toDouble()
                ) + 360).toInt() % 360.toFloat()
                mCurrentDegree = -azimuthDegrees

                //mConnectedThread?.write("ANGLE")
                //mConnectedThread?.write(mCurrentDegree.toString())
                val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
                val msgIntent = Intent("ACTION_AZIMUTH_CHANGED")
                msgIntent.putExtra("azimuth", mCurrentDegree)
                val stepIntent = Intent("ACTION_STEP_DETECTED").putExtra("step",mStep)
                localBroadcastManager.sendBroadcast(msgIntent)
                localBroadcastManager.sendBroadcast(stepIntent)
            }
       }
    }

    private val GPSLocation : Double
        get() {
            var dTime = 0.0
            var dDist = 0.0
            if(isGPSEnabled)
            {
                if(lastKnownLocation == null)
                    lastKnownLocation = nowKnownLocation
                if(lastKnownLocation != null && nowKnownLocation != null)
                {
                    val lat1 = lastKnownLocation!!.latitude
                    val lng1 = lastKnownLocation!!.longitude
                    val lat2 = nowKnownLocation!!.latitude
                    val lng2 = nowKnownLocation!!.longitude
                    dTime = (nowKnownLocation!!.time - lastKnownLocation!!.time) / 1000.0
                    dDist = distance(lat1,lng1,lat2,lng2)
                    if(dDist > 0.03)
                    {
                        lastKnownLocation = nowKnownLocation
                        return dDist
                    }
                }
            }
            return 0.0
        }

    override fun onLocationChanged(location: Location?) {
        nowKnownLocation = location
        val lng = location?.longitude
        val lat = location?.latitude
    }


    override fun onProviderEnabled(provider: String?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // 위치정보 업데이트
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    override fun onProviderDisabled(provider: String?) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

}
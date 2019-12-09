package moe.misakachan.notouchshoppingcart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import moe.misakachan.notouchshoppingcart.Service.BluetoothDataService

class MainActivity : AppCompatActivity() {
    private lateinit var mService: BluetoothDataService
    private var mBound: Boolean = false

    val BluetoothBroadcastRecevier  = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
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
                NotificationCompat.Builder(applicationContext, channelId)
            } else {
                NotificationCompat.Builder(applicationContext)
            }
            val direction = intent?.getStringExtra("direction")
            builder.setSmallIcon(R.drawable.cartcircle)
                .setContentTitle("Manual operation is required.")
                .setContentText("The $direction route was blocked by something.")
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(0, builder.build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //if(mBound)
        //    mService.sendMessage("test")
        val navController = Navigation.findNavController(this, R.id.mainNavHostFragment)
        bottomNavigationView.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }
}

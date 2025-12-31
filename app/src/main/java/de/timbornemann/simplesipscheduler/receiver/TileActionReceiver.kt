package de.timbornemann.simplesipscheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TileActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderReceiver.ACTION_ADD_DRINK) {
            val amount = intent.getIntExtra(ReminderReceiver.EXTRA_AMOUNT, 0)
            if (amount > 0) {
                val app = context.applicationContext as? SimpleSipApplication
                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        it.drinkRepository.addDrink(amount)
                    }
                }
            }
        }
    }
}



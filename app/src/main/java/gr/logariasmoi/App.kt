package gr.logariasmoi

import android.app.Application
import gr.logariasmoi.data.Repository
import gr.logariasmoi.notify.Notifications
import gr.logariasmoi.notify.ReminderScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Notifications.createChannel(this)
        ReminderScheduler.schedule(this)
    }
}

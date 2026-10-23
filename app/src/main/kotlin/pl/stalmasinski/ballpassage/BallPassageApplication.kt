package pl.stalmasinski.ballpassage

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import pl.stalmasinski.ballpassage.data.DataStoreUserDataRepository
import pl.stalmasinski.ballpassage.data.UserDataRepository

private val Context.userDataStore by preferencesDataStore(name = "user_data")

class BallPassageApplication : Application() {
    val userDataRepository: UserDataRepository by lazy { DataStoreUserDataRepository(userDataStore) }
}

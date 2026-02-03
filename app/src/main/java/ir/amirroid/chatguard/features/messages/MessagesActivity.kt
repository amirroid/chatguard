package ir.amirroid.chatguard.features.messages

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.amirroid.chatguard.ui.theme.ChatGuardTheme
import org.koin.androidx.compose.koinViewModel

class MessagesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MessagesViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ChatGuardTheme {
                MessagesModalBottomSheet(
                    state = state,
                    onSendPublicKey = {
                        viewModel.sendPublicKey(onSuccess = ::finish)
                    },
                    onSendMessage = {
                        viewModel.sendMessage(onSuccess = ::finish)
                    },
                    onMessageValueChange = viewModel::updateMessageText,
                    onSavePublicKey = viewModel::acceptPublicKey,
                    onDismissRequest = ::finish
                )
            }
        }
    }
}
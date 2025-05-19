package cl.puntito.simple_pip_mode

import androidx.annotation.NonNull
import cl.puntito.simple_pip_mode.actions.PipAction
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.FlutterEngine


open class PipCallbackHelper {
    private val CHANNEL = "puntito.simple_pip_mode"
    private lateinit var channel: MethodChannel

    fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
    }

    fun setChannel(channel: MethodChannel) {
        this.channel = channel
    }

    fun onPictureInPictureModeChanged(active: Boolean) {
        if (active) {
            channel.invokeMethod("onPipEntered", null)
        } else {
            channel.invokeMethod("onPipExited", null)
        }
    }    fun onPipAction(action: PipAction) {
        when (action) {
            PipAction.CUSTOM -> {
                channel.invokeMethod("onCustomPipAction", "custom_action")
            }
            PipAction.MIC_ON -> {
                channel.invokeMethod("onMicAction", "mic_on")
            }
            PipAction.MIC_OFF -> {
                channel.invokeMethod("onMicAction", "mic_off")
            }
            PipAction.CAMERA_ON -> {
                channel.invokeMethod("onCameraAction", "camera_on")
            }
            PipAction.CAMERA_OFF -> {
                channel.invokeMethod("onCameraAction", "camera_off")
            }
            else -> {
                channel.invokeMethod("onPipAction", action.name.lowercase())
            }
        }
    }
    
    // Additional method to send custom messages from the PIP action
    fun sendCustomActionMessage(message: String) {
        channel.invokeMethod("onCustomPipAction", message)
    }
    
    // Additional method to send mic state messages from the PIP action
    fun sendMicStateMessage(isMicOn: Boolean) {
        channel.invokeMethod("onMicAction", if (isMicOn) "mic_on" else "mic_off")
    }
    
    // Additional method to send camera state messages from the PIP action
    fun sendCameraStateMessage(isCameraOn: Boolean) {
        channel.invokeMethod("onCameraAction", if (isCameraOn) "camera_on" else "camera_off")
    }
}
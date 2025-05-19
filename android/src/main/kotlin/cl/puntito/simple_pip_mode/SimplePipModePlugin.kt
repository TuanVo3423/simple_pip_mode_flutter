package cl.puntito.simple_pip_mode

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import cl.puntito.simple_pip_mode.Constants.EXTRA_ACTION_TYPE
import cl.puntito.simple_pip_mode.Constants.SIMPLE_PIP_ACTION
import cl.puntito.simple_pip_mode.actions.PipAction
import cl.puntito.simple_pip_mode.actions.PipActionsLayout
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler


/** PIP_METHODS enum */
enum class PIP_METHODS(val methodName: String) {
    GET_PLATFORM_VERSION("getPlatformVersion"),
    IS_PIP_AVAILABLE("isPipAvailable"),
    IS_PIP_ACTIVATED("isPipActivated"),
    IS_AUTO_PIP_AVAILABLE("isAutoPipAvailable"),
    ENTER_PIP_MODE("enterPipMode"),
    EXIT_PIP_MODE("exitPipMode"),
    SET_PIP_LAYOUT("setPipLayout"),
    SET_IS_PLAYING("setIsPlaying"),
    SET_AUTO_PIP_MODE("setAutoPipMode"),
    SEND_CUSTOM_ACTION_MESSAGE("sendCustomActionMessage"),
    SET_MIC_STATE("setMicState"),
    SET_CAMERA_STATE("setCameraState"),
    SET_MIC_AND_CAMERA_STATES("setMicAndCameraStates"),
}

/** SimplePipModePlugin */
class SimplePipModePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private val CHANNEL = "puntito.simple_pip_mode"
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var activity: Activity
    private var actions: MutableList<RemoteAction> = mutableListOf()
    private var actionsLayout: PipActionsLayout = PipActionsLayout.NONE

    private var callbackHelper = PipCallbackHelper()
    private var params: PictureInPictureParams.Builder? = null
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL)
        callbackHelper.setChannel(channel)
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        broadcastReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onReceive(context: Context, intent: Intent) {
                if (SIMPLE_PIP_ACTION !== intent.action) {
                    return
                }
                intent.getStringExtra(EXTRA_ACTION_TYPE)?.let {
                    val action = PipAction.valueOf(it)
                    
                    when (action) {
                        // Special handling for custom action
                        PipAction.CUSTOM -> {
                            callbackHelper.onPipAction(action)
                        }
                        // Special handling for mic actions
                        PipAction.MIC_ON, PipAction.MIC_OFF -> {
                            action.afterAction()?.let {
                                toggleAction(action)
                            }
                            callbackHelper.onPipAction(action)
                        }
                        // Special handling for camera actions
                        PipAction.CAMERA_ON, PipAction.CAMERA_OFF -> {
                            action.afterAction()?.let {
                                toggleAction(action)
                            }
                            callbackHelper.onPipAction(action)
                        }
                        else -> {
                            action.afterAction()?.let {
                                toggleAction(action)
                            }
                            callbackHelper.onPipAction(action)
                        }
                    }
                }
            }
        }.also { broadcastReceiver = it }

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter(SIMPLE_PIP_ACTION),
            RECEIVER_EXPORTED
        )
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        context.unregisterReceiver(broadcastReceiver)
    }    @RequiresApi(Build.VERSION_CODES.O)    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            PIP_METHODS.GET_PLATFORM_VERSION.methodName -> getPlatformVersion(result)
            PIP_METHODS.IS_PIP_AVAILABLE.methodName -> isPipAvailable(result)
            PIP_METHODS.IS_PIP_ACTIVATED.methodName -> isPipActivated(result)
            PIP_METHODS.IS_AUTO_PIP_AVAILABLE.methodName -> isAutoPipAvailable(result)
            PIP_METHODS.ENTER_PIP_MODE.methodName -> enterPipMode(call, result)
            PIP_METHODS.EXIT_PIP_MODE.methodName -> exitPipMode(result)
            PIP_METHODS.SET_PIP_LAYOUT.methodName -> setPipLayout(call, result)
            PIP_METHODS.SET_IS_PLAYING.methodName -> setIsPlaying(call, result)
            PIP_METHODS.SET_AUTO_PIP_MODE.methodName -> setAutoPipMode(call, result)
            PIP_METHODS.SEND_CUSTOM_ACTION_MESSAGE.methodName -> sendCustomActionMessage(call, result)
            PIP_METHODS.SET_MIC_STATE.methodName -> setMicState(call, result)
            PIP_METHODS.SET_CAMERA_STATE.methodName -> setCameraState(call, result)
            PIP_METHODS.SET_MIC_AND_CAMERA_STATES.methodName -> setMicAndCameraStates(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
    }

    /* METHOD IMPLEMENTATION */

    private fun getPlatformVersion(result: MethodChannel.Result) {
        result.success("Android ${Build.VERSION.RELEASE}")
    }

    private fun isPipAvailable(result: MethodChannel.Result) {
        result.success(
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        )
    }

    private fun isPipActivated(result: MethodChannel.Result) {
        result.success(activity.isInPictureInPictureMode)
    }

    private fun isAutoPipAvailable(result: MethodChannel.Result) {
        result.success(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }

    private fun enterPipMode(call: MethodCall, result: MethodChannel.Result) {
        val aspectRatio = call.argument<List<Int>>("aspectRatio")
        val autoEnter = call.argument<Boolean>("autoEnter")
        val seamlessResize = call.argument<Boolean>("seamlessResize")
        var params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(aspectRatio!![0], aspectRatio[1]))
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params = params.setAutoEnterEnabled(autoEnter!!)
                .setSeamlessResizeEnabled(seamlessResize!!)
        }

        this.params = params

        result.success(
            activity.enterPictureInPictureMode(params.build())
        )
    }    /**
     * Exits Picture-in-Picture mode without terminating the app
     * This can be called from Flutter to programmatically exit PiP mode
     * The window will be hidden but the app will remain in memory so user can reopen it
     */    private fun exitPipMode(result: MethodChannel.Result) {
        try {
            // Check if the device supports PiP and if we're currently in PiP mode
            if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
                activity.isInPictureInPictureMode) {
                
                // Use moveTaskToBack which hides the activity without destroying it
                activity.moveTaskToBack(true)
                
                result.success(true)
            } else {
                // If we're not in PiP mode, just return success
                result.success(true)
            }
        } catch (e: Exception) {
            Log.e("PIP", "Error exiting PiP mode: ${e.message}")
            result.error("ExitPipError", "Error exiting PiP mode", e.message)
        }
    }

    private fun setPipLayout(call: MethodCall, result: MethodChannel.Result) {
        val success = call.argument<String>("layout")?.let {
            try {
                Log.i("PIP", "layout = ${convertAction(it)}")
                actionsLayout = PipActionsLayout.valueOf(convertAction(it))
                actions = actionsLayout.remoteActions(context)
                true
            } catch (e: Exception) {
                Log.e("PIP", e.message?: "Error setting layout")
                false
            }
        } ?: false
        result.success(success)
    }

    private fun setIsPlaying(call: MethodCall, result: MethodChannel.Result) {
        call.argument<Boolean>("isPlaying")?.let { isPlaying ->
            if (actionsLayout.actions.contains(PipAction.PLAY) ||
                actionsLayout.actions.contains(PipAction.PAUSE)
            ) {
                var i = actionsLayout.actions.indexOf(PipAction.PLAY)
                if (i == -1) {
                    i = actionsLayout.actions.indexOf(PipAction.PAUSE)
                }
                if (i >= 0) {
                    actionsLayout.actions[i] =
                        if (isPlaying) PipAction.PAUSE else PipAction.PLAY
                    renderPipActions()
                    result.success(true)
                }
            } else {
                result.success(false)
            }
        } ?: result.success(false)
    }

    private fun setAutoPipMode(call: MethodCall, result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val aspectRatio = call.argument<List<Int>>("aspectRatio")
            val autoEnter = call.argument<Boolean>("autoEnter")
            val seamlessResize = call.argument<Boolean>("seamlessResize")
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(aspectRatio!![0], aspectRatio[1]))
                .setAutoEnterEnabled(autoEnter!!)
                .setSeamlessResizeEnabled(seamlessResize!!)
                .setActions(actions)

            this.params = params

            activity.setPictureInPictureParams(params.build())

            result.success(true)
        } else {
            result.error(
                "NotImplemented",
                "System Version less than Android S found",
                "Expected Android S or newer."
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleAction(action: PipAction) {
        actionsLayout.toggleToAfterAction(action)
        renderPipActions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPipActions() {
        actions = PipActionsLayout.remoteActions(context, actionsLayout.actions)
        params?.let {
            it.setActions(actions).build()
            activity.setPictureInPictureParams(it.build())
        }
    }

    private fun convertAction(action: String) = action
        .replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace(Regex("([a-z])([0-9])"), "$1_$2")
        .uppercase()

    // New method to send custom action messages from PIP
    private fun sendCustomActionMessage(call: MethodCall, result: MethodChannel.Result) {
        val message = call.argument<String>("message") ?: "default_message"
        channel.invokeMethod("onCustomPipAction", message)
        result.success(true)
    }

    private fun setMicState(call: MethodCall, result: MethodChannel.Result) {
        val actionName = call.argument<String>("action")
        val micOn = actionName == PipAction.MIC_ON.name
        
        // Find any mic actions in the current layout and replace them
        if (actionsLayout.actions.contains(PipAction.MIC_ON) || 
            actionsLayout.actions.contains(PipAction.MIC_OFF)) {
            
            val targetAction = if (micOn) PipAction.MIC_ON else PipAction.MIC_OFF
            
            // Find current mic action index
            var micActionIndex = actionsLayout.actions.indexOf(PipAction.MIC_ON)
            if (micActionIndex == -1) {
                micActionIndex = actionsLayout.actions.indexOf(PipAction.MIC_OFF)
            }
            
            if (micActionIndex >= 0) {
                actionsLayout.actions[micActionIndex] = targetAction
                renderPipActions()
                
                // Send the mic state change back to Flutter
                // callbackHelper.sendMicStateMessage(micOn)
                
                result.success(true)
                return
            }
        }
        
        result.success(false)
    }

    private fun setCameraState(call: MethodCall, result: MethodChannel.Result) {
        val actionName = call.argument<String>("action")
        val cameraOn = actionName == PipAction.CAMERA_ON.name
        
        // Find any camera actions in the current layout and replace them
        if (actionsLayout.actions.contains(PipAction.CAMERA_ON) || 
            actionsLayout.actions.contains(PipAction.CAMERA_OFF)) {
            
            val targetAction = if (cameraOn) PipAction.CAMERA_ON else PipAction.CAMERA_OFF
            
            // Find current camera action index
            var cameraActionIndex = actionsLayout.actions.indexOf(PipAction.CAMERA_ON)
            if (cameraActionIndex == -1) {
                cameraActionIndex = actionsLayout.actions.indexOf(PipAction.CAMERA_OFF)
            }
            
            if (cameraActionIndex >= 0) {
                actionsLayout.actions[cameraActionIndex] = targetAction
                renderPipActions()
                
                // Send the camera state change back to Flutter
                // callbackHelper.sendCameraStateMessage(cameraOn)
                
                result.success(true)
                return
            }
        }
        
        result.success(false)
    }

    private fun setMicAndCameraStates(call: MethodCall, result: MethodChannel.Result) {
        val micAction = call.argument<String>("micAction")
        val cameraAction = call.argument<String>("cameraAction")
        
        val micOn = micAction == PipAction.MIC_ON.name
        val cameraOn = cameraAction == PipAction.CAMERA_ON.name
        
        var micChanged = false
        var cameraChanged = false
        
        // Update mic state if the layout has mic controls
        if (actionsLayout.actions.contains(PipAction.MIC_ON) || 
            actionsLayout.actions.contains(PipAction.MIC_OFF)) {
            
            val targetMicAction = if (micOn) PipAction.MIC_ON else PipAction.MIC_OFF
            
            // Find current mic action index
            var micActionIndex = actionsLayout.actions.indexOf(PipAction.MIC_ON)
            if (micActionIndex == -1) {
                micActionIndex = actionsLayout.actions.indexOf(PipAction.MIC_OFF)
            }
            
            if (micActionIndex >= 0) {
                actionsLayout.actions[micActionIndex] = targetMicAction
                micChanged = true
                
                // Send the mic state change back to Flutter
                callbackHelper.sendMicStateMessage(micOn)
            }
        }
        
        // Update camera state if the layout has camera controls
        if (actionsLayout.actions.contains(PipAction.CAMERA_ON) || 
            actionsLayout.actions.contains(PipAction.CAMERA_OFF)) {
            
            val targetCameraAction = if (cameraOn) PipAction.CAMERA_ON else PipAction.CAMERA_OFF
            
            // Find current camera action index
            var cameraActionIndex = actionsLayout.actions.indexOf(PipAction.CAMERA_ON)
            if (cameraActionIndex == -1) {
                cameraActionIndex = actionsLayout.actions.indexOf(PipAction.CAMERA_OFF)
            }
            
            if (cameraActionIndex >= 0) {
                actionsLayout.actions[cameraActionIndex] = targetCameraAction
                cameraChanged = true
                
                // Send the camera state change back to Flutter
                // callbackHelper.sendCameraStateMessage(cameraOn)
            }
        }
        
        // If either state changed, update the PIP actions
        if (micChanged || cameraChanged) {
            renderPipActions()
            result.success(true)
        } else {
            result.success(false)
        }
    }
}

import 'dart:async';

import 'package:flutter/services.dart';
import 'package:simple_pip_mode/actions/pip_action.dart';
import 'package:simple_pip_mode/actions/pip_actions_layout.dart';
import 'package:simple_pip_mode/aspect_ratio.dart';

/// Main controller class.
/// It can verify whether the system supports PIP,
/// check whether the app is currently in PIP mode,
/// request entering PIP mode,
/// and call some callbacks when the app changes its mode.
class SimplePip {
  static const _channel = MethodChannel('puntito.simple_pip_mode');

  /// Whether this device supports PIP mode.
  static Future<bool> get isPipAvailable async {
    final bool? isAvailable = await _channel.invokeMethod('isPipAvailable');
    return isAvailable ?? false;
  }

  /// Whether the device supports AutoEnter PIP parameter (Android S)
  static Future<bool> get isAutoPipAvailable async {
    final bool? isAvailable = await _channel.invokeMethod('isAutoPipAvailable');
    return isAvailable ?? false;
  }

  /// Whether the app is currently in PIP mode.
  static Future<bool> get isPipActivated async {
    final bool? isActivated = await _channel.invokeMethod('isPipActivated');
    return isActivated ?? false;
  }

  /// Called when the app enters PIP mode
  VoidCallback? onPipEntered;

  /// Called when the app exits PIP mode
  VoidCallback? onPipExited;

  /// Called when the user taps on a PIP action
  void Function(PipAction)? onPipAction;

  /// Called when the user taps on a custom PIP action
  void Function(String)? onCustomPipAction;

  /// Called when the user taps on the microphone PIP action
  void Function(String)? onMicAction;

  /// Called when the user taps on the camera PIP action
  void Function(String)? onCameraAction;

  Future<bool> exitPipMode() async {
    final bool? exitSuccessfully = await _channel.invokeMethod('exitPipMode');
    return exitSuccessfully ?? false;
  }

  /// Request entering PIP mode
  Future<bool> enterPipMode({
    AspectRatio aspectRatio = const (16, 9),
    bool autoEnter = false,
    bool seamlessResize = false,
  }) async {
    Map params = {
      'aspectRatio': aspectRatio.asList,
      'autoEnter': autoEnter,
      'seamlessResize': seamlessResize,
    };
    final bool? enteredSuccessfully = await _channel.invokeMethod(
      'enterPipMode',
      params,
    );
    return enteredSuccessfully ?? false;
  }

  /// Request setting automatic PIP mode.
  /// Android 12 (Android S, API level 31) or newer required.
  Future<bool> setAutoPipMode({
    AspectRatio aspectRatio = const (16, 9),
    bool seamlessResize = false,
    bool autoEnter = true,
  }) async {
    Map params = {
      'aspectRatio': aspectRatio.asList,
      'autoEnter': autoEnter,
      'seamlessResize': seamlessResize,
    };
    final bool? setSuccessfully = await _channel.invokeMethod(
      'setAutoPipMode',
      params,
    );
    return setSuccessfully ?? false;
  }

  /// Updates the current actions layout with a preset layout
  /// The preset layout is defined by [PipActionsLayout] and it's equivalent enum inside Android src
  Future<bool> setPipActionsLayout(PipActionsLayout layout) async {
    Map params = {'layout': layout.name};
    final bool? setSuccessfully = await _channel.invokeMethod(
      'setPipLayout',
      params,
    );
    return setSuccessfully ?? false;
  }

  /// Updates the actions [PipAction.play] and [PipAction.pause]
  /// When it is called it does re-render the action inside PIP acording with [isPlaying] value
  ///
  /// If [isPlaying] is `true` then PIP will shows [PipAction.pause] action
  /// If [isPlaying] is `false` then PIP will shows [PipAction.play] action
  ///
  /// NOTE: This method should ONLY be used to update PIP action when the player state was changed by
  /// OTHER button that is NOT the PIP's one (ex.: the player play/pause button, notification controller play/pause button
  /// or whatever button you have that calls your playerController's play/pause). When user taps PIP's [PipAction.play] or
  /// [PipAction.pause] it automatically updates the action, WITHOUT NEEDING to call this [setIsPlaying] method.
  ///
  /// Only affects media actions layout presets or presets that uses [PipAction.play] or [PipAction.pause] actions.
  Future<bool> setIsPlaying(bool isPlaying) async {
    Map params = {'isPlaying': isPlaying};
    final bool? setSuccessfully = await _channel.invokeMethod(
      'setIsPlaying',
      params,
    );
    return setSuccessfully ?? false;
  }

  /// Sends a custom message that will be received in your Flutter app
  /// when the custom PIP action is triggered
  Future<bool> sendCustomActionMessage(String message) async {
    Map params = {'message': message};
    final bool? sentSuccessfully = await _channel.invokeMethod(
      'sendCustomActionMessage',
      params,
    );
    return sentSuccessfully ?? false;
  }

  /// Sets the microphone state in PIP mode (on/off)
  /// This should be called when the microphone state changes from outside the PIP mode
  Future<bool> setMicState(bool isMicOn) async {
    // The layout should already be set to PipActionsLayout.mic
    // This updates the icon based on the current mic state
    PipAction desiredAction = isMicOn ? PipAction.micOn : PipAction.micOff;
    Map params = {'action': desiredAction.name};
    final bool? setSuccessfully = await _channel.invokeMethod(
      'setMicState',
      params,
    );
    return setSuccessfully ?? false;
  }

  /// Sets the camera state in PIP mode (on/off)
  /// This should be called when the camera state changes from outside the PIP mode
  Future<bool> setCameraState(bool isCameraOn) async {
    // The layout should already be set to PipActionsLayout.camera
    // This updates the icon based on the current camera state
    PipAction desiredAction =
        isCameraOn ? PipAction.cameraOn : PipAction.cameraOff;
    Map params = {'action': desiredAction.name};
    final bool? setSuccessfully = await _channel.invokeMethod(
      'setCameraState',
      params,
    );
    return setSuccessfully ?? false;
  }

  /// Sets both microphone and camera states in PIP mode (on/off)
  /// This should be called when both states need to be updated at once
  /// The layout should be set to PipActionsLayout.micAndCamera
  Future<bool> setMicAndCameraStates(bool isMicOn, bool isCameraOn) async {
    // This uses the dedicated combined method to update both states in one call
    Map params = {
      'micAction': isMicOn ? 'MIC_ON' : 'MIC_OFF',
      'cameraAction': isCameraOn ? 'CAMERA_ON' : 'CAMERA_OFF',
    };

    final bool? setSuccessfully = await _channel.invokeMethod(
      'setMicAndCameraStates',
      params,
    );

    return setSuccessfully ?? false;
  }

  SimplePip({
    this.onPipEntered,
    this.onPipExited,
    this.onPipAction,
    this.onCustomPipAction,
    this.onMicAction,
    this.onCameraAction,
  }) {
    if (onPipEntered != null ||
        onPipExited != null ||
        onPipAction != null ||
        onCustomPipAction != null ||
        onMicAction != null ||
        onCameraAction != null) {
      _channel.setMethodCallHandler((call) async {
        print('Received call from native: ${call.method}');
        switch (call.method) {
          case 'onPipEntered':
            onPipEntered?.call();
          case 'onPipExited':
            onPipExited?.call();
          case 'onPipAction':
            String arg = call.arguments;
            PipAction action = PipAction.values.firstWhere(
              (e) => e.name == arg,
            );
            onPipAction?.call(action);
          case 'onCustomPipAction':
            String arg = call.arguments;
            onCustomPipAction?.call(arg);
          case 'onMicAction':
            String arg = call.arguments;
            onMicAction?.call(arg);
          case 'onCameraAction':
            String arg = call.arguments;
            onCameraAction?.call(arg);
        }
      });
    }
  }
}

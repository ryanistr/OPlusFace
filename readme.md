This guide outlines the Smali modifications required to integrate your custom `FaceAuthBridge` into `services.jar`. This patch bypasses the dependency on a native AOSP Face HAL and redirects AOSP face operations to your Java-based implementation.

### Prerequisites

1. **Compile your Java Sources**: Compile `FaceAuthBridge.java` and related files (`FacePPImpl`, `Camera*`, etc.) into `.smali` files. Ensure the package structure (`ax/nd/faceunlock/...`) is preserved.
2. **Add to Classpath**: Add these compiled Smali files to your `services.jar` (or a separate dex file added to the bootclasspath).

---

### Phase 1: Patch `FaceService.smali`

**Goal:** Force `FaceService` to create a `FaceProvider` even if the system HAL (`IFace`) is missing.

**File:** `service/com/android/server/biometrics/sensors/face/FaceService.smali`
**Method:** `lambda$new$2` (Note: Method name might vary due to obfuscation; look for the method calling `getIFace`).

**Locate this block:**

```smali
.line 862
invoke-static {v1}, Landroid/hardware/face/FaceSensorConfigurations;->getIFace(Ljava/lang/String;)Landroid/hardware/biometrics/face/IFace;
move-result-object v2

.line 863
const/4 v3, 0x0
const-string v4, "FaceService"

if-nez v2, :cond_39
# ... (Error logging) ...
return-object v3  # Returns null, stopping initialization

```

**Replace with:**

```smali
invoke-static {v1}, Landroid/hardware/face/FaceSensorConfigurations;->getIFace(Ljava/lang/String;)Landroid/hardware/biometrics/face/IFace;
move-result-object v2

# PATCH START: Force initialization if HAL is missing
if-nez v2, :cond_39
const-string v2, "FaceService"
const-string v3, "HAL missing, forcing FaceProvider for FaceAuthBridge"
invoke-static {v2, v3}, Landroid/util/Slog;->w(Ljava/lang/String;Ljava/lang/String;)I

# Proceed as if HAL existed. 
# We need to construct FaceProvider with null props (we will handle nulls in FaceProvider)
move-object v9, v3 # v3 is null from original code
goto :skip_get_props

:cond_39
# Original prop fetching
:try_start_39
invoke-interface {v2}, Landroid/hardware/biometrics/face/IFace;->getSensorProps()[Landroid/hardware/biometrics/face/SensorProps;
move-result-object v0
move-object v9, v0
:try_end_52
.catch Landroid/os/RemoteException; {:try_start_39 .. :try_end_52} :catch_5b

:skip_get_props
# PATCH END

```

---

### Phase 2: Patch `FaceProvider.smali`

**Goal:** Initialize `FaceAuthBridge`, prevent crashes from missing HAL, and register a "virtual" sensor.

**File:** `service/com/android/server/biometrics/sensors/face/aidl/FaceProvider.smali`

#### 1. Hook Initialization

**Method:** `<init>` (The main constructor)

**Locate:**

```smali
.line 217
iput-object p9, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mBiometricHandlerProvider:Lcom/android/server/biometrics/BiometricHandlerProvider;

```

**Insert after:**

```smali
# PATCH: Initialize FaceAuthBridge
invoke-static {p1}, Lax/nd/faceunlock/FaceAuthBridge;->init(Landroid/content/Context;)V

```

#### 2. Inject Dummy Sensor

**Method:** `initSensors`

**Locate:**

```smali
.line 239
const/4 v0, 0x0
if-eqz p1, :cond_17

```

**Insert at the very beginning of the method:**

```smali
# PATCH: If props (p2) is null/empty, manually add a sensor
if-eqz p2, :inject_sensor
array-length v0, p2
if-nez v0, :original_logic

:inject_sensor
# Manually add AIDL sensor with ID 0
# Prepare arguments for addAidlSensors
# We need to construct a dummy SensorProps if p2 is null, OR simply copy the logic of addAidlSensors here manually.
# Easiest: Construct a dummy SensorProps object or just call the logic directly.

# Simplified: Directly instantiate Sensor for ID 0
new-instance v1, Lcom/android/server/biometrics/sensors/face/aidl/Sensor;
iget-object v3, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mContext:Landroid/content/Context;
iget-object v4, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mHandler:Landroid/os/Handler;
const/4 v5, 0x0 # Null props (Sensor handles null check or we assume it wont crash if we dont access commonProps)
iget-object v6, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mBiometricContext:Lcom/android/server/biometrics/log/BiometricContext;
move-object v2, p0
move v7, p1 # resetLockoutRequiresChallenge
invoke-direct/range {v1 .. v7}, Lcom/android/server/biometrics/sensors/face/aidl/Sensor;-><init>(Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;Landroid/content/Context;Landroid/os/Handler;Landroid/hardware/biometrics/face/SensorProps;Lcom/android/server/biometrics/log/BiometricContext;Z)V

# Init sensor
iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mLockoutResetDispatcher:Lcom/android/server/biometrics/sensors/LockoutResetDispatcher;
invoke-virtual {v1, v0, p0}, Lcom/android/server/biometrics/sensors/face/aidl/Sensor;->init(Lcom/android/server/biometrics/sensors/LockoutResetDispatcher;Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;)V

# Add to mFaceSensors (ID 0)
iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mFaceSensors:Lcom/android/server/biometrics/sensors/SensorList;
const/4 v2, 0x0 # Sensor ID 0
invoke-static {}, Landroid/app/ActivityManager;->getCurrentUser()I
move-result v3
const/4 v4, 0x0 # Observer
invoke-virtual {v0, v2, v1, v3, v4}, Lcom/android/server/biometrics/sensors/SensorList;->addSensor(ILjava/lang/Object;ILandroid/app/SynchronousUserSwitchObserver;)V

return-void # Exit initSensors early

:original_logic

```

*Note: If `Sensor` constructor crashes with null `SensorProps`, you must construct a dummy `SensorProps` object in Smali. This is complex. Alternatively, ensure `FaceSensorConfigurations` returns a dummy prop in `FaceService` instead of null.*

---

### Phase 3: Patch `FaceAuthenticationClient.smali`

**Goal:** Redirect authentication requests to `FaceAuthBridge`.

**File:** `service/com/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient.smali`

#### 1. Hijack Start

**Method:** `startHalOperation`

**Locate:**

```smali
.line 222
invoke-direct {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient;->doAuthenticate()V

```

**Replace with:**

```smali
# PATCH: Redirect to FaceAuthBridge
invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
move-result-object v0

# Get Sensor ID
invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient;->getSensorId()I
move-result v1

# Get Target User ID
invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient;->getTargetUserId()I
move-result v2

# Get Listener (Converter)
invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient;->getListener()Lcom/android/server/biometrics/sensors/ClientMonitorCallbackConverter;
move-result-object v3

# Call startAuthenticate(int sensorId, int userId, Object receiver)
invoke-virtual {v0, v1, v2, v3}, Lax/nd/faceunlock/FaceAuthBridge;->startAuthenticate(IILjava/lang/Object;)V

```

#### 2. Hijack Stop

**Method:** `stopHalOperation`

**Locate:**

```smali
.line 277
iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceAuthenticationClient;->mCancellationSignal:Landroid/hardware/biometrics/common/ICancellationSignal;
invoke-interface {v0}, Landroid/hardware/biometrics/common/ICancellationSignal;->cancel()V

```

**Replace with:**

```smali
# PATCH: Redirect to FaceAuthBridge
invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
move-result-object v0
invoke-virtual {v0}, Lax/nd/faceunlock/FaceAuthBridge;->stopAuthenticate()V

```

---

### Phase 4: Patch `FaceEnrollClient.smali` (Generic)

**Goal:** Redirect enrollment requests. (File not provided, but apply logic to `FaceEnrollClient.smali`)

#### 1. Hijack Start (`startHalOperation`)

Replace `enroll(...)` call with:

```smali
invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
move-result-object v0
invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceEnrollClient;->getTargetUserId()I
move-result v1
invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceEnrollClient;->getListener()Lcom/android/server/biometrics/sensors/ClientMonitorCallbackConverter;
move-result-object v2
# Assumption: FaceEnrollClient has a field for previewSurface or you pass null if Bridge handles camera internally
# FaceAuthBridge.startEnroll(int userId, Object receiver, Surface surface)
const/4 v3, 0x0 
invoke-virtual {v0, v1, v2, v3}, Lax/nd/faceunlock/FaceAuthBridge;->startEnroll(ILjava/lang/Object;Landroid/view/Surface;)V

```

#### 2. Hijack Stop (`stopHalOperation`)

Replace cancel logic with:

```smali
invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
move-result-object v0
invoke-virtual {v0}, Lax/nd/faceunlock/FaceAuthBridge;->stopEnroll()V

```

---

### Phase 5: Patch `FaceRemovalClient.smali` (Generic)

**Goal:** Redirect removal requests.

In `startHalOperation`, call `FaceAuthBridge.remove(userId, faceId, receiver)`.

* Note: `FaceRemovalClient` usually deletes from HAL. You must call `FaceAuthBridge` to delete from your database.

---

### Phase 6: Patch `FaceGetAuthenticatorIdClient.smali` (Generic)

**Goal:** Return the ID from your bridge.

In `startHalOperation`:

1. Get ID from `FaceAuthBridge.getAuthenticatorId()`.
2. Immediately call `onAuthenticatorIdRetrieved` on the receiver (or `mCallback`).
3. Signal client finished.

---

### Critical Final Steps

1. **Recompile**: Recompile `services.jar`.
2. **Permissions**: Ensure your `FaceAuthBridge` (specifically `CameraService`) has necessary permissions in `AndroidManifest.xml` if it's a separate app, or ensure `system_server` has Camera permissions (it usually does).
3. **SELinux**: If `system_server` cannot access the camera devices directly due to SELinux, you will need to patch `sepolicy` (allow `system_server` access to `video_device` or `camera_device`). If your bridge runs inside `system_server`, this context applies.
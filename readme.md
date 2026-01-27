# OPlusFace Unlock Guide

Follow this guide below for manual patch or follow or follow [this guide](AUTOPATCH/README.md) for auto patch with script by Danda420
This will guide you to patch precompiled services.jar to implement Motorola Face Unlock

Credits me or this repo when use or helped your work. Days were spent figuring out and debugging crashes for this method respect my works.

---

## ⚠️ Prerequisites

- **Face Unlock HALs**  
  Ensure your vendor partition contains the necessary Face Unlock HALs.

- **Compatibility**  
  This method is untested on devices or vendors without existing Face Unlock HALs.
  ```md
  If your device vendor does not have a FaceHAL try implementing AOSP FaceHAL or Fork this repo and adjust accordingly.
  ```

---

## 🛠️ Step 1: Modify `services.jar`

### 1. Initialize the Bridge

Open `services.jar` and locate the `FaceProvider` class. Find the `initSensors` method.

**Action:** Add the following code after the `.registers` and `.param` directives:

```smali
iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mContext:Landroid/content/Context;
invoke-static {v0}, Lax/nd/faceunlock/FaceAuthBridge;->init(Landroid/content/Context;)V
```

**Example Context:**

```smali
.method private initSensors(Z[Landroid/hardware/biometrics/face/SensorProps;)V
    .registers 11
    .param p1, "resetLockoutRequiresChallenge"  # Z
    .param p2, "props"  # [Landroid/hardware/biometrics/face/SensorProps;

    .line 239

    iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mContext:Landroid/content/Context;
    invoke-static {v0}, Lax/nd/faceunlock/FaceAuthBridge;->init(Landroid/content/Context;)V
    # rest of the method...
```

---

### 2. Replace Methods

Search for the following methods in `FaceProvider` class and replace the **entire method** with the patches below.

#### Patched Methods

```smali
.method public cancelAuthentication(ILandroid/os/IBinder;J)V
    .registers 8
    .param p1, "sensorId"  # I
    .param p2, "token"  # Landroid/os/IBinder;
    .param p3, "requestId"  # J
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_10
    invoke-virtual {v0}, Lax/nd/faceunlock/FaceAuthBridge;->stopAuthenticate()V
    :cond_10
    return-void
.end method
```
```smali
.method public cancelEnrollment(ILandroid/os/IBinder;J)V
    .registers 8
    .param p1, "sensorId"  # I
    .param p2, "token"  # Landroid/os/IBinder;
    .param p3, "requestId"  # J
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_10
    invoke-virtual {v0}, Lax/nd/faceunlock/FaceAuthBridge;->stopEnroll()V
    :cond_10
    return-void
.end method
```
```smali
.method public getAuthenticatorId(II)J
    .registers 7
    .param p1, "sensorId"  # I
    .param p2, "userId"  # I
    
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_12
    invoke-virtual {v0}, Lax/nd/faceunlock/FaceAuthBridge;->getAuthenticatorId()J
    move-result-wide v0
    return-wide v0
    :cond_12
    const-wide/16 v0, 0x0
    return-wide v0
.end method
```
```smali
.method public getEnrolledFaces(II)Ljava/util/List;
    .registers 7
    .param p1, "sensorId"  # I
    .param p2, "userId"  # I
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(II)",
            "Ljava/util/List<",
            "Landroid/hardware/face/Face;",
            ">;"
        }
    .end annotation
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_12
    invoke-virtual {v0, p1, p2}, Lax/nd/faceunlock/FaceAuthBridge;->getEnrolledFaces(II)Ljava/util/List;
    move-result-object v0
    return-object v0
    :cond_12
    new-instance v0, Ljava/util/ArrayList;
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
    return-object v0
.end method
```
```smali
.method public isHardwareDetected(I)Z
    .registers 4
    .param p1, "sensorId"  # I

    const/4 v0, 0x1

    return v0
.end method
```
```smali
.method public scheduleAuthenticate(Landroid/os/IBinder;JILcom/android/server/biometrics/sensors/ClientMonitorCallbackConverter;Landroid/hardware/face/FaceAuthenticateOptions;ZIZ)J
    .registers 20
    .param p1, "token"  # Landroid/os/IBinder;
    .param p2, "operationId"  # J
    .param p4, "cookie"  # I
    .param p5, "callback"  # Lcom/android/server/biometrics/sensors/ClientMonitorCallbackConverter;
    .param p6, "options"  # Landroid/hardware/face/FaceAuthenticateOptions;
    .param p7, "restricted"  # Z
    .param p8, "statsClient"  # I
    .param p9, "allowBackgroundAuthentication"  # Z
    
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_18
    invoke-virtual/range {p6 .. p6}, Landroid/hardware/face/FaceAuthenticateOptions;->getUserId()I
    move-result v1
    invoke-virtual/range {p6 .. p6}, Landroid/hardware/face/FaceAuthenticateOptions;->getSensorId()I
    move-result v2
    invoke-virtual {v0, v2, v1, p5}, Lax/nd/faceunlock/FaceAuthBridge;->startAuthenticate(IILjava/lang/Object;)V
    :cond_18
    const-wide/16 v0, 0x1
    return-wide v0
.end method
```
```smali
.method public scheduleEnroll(ILandroid/os/IBinder;[BILandroid/hardware/face/IFaceServiceReceiver;Ljava/lang/String;[ILandroid/view/Surface;ZLandroid/hardware/face/FaceEnrollOptions;)J
    .registers 15
    .param p1, "sensorId"  # I
    .param p2, "token"  # Landroid/os/IBinder;
    .param p3, "hat"  # [B
    .param p4, "userId"  # I
    .param p5, "receiver"  # Landroid/hardware/face/IFaceServiceReceiver;
    .param p6, "opPackageName"  # Ljava/lang/String;
    .param p7, "disabledFeatures"  # [I
    .param p8, "previewSurface"  # Landroid/view/Surface;
    .param p9, "debugConsent"  # Z
    .param p10, "options"  # Landroid/hardware/face/FaceEnrollOptions;
   
    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    invoke-virtual {v0, p4, p5, p8}, Lax/nd/faceunlock/FaceAuthBridge;->startEnroll(ILjava/lang/Object;Landroid/view/Surface;)V
    const-wide/16 v0, 0x1
    return-wide v0
.end method
```
```smali
.method public scheduleGenerateChallenge(IILandroid/os/IBinder;Landroid/hardware/face/IFaceServiceReceiver;Ljava/lang/String;)V
    .registers 9
    .param p1, "sensorId"  # I
    .param p2, "userId"  # I
    .param p3, "token"  # Landroid/os/IBinder;
    .param p4, "receiver"  # Landroid/hardware/face/IFaceServiceReceiver;
    .param p5, "opPackageName"  # Ljava/lang/String;

    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_10
    invoke-virtual {v0, p1, p2, p4}, Lax/nd/faceunlock/FaceAuthBridge;->generateChallenge(IILjava/lang/Object;)V
    :cond_10
    return-void
.end method
```
```smali
.method public scheduleRemove(ILandroid/os/IBinder;IILandroid/hardware/face/IFaceServiceReceiver;Ljava/lang/String;)V
    .registers 10
    .param p1, "sensorId"  # I
    .param p2, "token"  # Landroid/os/IBinder;
    .param p3, "faceId"  # I
    .param p4, "userId"  # I
    .param p5, "receiver"  # Landroid/hardware/face/IFaceServiceReceiver;
    .param p6, "opPackageName"  # Ljava/lang/String;

    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_10
    invoke-virtual {v0, p4, p3, p5}, Lax/nd/faceunlock/FaceAuthBridge;->remove(IILjava/lang/Object;)V
    :cond_10
    return-void
.end method
```
```smali
.method public scheduleRevokeChallenge(IILandroid/os/IBinder;Ljava/lang/String;J)V
    .registers 10
    .param p1, "sensorId"  # I
    .param p2, "userId"  # I
    .param p3, "token"  # Landroid/os/IBinder;
    .param p4, "opPackageName"  # Ljava/lang/String;
    .param p5, "challenge"  # J

    invoke-static {}, Lax/nd/faceunlock/FaceAuthBridge;->getInstance()Lax/nd/faceunlock/FaceAuthBridge;
    move-result-object v0
    if-eqz v0, :cond_11
    const/4 v2, 0x0
    invoke-virtual {v0, p1, p2, v2}, Lax/nd/faceunlock/FaceAuthBridge;->revokeChallenge(IILjava/lang/Object;)V
    :cond_11
    return-void
.end method
```

### 3. Add Classes and Repack

* Add the `classes.dex` of the implementation to `services.jar`.
* **Rename Strategy:** If your last dex is `classes4.dex`, name the new one `classes5.dex` (increment accordingly).
* **Repack:** Repack `services.jar`. **DO NOT SIGN IT AND KEEP ORIGINAL SIGNATURE.**

---

## 📱 Step 2: Modify `SystemUI`

This modification ensures the lockscreen updates without requiring a reboot.

1. Open `SystemUI.apk`
2. Navigate to `com.android.systemui.biometrics.AuthController`
3. Find the `isFaceAuthEnrolled(I)Z` method
4. Replace the **entire method** with the following:

```smali
.method public isFaceAuthEnrolled(I)Z
    .registers 4

    const-string v0, "persist.sys.oplus.isFaceEnrolled"
    invoke-static {v0}, Landroid/os/SystemProperties;->get(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    const-string v1, "1"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0

    return v0
.end method
```

Then finally just copy the whole system folder into your system partition for the libs, init.rc, and face models
---
## 📜 Credits and Contributions
- [**UniversalAuth**](https://github.com/null-dev/UniversalAuth)
- [**ryanistr** ](https://github.com/ryanistr)
- **Motorola**
- [**Danda420**](https://github.com/Danda420)
---
# Enjoy.

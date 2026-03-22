# OPlusFace Unlock Guide

Follow this guide below for manual patch or follow or follow [this guide](AUTOPATCH/README.md) for auto patch with script by Danda420
This will guide you to patch precompiled services.jar to implement Motorola Face Unlock to Oplus Services

Credits me or this repo when use or helped your work. Days were spent figuring out and debugging crashes for this method respect my works.

---

## 🛠️ Step 1: Modify `services.jar`

### 1. Initialize the Bridge

Open `services.jar` and locate the `com.android.server.biometrics.sensors.face.FaceService` class. Find the `getDeclaredInstances` method and replace with this method.

```
.method public static getDeclaredInstances()[Ljava/lang/String;
    .registers 3

    const/4 v0, 0x1

    new-array v0, v0, [Ljava/lang/String;

    const-string v1, "default"

    const/4 v2, 0x0

    aput-object v1, v0, v2

    return-object v0
.end method
```
```
.method private synthetic lambda$new$2(Ljava/lang/String;)Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;
    .registers 16
    .param p1, "name"  # Ljava/lang/String;

    .line 861
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    sget-object v1, Landroid/hardware/biometrics/face/IFace;->DESCRIPTOR:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, "/"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 862
    .local v1, "fqName":Ljava/lang/String;
    invoke-static {v1}, Landroid/hardware/face/FaceSensorConfigurations;->getIFace(Ljava/lang/String;)Landroid/hardware/biometrics/face/IFace;

    move-result-object v2

    .line 863
    .local v2, "face":Landroid/hardware/biometrics/face/IFace;
    const/4 v3, 0x0

    const-string v4, "FaceService"

    if-nez v2, :cond_4e

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "Unable to get declared service, using fake props: "

    invoke-virtual {v0, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v4, v0}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;)I

    new-instance v13, Landroid/hardware/biometrics/face/SensorProps;

    invoke-direct {v13}, Landroid/hardware/biometrics/face/SensorProps;-><init>()V

    new-instance v0, Landroid/hardware/biometrics/common/CommonProps;

    invoke-direct {v0}, Landroid/hardware/biometrics/common/CommonProps;-><init>()V

    const/4 v11, 0x1

    iput v11, v0, Landroid/hardware/biometrics/common/CommonProps;->sensorId:I

    iput-object v0, v13, Landroid/hardware/biometrics/face/SensorProps;->commonProps:Landroid/hardware/biometrics/common/CommonProps;

    const/4 v10, 0x1

    new-array v9, v10, [Landroid/hardware/biometrics/face/SensorProps;

    const/4 v10, 0x0

    aput-object v13, v9, v10

    goto :goto_6c

    :cond_4e
    :try_start_4e
    invoke-interface {v2}, Landroid/hardware/biometrics/face/IFace;->getSensorProps()[Landroid/hardware/biometrics/face/SensorProps;

    move-result-object v0

    move-object v9, v0
    :try_end_53
    .catch Landroid/os/RemoteException; {:try_start_4e .. :try_end_53} :catch_54

    goto :goto_6c

    :catch_54
    move-exception v0

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "Remote exception: "

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;)I

    return-object v3

    .local v9, "props":[Landroid/hardware/biometrics/face/SensorProps;
    :goto_6c
    :try_start_6c
    new-instance v5, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;

    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/FaceService;->getContext()Landroid/content/Context;

    move-result-object v6
    
    iget-object v7, p0, Lcom/android/server/biometrics/sensors/face/FaceService;->mBiometricStateCallback:Lcom/android/server/biometrics/sensors/BiometricStateCallback;
    
    iget-object v8, p0, Lcom/android/server/biometrics/sensors/face/FaceService;->mAuthenticationStateListeners:Lcom/android/server/biometrics/sensors/AuthenticationStateListeners;

    iget-object v11, p0, Lcom/android/server/biometrics/sensors/face/FaceService;->mLockoutResetDispatcher:Lcom/android/server/biometrics/sensors/LockoutResetDispatcher;

    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/FaceService;->getContext()Landroid/content/Context;

    move-result-object v0

    invoke-static {v0}, Lcom/android/server/biometrics/log/BiometricContext;->getInstance(Landroid/content/Context;)Lcom/android/server/biometrics/log/BiometricContext;

    move-result-object v12

    const/4 v13, 0x0

    move-object v10, p1

    invoke-direct/range {v5 .. v13}, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;-><init>(Landroid/content/Context;Lcom/android/server/biometrics/sensors/BiometricStateCallback;Lcom/android/server/biometrics/sensors/AuthenticationStateListeners;[Landroid/hardware/biometrics/face/SensorProps;Ljava/lang/String;Lcom/android/server/biometrics/sensors/LockoutResetDispatcher;Lcom/android/server/biometrics/log/BiometricContext;Z)V
    :try_end_85
    .catch Landroid/os/RemoteException; {:try_start_6c .. :try_end_85} :catch_86

    return-object v5

    :catch_86
    move-exception v0

    const-string v5, "Remote exception during init"

    invoke-static {v4, v5}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;)I

    return-object v3
.end method
```

Then locate the `FaceProvider` class. Find the `initSensors` method.

Patch the whole method with this
```smali
.method private initSensors(Z[Landroid/hardware/biometrics/face/SensorProps;)V
    .registers 12

    iget-object v0, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mContext:Landroid/content/Context;

    invoke-static {v0}, Lax/nd/faceunlock/FaceAuthBridge;->init(Landroid/content/Context;)V

    new-instance v0, Landroid/hardware/biometrics/face/SensorProps;

    invoke-direct {v0}, Landroid/hardware/biometrics/face/SensorProps;-><init>()V

    new-instance v1, Landroid/hardware/biometrics/common/CommonProps;

    invoke-direct {v1}, Landroid/hardware/biometrics/common/CommonProps;-><init>()V

    const/4 v2, 0x1

    iput v2, v1, Landroid/hardware/biometrics/common/CommonProps;->sensorId:I

    iput-object v1, v0, Landroid/hardware/biometrics/face/SensorProps;->commonProps:Landroid/hardware/biometrics/common/CommonProps;

    const/4 v1, 0x0

    invoke-direct {p0, v0, v1}, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->addAidlSensors(Landroid/hardware/biometrics/face/SensorProps;Z)V

    invoke-static {}, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->getExtImpl()Lcom/android/server/biometrics/sensors/face/aidl/IFaceProviderExt;

    move-result-object v2

    move-object v3, p0

    iget-object v4, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mContext:Landroid/content/Context;

    const/4 v1, 0x1

    new-array v5, v1, [Landroid/hardware/biometrics/face/SensorProps;

    const/4 v1, 0x0

    aput-object v0, v5, v1

    iget-object v6, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mHalInstanceName:Ljava/lang/String;

    iget-object v7, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;->mHandler:Landroid/os/Handler;

    invoke-interface/range {v2 .. v7}, Lcom/android/server/biometrics/sensors/face/aidl/IFaceProviderExt;->init(Lcom/android/server/biometrics/sensors/face/aidl/FaceProvider;Landroid/content/Context;[Landroid/hardware/biometrics/face/SensorProps;Ljava/lang/String;Landroid/os/Handler;)V

    return-void
.end method
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
### 3. Extra fix (additional)
Note: Only apply this specific patch if you're experiencing log spam issues caused by an NPE specifically around FaceStartUserClient.

Find this specific method inside `FaceStartUserClient` and replace `startHalOperation()V` with this one
```smali
.method protected startHalOperation()V
    .registers 8

    .line 61
    const-string v0, "FaceStartUserClient"

    :try_start_2
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getFreshDaemon()Ljava/lang/Object;

    move-result-object v1

    if-eqz v1, :cond_null

    check-cast v1, Landroid/hardware/biometrics/face/IFace;

    .line 62
    .local v1, "hal":Landroid/hardware/biometrics/face/IFace;
    invoke-interface {v1}, Landroid/hardware/biometrics/face/IFace;->getInterfaceVersion()I

    move-result v2

    .line 63
    .local v2, "version":I
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getSensorId()I

    move-result v3

    .line 64
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getTargetUserId()I

    move-result v4

    iget-object v5, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->mSessionCallback:Landroid/hardware/biometrics/face/ISessionCallback;

    .line 63
    invoke-interface {v1, v3, v4, v5}, Landroid/hardware/biometrics/face/IFace;->createSession(IILandroid/hardware/biometrics/face/ISessionCallback;)Landroid/hardware/biometrics/face/ISession;

    move-result-object v3

    .line 68
    .local v3, "newSession":Landroid/hardware/biometrics/face/ISession;
    if-eqz v3, :cond_24

    .line 69
    invoke-interface {v3}, Landroid/hardware/biometrics/face/ISession;->asBinder()Landroid/os/IBinder;

    move-result-object v4

    invoke-static {v4}, Landroid/os/Binder;->allowBlocking(Landroid/os/IBinder;)Landroid/os/IBinder;

    goto :goto_2a

    .line 71
    :cond_24
    const-string/jumbo v4, "newSession is null"

    invoke-static {v0, v4}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 74
    :goto_2a
    iget-object v4, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->mUserStartedCallback:Lcom/android/server/biometrics/sensors/StartUserClient$UserStartedCallback;

    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getTargetUserId()I

    move-result v5

    invoke-interface {v4, v5, v3, v2}, Lcom/android/server/biometrics/sensors/StartUserClient$UserStartedCallback;->onUserStarted(ILjava/lang/Object;I)V

    .line 75
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getCallback()Lcom/android/server/biometrics/sensors/ClientMonitorCallback;

    move-result-object v4

    const/4 v5, 0x1

    invoke-interface {v4, p0, v5}, Lcom/android/server/biometrics/sensors/ClientMonitorCallback;->onClientFinished(Lcom/android/server/biometrics/sensors/BaseClientMonitor;Z)V
    :try_end_3b
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_3b} :catch_3c

    .line 83
    .end local v1  # "hal":Landroid/hardware/biometrics/face/IFace;
    .end local v2  # "version":I
    .end local v3  # "newSession":Landroid/hardware/biometrics/face/ISession;
    goto :goto_4a

    :cond_null
    iget-object v1, p0, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->mUserStartedCallback:Lcom/android/server/biometrics/sensors/StartUserClient$UserStartedCallback;
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getTargetUserId()I
    move-result v2
    const/4 v3, 0x0
    const/4 v4, 0x0
    invoke-interface {v1, v2, v3, v4}, Lcom/android/server/biometrics/sensors/StartUserClient$UserStartedCallback;->onUserStarted(ILjava/lang/Object;I)V

    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getCallback()Lcom/android/server/biometrics/sensors/ClientMonitorCallback;
    move-result-object v1
    const/4 v2, 0x1
    invoke-interface {v1, p0, v2}, Lcom/android/server/biometrics/sensors/ClientMonitorCallback;->onClientFinished(Lcom/android/server/biometrics/sensors/BaseClientMonitor;Z)V
    goto :goto_4a

    .line 79
    :catch_3c
    move-exception v1

    .line 81
    .local v1, "e":Ljava/lang/Exception;
    const-string v2, "Remote exception"

    invoke-static {v0, v2, v1}, Landroid/util/Slog;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 82
    invoke-virtual {p0}, Lcom/android/server/biometrics/sensors/face/aidl/FaceStartUserClient;->getCallback()Lcom/android/server/biometrics/sensors/ClientMonitorCallback;

    move-result-object v0

    const/4 v2, 0x0

    invoke-interface {v0, p0, v2}, Lcom/android/server/biometrics/sensors/ClientMonitorCallback;->onClientFinished(Lcom/android/server/biometrics/sensors/BaseClientMonitor;Z)V

    .line 84
    .end local v1  # "e":Ljava/lang/Exception;
    :goto_4a
    return-void
.end method
```

### 4. Add Classes and Repack

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
- [**ryanistr** ](https://github.com/ryanistr)
- [**Danda420**](https://github.com/Danda420)
- [**LazyBones**](https://www.coolapk.com/)
- **Motorola**
- [**UniversalAuth**](https://github.com/null-dev/UniversalAuth)
---
## Enjoy.

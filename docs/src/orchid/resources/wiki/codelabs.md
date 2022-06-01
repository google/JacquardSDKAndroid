You have now successfully integrated the Jacquard SDK and learned how to find and connect to Jacquard 
tags. There's more to the SDK which you can explore in a few different ways.

## Table of contents
1. [Sending commands](#section1)
1. [Observing Notifications](#section2)
1. [Updating firmware](#section3)
1. [Motion Capture](#section4)


## <a name="section1"></a>1. Sending commands

Now we'll explore sending commands to the tag, in this case
`RenameTagCommand`, but commands are documented fully in the
api documentation.

When you have `ConnectedJacquardTag` available, you can enqueue commands which will be sent to 
the tag in sequential manner as below -

```java
public Signal<String> renameTag(String tagName) {
  return connectedJacquardTag
          .enqueue(new RenameTagCommand(tagName)));
}
```

Note - Your app should listen for command success as well as failures both as below -
```java
updateTagName("NewTagName").observe(new Observer<String>() {
  @Override
  public void onNext(@NonNull String s) {
    // SUCCESS
  }

  @Override
  public void onError(@NonNull Throwable t) {
    // FAILURE
  }

  @Override
  public void onComplete() {
  }
});
```
 
`ConnectedJacquardTag.enqueue(Request command, int retries)` this api will give you ability to retry 
sending same request multiple times if it fails in prior attempt.

## <a name="section2"></a>2. Observing Notifications

Commands are initiated by the app and sent to the tag (possibly with a
response). Notifications on the other hand originate from the tag at
any time. We can ask to be notified any time a notification of
interest occurs. 

To get the tag battery notifiations -
```java
private Signal<BatteryStatus> getBatteryNotifications(ConnectedJacquardTag tag) {
  return tag.subscribe(new BatteryStatusNotificationSubscription());
}
```

To get tag attach-detach notifications - 
```java
private Signal<GearState> getGearNotifications(ConnectedJacquardTag tag) {
  return tag.getConnectedGearSignal();
}
```

Once you have `ConnectedJacquardTag`, you can insert tag into  Jacquard gear and 
observe for the gesture events as below -

```java
private Signal<Gesture> getGestures(ConnectedJacquardTag tag) {
  return tag.subscribe(new GestureNotificationSubscription());
}
```

You have now successfully integrated the Jacquard SDK and learned how
to connect, send commands and observe notifications. There's more to
the SDK which you can see demonstrated in the [sample
app](https://github.com/google/JacquardSDKAndroid) and read about in the
SDK documentation (see the table of contents on the left side of this
page).

## <a name="section3"></a>3. Updating firmware

Before using any Jacquard SDK core api, it is always recommended to ensure that you have the latest 
available firmware. When your tag is paired, next ideal step should be to check for firmware update
by calling Jacquard firmware apis which are backed up by Jacquard cloud. 
Updating firmware is a 3 step process - 

#### 1. Check if update available
This step would need internet connectivity as it calls Jacquard cloud api to see if any firmware 
updates are available. If update are available for your current firmware either MANDATORY or 
OPTIONAL, sdk will start downloading firmware binary implicitly. This step will be completed when 
firmware binary is downloaded successfully. You do not need to worry about downloaded binary, sdk 
will use it when you try to apply the updates. 
#### 2. Apply updates
Jacquard sdk will transfer downloaded firmware binary to the tag using bluetooth high priority 
connection. This step might take a while depending on the size of downloaded firmware binary.
#### 3. Execute updates
In this step, Jacquard sdk will install the firmware binary on the tag. If you have send 
`autoExecute=true` in step 2, you can skip this step. For tag and interposer firmware update, this
 is a mandatory step however in case of loadable module, this step is not required.


Let's start with the implementation. It's pretty much straight forward. 

- #### Update tag / interposer firmware

##### 1. Check if update available - 

To check if update is available for both tag and interposer, insert your tag into your 
gear and use below piece of code -  

```java
public Signal<List<DFUInfo>> checkFirmware(boolean forceUpdate) {
  return connectedJacquardTag.dfuManager().checkFirmware(connectedJacquardTag.getComponents(), forceUpdate);
}
```

This code will return list of <code>DFUInfo</code> for each input component. Check the value for 
`DFUInfo.dfuStatus()` to know what type of firmware update is available. It could be either 
`MANDATORY` or `OPTIONAL` or `NOT_AVAILABLE`. You can set `forceUpdate` as `true` to always hit the 
cloud ignoring the local cache. To get firmare updates from local cache, set `forceUpdate` as `false`.

##### 2. Apply updates -

Here, Jacquard sdk will transfer downloaded firmware binary from mobile to the tag. You need to use 
list of `DFUInfo` which you have received in previous step.

```java
public Signal<FirmwareUpdateState> applyUpdates(List<DFUInfo> dfuInfos, boolean autoExecute){
  return connectedJacquardTag.dfuManager().applyUpdates(dfuInfos, autoExecute);
}
```

`FirmwareUpdateState` returned will give you the exact status of the operation. When Jacquard sdk 
starts transferring firmware binary to the tag i.e. state is `TRANSFER_PROGRESS`, you can track the 
transfer progress using `FirmwareUpdateState#transferProgress`. You can use `autoExecute=true` to 
install the updates automatically otherwise you need to call below `executeUpdates()` api explicitly. 
`FirwareUpdateState.error()` will give you the error occurred in this step.

##### 3. Execute updates -

This is the last step in firmware update in which Jacquard sdk instructs tag to install the firmware 
binary transferred in previous step. This step is not required if you are sending `autoExecute=true`
in `applyUpdates()` api. If you are updating tag firmware, tag will reboot during this step. 

```java
public Signal<FirmwareUpdateState> executeFirmware(){
  return connectedJacquardTag.dfuManager().executeUpdates();
}
```

`FirmwareUpdateState` returned by this api can either be `EXECUTING` or `COMPLETED`.


- #### Update Loadable Module

##### 1. Check if update available -

You can execute `ListModulesCommand` command to fetch all loadable modules present on the tag. This 
command will give you `List<Module>`. Similar to tag & interposer firmware update, first step here 
is also to check if update is available using below api - 

```java
public Signal<DFUInfo> checkModuleUpdate(Module module) {
  return connectedJacquardTag.dfuManager().checkModuleUpdate(module);
}
```

This code will return `DFUInfo` for `Module` used as input. Check the value for 
`DFUInfo.dfuStatus()` to know what type of update is available. It could be either 
`MANDATORY` or `OPTIONAL` or `NOT_AVAILABLE`. Please note that `checkModuleUpdate` will always fetch
updates from cloud bypassing the local cache.


##### 2. Apply module updates -

This step is pretty much similar to step 2 of tag & interposer updates. Use below piece of code to 
start sending firmware binary to the tag.

```java
public Signal<FirmwareUpdateState> applyModuleUpdate(DFUInfo dfuInfo){
  return connectedJacquardTag.dfuManager().applyModuleUpdate(dfuInfo);
}
```

`FirmwareUpdateState` returned will give you the exact status of the operation. When Jacquard sdk 
starts transferring firmware binary to the tag i.e. state is `TRANSFER_PROGRESS`, you can track the 
transfer progress using `FirmwareUpdateState#transferProgress`. Keep watching 
`FirwareUpdateState.error()` for any errors occurred in this step.

##### 3. Execute module updates -

There is no need to install/execute lodable module firmware as such. Once loadble module binary is 
transferred to the tag, you are good to play around with it.


- #### Interrupt firmware update process

Call below api if you want to cancel firmware update process at any point. 

```java
public void stop() {
  connectedJacquardTag.dfuManager().stop();
}
```


## <a name="section4"></a>4. Motion Capture
With this feature, you can record, view and download your tag's Inertial Measurement Unit
(IMU) motion sensor data: accelerometer (x, y, z), and gyroscope (yaw, pitch, roll) data. This section
will guide you to record, download, parse, erase Imu session. If you want the tag should collect Imu 
samples, loadable module must be activated, but it will disable Wake on Motion(WoM) on the tag and 
you can easily notice that tag battery is draining quickly.

#### 1. Instantiate
You can create instance of `ImuModule` using `ConnectedJacquardTag` as below - 

```java
    new ImuModule(connectedJacquardTag);
```
#### 2. Initialize
This is a mandatory step before you start recording IMU samples. There are multiple steps involved 
during initialize process. Initialize process will be quicker when Data Collection Loadable Module
 (DCLM) is present on the tag and its activated. If DCLM is not present on the tag, `ImuModule` will 
 perform device firmware update to download DCLM binary from cloud and send it to the tag. Once DFU 
 is successful, `ImuModule` will activate the DCLM to finish initialize process.
 
 ```java
     Signal<InitState> initialize();
```

This api gives you every step involved in initialize process by sending `InitState`. Below could be 
multiple initialize states \
    1. **INIT -** Sdk will fetch LM modules from ujt and checks if present and activated. \
    2. **CHECK_FOR_UPDATES -** Repeated. If LM is not present, sdk will check if Dfu is needed for 
    ujt & DC LM.  \
    3. **TAG_DFU -** Sdk is performing ujt firmware update. This is Imu Init state but it will have 
    `FirmwareUpdateState` bundled inside so that the app could know exact progress for ujt dfu. 
    `AutoExecute` will be `true` for tag dfu. \
    4. **MODULE_DFU -** Sdk will be performing dfu for DC LM to the ujt. This is Imu Init state but 
    it will have `FirmwareUpdateState` bundled inside so that the app could know exact progress for 
    dc lm dfu. \
    5. **ACTIVATE -** At this state, sdk will try to activate DC LM. \
    6. **INITIALIZED -** ImuModule is now ready to use.
    
#### 3. Start & stop Imu session
You can start collecting Imu samples by calling ```Signal<String> startImuSession()``` api. This api 
returns unique Imu session id which is nothing but unix timestamp in seconds. Call ```Signal<Boolean> 
stopImuSession()``` to end current Imu session. Important point to remember here is that - you should
not attach/detach the tag from gear during Imu session. Which means - while starting a new Imu session 
whichever is the gear state, either attached or detached, must be same till you call `stopImuSession()`
api.

#### 4. Fetch Imu session list

```java
public Signal<List<ImuSessionInfo>> getImuSessionsList() {
  return imuModule.getImuSessionsList();
}
```
Use above code to fetch list of Imu sessions present on the tag. It will return `List<ImuSessionInfo>`.
If there active Imu session on the tag, you can't call this api. 

#### 5. Download Imu session
To download recorded Imu session data from the tag to your mobile, use below code  - 

```java
public Signal<Pair<Integer, File>> downloadImuData(ImuSessionInfo info) { 
    return imuModule.downloadImuData(info);
}
```
OR
```java
public Signal<Pair<Integer, File>> downloadImuData(String imuSessionId) { 
    return imuModule.downloadImuData(imuSessionId);
}
```

Both apis gives you download progress and file handle where Imu session data will be saved. As tag has 
limited storage, to free up the space, it's highly recommended to erase Imu session from the tag 
after it's downloaded to the mobile device. If there active Imu session on the tag, you can't call 
this api. To cancel in-progress downloading, you can simply `unsubscribe()` from observable returned
 by above api.

#### 6. Erase Imu session(s)
You can either choose to delete a specific Imu session or all sessions from the tag. There are 
 overloaded apis available -

```java
public Signal<Boolean> erase(@NonNull ImuSessionInfo session) {
  return imuModule.erase(session);
}
```
OR
```java
public Signal<Boolean> erase(@NonNull String sessionId) {
  return imuModule.erase(sessionId);
}
```
OR
```java
public Signal<Boolean> eraseAll() {
  return imuModule.eraseAll();
}
```

#### 7. Parse Imu session data
Once you download the Imu session to the mobile device, now its time to parse that session data to 
view Imu samples. You can input downloaded session file to below api to parse Imu session. 

```java
public Signal<ImuSessionData> parseImuData(@NonNull final String path) {
  return ImuModule.parseImuData(path);
}
```

#### 8. Get Data Collection status
To know the current data collection status if the tag, use below code - 
```java
  public Signal<DataCollectionStatus> getDataCollectionStatus() {
    return imuModule.getDataCollectionStatus();
  }
```
If ```DataCollectionStatus``` is ```DATA_COLLECTION_LOGGING``` means there is active Imu session 
on the tag and tag is recording Imu samples.

#### 9. Deactivate Data Collection Loadable Module
When there is no active Imu session, you must deactivate/unload data collection loadable module to 
save the tag battery. When lodable module is disabled, Wake on Motion(WoM) will be activated on the 
tag and if idle for 10 mins, tag will go to sleep to preserve the battery.

```java
  public Signal<Boolean> unloadModule() {
    return imuModule.unloadModule();
  }
```
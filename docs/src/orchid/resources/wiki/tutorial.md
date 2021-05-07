In this tutorial you will learn how to integrate the Jacquard SDK into
an Android app, scan and connect to a tag, send commands and respond to
notifications.

You will need:
1. Some Jacquard gear TODO: Link to getting started section/page about
   obtaining gear.
2. An Android device (since the Jacquard SDK uses Bluetooth to connect to
   your tag the simulator won't work).

## Table of contents
1. [Prepare your Jacquard Tag](#section1)
1. [Create a new Android project](#section2)
1. [Integrate Jacquard SDK](#section3)
1. [Add the scanning table view](#section4)
1. [Displaying the advertised tags and connecting the TableView Delegate](#section5)
1. [Displaying already connected tags](#section6)
1. [Connecting to tags](#section7)
1. [Sending commands](#section8)
1. [Observing Notifications](#section9)


## <a name="section1"></a>1. Prepare your Jacquard Tag

Before continuing, update the Jacquard firmware as per the [instructions](/wiki/firmware_update).

To ensure the smoothest path through this tutorial, go to Android
Bluetooth settings, find the entry for your Jacquard Tag and choose
"Forget this device".

## <a name="section2"></a>2. Create a new Android project

In Android Studio, create a new Android project.

> File menu -> New -> New Project...

![Android Studio new project dialog](../../assets/media/tutorial/create_new_project.png)

## <a name="section3"></a>3. Integrate Jacquard SDK

It's easy to integrate the Jacquard SDK using Maven.

You need to include Jacquard in the `dependencies` section of your gradle file:

```java
implementation "com.google.jacquard:jacquard-sdk:0.1.0"
```

## <a name="section4"></a>4. Add the scanning table view

The app you will make has two screens. The first is a tableview
listing any nearby advertising tags. The second is a screen with a few
simple labels and buttons which you will connect to Jacquard
functions.

First lets set up the Scanning activity.

1. Select `File > New > Activity > Empty Activity`.
   We'll make this the main activity.
1. Add a RecyclerView by going to `activity_main.xml`.
1. Your activity should look something like this:
   ![Main activity layout](../../assets/media/tutorial/activity_main.png)

You're now ready to start coding!

```java
jacquardManager.startScanning()
               .distinct()
               .scan(new ArrayList<AdvertisedJacquardTag>(), 
                      (tagList, tag) -> {
                        tagList.add(tag);
                        return tagList;
                      })
               .onNext(tagList -> // Notify adapter to update UI);  
```

### Put the Jacquard Tag into advertising mode

Press and hold the button on your tag for 3 or 4 seconds. The LED on
the tag will start pulsing, and you should see a log entry in Xcode
`Found advertising tag 01ZK` (your tag's identifier will be
different).

## <a name="section5"></a>5. Displaying the advertised tags and populating the RecyclerView

Here we are using a standard Android approach to display the found tag in
a cell and respond to a tapped cell. You may do this yourself.

You should be able to build and run and see advertising tags in the
table view (you will need to press and hold the tag button again -
Jacquard tags stop advertising after 60 seconds).

## <a name="section6"></a>6. Displaying already connected tag

When the tag is paired and connected to Android, it will no longer advertise.
Instead, the list of tags already known and connected to Android can be retrieved.
To achieve this, when user tries to pair to the ujt through sample app, 
tag info should be saved to the shared preferences. Its totally upto you, 
how you want to implement it.

```java
private void savePairedDevices(KnownTag tag) {
  Set<KnownTag> pairedTags = new HashSet<>(preferences.getPairedTags());
  pairedTags.add(tag);
  preferences.putPairedDevices(pairedTags);
}
```

## <a name="section7"></a>7. Connecting to tags

Connecting to tags is an important step that is fully documented in
[Connecting to Tags](Connecting%20to%20Tags.html).

### Connecting to the tag

When user tries to pair with a selected tag, you can use either of the below apis -

```java
jacquardManager.connect(address);
```
OR
```java
jacquardManager.connect(advertisedJacquardTag);
```
Both the apis will emit `ConnectionState`. You should monitor `ConnectionState.getType()` to know
exact progress of pairing. Your application can get `ConnectedJacquardTag` as per below sample code - 

```java
public Signal<ConnectedJacquardTag> getConnectedTagSignal() {       
  return jacquardManager.connect(address)
          .filter(state -> state.isType(CONNECTED))
          .map(state -> state.connected());
}
```


Congratulations, your app will now show an advertising tag, connect to
it. Try it now (and don't forget to press the tag's button for four seconds 
to restart pairing mode).

## <a name="section8"></a>8. Sending commands

Now we'll explore sending commands to the tag, in this case
`RenameTagCommand`, but commands are documented fully in the
[Commands](Commands.html) documentation.

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

## <a name="section9"></a>9. Observing Notifications

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

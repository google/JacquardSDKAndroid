In this tutorial you will learn how to integrate the Jacquard SDK into
an Android app, scan and connect to a tag, send commands and respond to
notifications using Android Studio.

You will need:
1. Some Jacquard gear. You can order it [here](https://atap.google.com/jacquard/products/).
2. An Android device (since the Jacquard SDK uses Bluetooth to connect to
   your tag the simulator won't work).

## Table of contents
1. [Prepare your Jacquard Tag](#section1)
1. [Create a new Android project](#section2)
1. [Integrate Jacquard SDK](#section3)
1. [Add the scanning RecyclerView](#section4)
1. [Displaying the advertised tags and populating the RecyclerView](#section5)
1. [Displaying already connected tags](#section6)
1. [Connecting to tags](#section7)

## <a name="section1"></a>1. Prepare your Jacquard Tag

To ensure the smoothest path through this tutorial, go to Android
Bluetooth settings, find the entry for your Jacquard Tag and choose
"Forget this device".

## <a name="section2"></a>2. Create a new Android project

In Android Studio, create a new Android project.

> File menu -> New -> New Project...

![Android Studio new project dialog](../../assets/media/tutorial/create_new_project.png)

Select `No Activity` as your project template then create the project.

## <a name="section3"></a>3. Integrate Jacquard SDK

It's easy to integrate the Jacquard SDK using Maven. First, you need to include GMaven into your project gradle file - `build.gradle (Project: Jacquard_Tutorial)`.

```java
allprojects {
  repositories {
    ...
    maven {
        url "https://maven.google.com/"
    }
  }
}
```

Then you need to include Jacquard in the `dependencies` section of your application's gradle file - `build.gradle (Module: Jacquard_Tutorial.app)`.
You might need to expand the Gradle Scripts dropdown in the Project panel on the left.

```java
implementation "com.google.jacquard:jacquard-sdk:1.0.0"
```

## <a name="section4"></a>4. Add the scanning RecyclerView

The app you will make has two screens. The first is a RecyclerView
listing any nearby advertising tags. The second is a screen with a few
simple labels and buttons which you will connect to Jacquard
functions.

First lets set up the Scanning activity.

1. {.tutorial_list}Select `File > New > Activity > Empty Activity`.
   ![Create main activity](../../assets/media/tutorial/create_main_activity.png)
   We will also make this the main activity.
1. Open `activity_main.xml` and add a RecyclerView. Your activity layout file should look something like this if you select "Split" view:
   ![Main activity layout](../../assets/media/tutorial/activity_main.png)

Now we can start coding!

### Set blue tooth permissions

First we need to request for bluetooth permissions, which also requires fine location permission. In `AndroidManifest.xml`, make sure you have these
permissions:

```
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Create RecyclerView adapter

We have to create an adapter to update the RecycleView's UI. Create a file named `TagListAdapter`. In this adapter, we are going to implement a simple
item UI with just the name of the tag:
```java
public class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.AdvertisedJacquardTagViewHolder> {
  private List<AdvertisedJacquardTag> tagList;

  public TagListAdapter(List<AdvertisedJacquardTag> tagList) {
    this.tagList = tagList;
  }

  @NonNull
  @Override
  public AdvertisedJacquardTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = layoutInflater.inflate(R.layout.tag_item, parent, false);
    return new AdvertisedJacquardTagViewHolder(view);
  }

  @Override
  public int getItemCount() {
    return tagList.size();
  }

  @Override
  public void onBindViewHolder(@NonNull AdvertisedJacquardTagViewHolder holder, int position) {
    holder.bindView(tagList.get(position).displayName());
  }

  class AdvertisedJacquardTagViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final View view;

    AdvertisedJacquardTagViewHolder(View itemView) {
      super(itemView);
      view = itemView;
      view.setOnClickListener(this);
    }

    void bindView(String name) {
      TextView tv = view.findViewById(R.id.tag_item_name);
      tv.setText(name);
    }

    @Override
    public void onClick(View v) {
      // Item click
    }
  }
}
```

And the layout file should look something like the following:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="80dp">

  <TextView
      android:id="@+id/tag_item_name"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:gravity="center_vertical" />

</RelativeLayout>
```

### Setup RecyclerView

Now switching back to your MainActivity file. First, we need to create a private variable to hold our scanned tag list:

```
private ArrayList<AdvertisedJacquardTag> tags = new ArrayList<>();
```

Then, in the `onCreate` method we will set our RecycleView to use the newly created TagListAdapter:

```
RecyclerView recyclerView = findViewById(R.id.tag_recyclerview);
recyclerView.setLayoutManager(new LinearLayoutManager(this));
recyclerView.setAdapter(new TagListAdapter(tags));
```

## <a name="section5"></a>5. Displaying the advertised tags and populating the RecyclerView

Before we can scan for tags, we'll need to request for the necessary permissions. Copy and paste the following into MainActivity:

```java
private final ActivityResultLauncher<String> requestPermissionLauncher =
    registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {
          if (isGranted) {
            startScan();
          }
        });

private boolean hasPermissions() {
  if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
    return true;
  } else if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
    // User has denied the permission. Its time to show rationale.
    return false;
  } else {
    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION);
    return false;
  }
}
```

Now create a `onResume` method and put the following in `onResume`. This will request the permission if necessary whenever we come back to this screen.

```
if (hasPermissions()) {
  startScan();
}
```

### Scan for advertising Jacquard tags

Now we will create the startScan method that was used in `onResume` and put our tag scanning codes inside.
This will start scanning for advertising Jacquard tags whenever the activity is back on screen.

```java
private void startScan() {
    JacquardManager jacquardManager = JacquardManager.getInstance();
    Signal<List<AdvertisedJacquardTag>> scanningSignal = jacquardManager.startScanning()
        .distinct()
        .scan(tags,
            (tagList, tag) -> {
              tagList.add(tag);
              return tagList;
            });
    scanningSignal.onNext(tagList -> {
      // Notify RecyclerView adapter to update the list
      RecyclerView recyclerView = findViewById(R.id.tag_recyclerview);
      recyclerView.getAdapter().notifyDataSetChanged();
    });
}
```

### Put the Jacquard Tag into advertising mode

Press and hold the button on your tag for 3 or 4 seconds. The LED on
the tag will start pulsing.

You should be able to the RecyclerView updated with your Jacquard Tag name.


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

Connecting to tags is an important step that is fully documented in the api documentation.

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
package teacher.retirementcountdown.app.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.wasabeef.glide.transformations.BlurTransformation;
import teacher.retirementcountdown.app.adapter.BucketListAdapter;
import teacher.retirementcountdown.app.model.BucketListItem;
import teacher.retirementcountdown.app.R;

public class BucketActivity extends AppCompatActivity {

    private AdView adView;
    private ImageView backgroundImageView;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BackgroundPrefs";
    private static final String PREFS_NAME2 = "BackgroundPrefs2";
    private static final String IMAGE_INDEX_KEY = "imageIndex";
    private static final String KEY_BUCKET_LIST = "bucket_list";

    private int INTERVAL_MS;
    private int bluring = 1;
    private int currentImageIndex;

    private ArrayList<String> flower_array;
    private List<BucketListItem> bucketListItems = new ArrayList<>();
    private BucketListAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bucket);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backgroundImageView = findViewById(R.id.backgroundImageView2);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentImageIndex = sharedPreferences.getInt(IMAGE_INDEX_KEY, 0);

        loadSelectedImages();
        loadDuration();

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        EditText editTextItem = findViewById(R.id.editTextItem);
        Button buttonAddItem = findViewById(R.id.buttonAddItem);

        bucketListItems = loadBucketListItems();

        adapter = new BucketListAdapter(bucketListItems,
                this::onDeleteItem,
                this::onCheckItem);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonAddItem.setOnClickListener(v -> {
            String text = editTextItem.getText().toString();
            if (!text.isEmpty()) {
                bucketListItems.add(new BucketListItem(text, false));
                adapter.notifyDataSetChanged();
                saveBucketListItems();
                editTextItem.setText("");
            }
        });

        MobileAds.initialize(this, initializationStatus -> {
        });
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
        handler.removeCallbacks(backgroundChanger);
    }

    private void onDeleteItem(BucketListItem item) {
        bucketListItems.remove(item);
        adapter.notifyDataSetChanged();
        saveBucketListItems();
    }

    private void onCheckItem(BucketListItem item, boolean isChecked) {
        item.setChecked(isChecked);
        saveBucketListItems();
    }

    private void saveBucketListItems() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(bucketListItems);
        editor.putString(KEY_BUCKET_LIST, json);
        editor.apply();
    }

    private List<BucketListItem> loadBucketListItems() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_BUCKET_LIST, null);
        Type type = new TypeToken<ArrayList<BucketListItem>>() {
        }.getType();
        List<BucketListItem> items = gson.fromJson(json, type);
        return items != null ? items : new ArrayList<>();
    }

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Intent intent = new Intent(BucketActivity.this, MainActivity.class);
            intent.putExtra("checkUpdated", false);
            startActivity(intent);
            finish();
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (flower_array.size() > 0) {
                        setBackgroundImage(0);
                    }
                } else {
                    Toast.makeText(this, "Permission denied. Cannot access media.", Toast.LENGTH_SHORT).show();
                }
            });

    private Runnable backgroundChanger = new Runnable() {
        @Override
        public void run() {
            currentImageIndex = (currentImageIndex + 1) % flower_array.size();

            if (flower_array.size() > 1) {
                bluring = 0;
            }
            setBackgroundImage(currentImageIndex);
            saveBackgroundIndex(currentImageIndex);

            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    private void saveBackgroundIndex(int index) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(IMAGE_INDEX_KEY, index);
        editor.apply();
    }

    private void loadDuration() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedDuration = sharedPreferences.getString("DURATION_KEY", "No Duration");
        if (savedDuration != null && !savedDuration.equals("") && !savedDuration.equals("No Duration")) {
            INTERVAL_MS = Integer.parseInt(savedDuration) * 1000;
            handler.postDelayed(backgroundChanger, INTERVAL_MS);
        } else {
            handler.removeCallbacks(backgroundChanger);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setBackgroundImage(int index) {
        String uriString = flower_array.get(index);
        RequestOptions requestOptions = new RequestOptions();

        if (bluring != 0) {
            requestOptions = requestOptions.transform(new BlurTransformation(bluring));
        }

        if (uriString.startsWith("android.resource")) {
            int resourceId = getResourceIdFromUri(uriString);
            Glide.with(this)
                    .load(resourceId)
                    .apply(requestOptions)
                    .into(backgroundImageView);
        } else if (uriString.startsWith("content://")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED) {
                    loadContentUri(Uri.parse(uriString), requestOptions);
                } else {
                    // Request permission if not granted
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    loadContentUri(Uri.parse(uriString), requestOptions);
                } else {
                    // Request permission if not granted
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        }
    }

    private void loadContentUri(Uri uri, RequestOptions requestOptions) {
        Glide.with(this)
                .load(uri)
                .apply(requestOptions)
                .into(backgroundImageView);
    }

    private int getResourceIdFromUri(String uriString) {
        Uri uri = Uri.parse(uriString);
        return Integer.parseInt(uri.getLastPathSegment());
    }

    private void loadSelectedImages() {
        Set<String> selectedImages = new HashSet<>();
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedSelectedImages = sharedPreferences.getStringSet("selected_images", new HashSet<>());
        selectedImages.addAll(savedSelectedImages);

        flower_array = new ArrayList<>(selectedImages);

        System.out.println("yaser hamdo222 ");

        for (int i = 0; i < flower_array.size(); i++) {
            System.out.println("yaser hamdo222 " + flower_array.get(i));
        }

        if (flower_array.isEmpty()) {
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide2);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide1);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide4);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide3);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide5);
        }
    }
}
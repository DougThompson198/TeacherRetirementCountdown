package teacher.retirementcountdown.app.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teacher.retirementcountdown.app.R;
import teacher.retirementcountdown.app.adapter.ImageAdapter;

public class ImagesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BackgroundPrefs";
    private static final String PREFS_NAME2 = "BackgroundPrefs2";
    private static final String KEY_SAVED_IMAGES = "saved_images";

    private AdView adView;
    private RecyclerView recyclerView;

    private List<String> imageList;
    private Set<String> selectedImages;
    private ImageAdapter adapter;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_images);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageList = new ArrayList<>();
        selectedImages = new HashSet<>();

        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ImageAdapter(imageList, selectedImages, this::deleteImage, this::selectImage);
        recyclerView.setAdapter(adapter);

        loadSavedImages();

        loadSelectedImages();

        addDrawableImages();

        Button addButton = findViewById(R.id.addImageButton);
        addButton.setOnClickListener(v -> showImageSourceDialog());

        Button setDuration = findViewById(R.id.setDuration);
        setDuration.setOnClickListener(v -> showDurationPopup());

        Button deleteAllImages = findViewById(R.id.deleteAllImages);
        deleteAllImages.setOnClickListener(v -> deleteAll());

        Button setBlur = findViewById(R.id.setBlur);
        setBlur.setOnClickListener(v -> showBluringPopup());

        Button selectAllButton = findViewById(R.id.selectAllButton);
        selectAllButton.setOnClickListener(v -> toggleSelectAll());

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        MobileAds.initialize(this, initializationStatus -> {});
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
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

    private void deleteAll() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME2, MODE_PRIVATE);
        preferences.edit().clear().apply();
        imageList = new ArrayList<>();
        selectedImages = new HashSet<>();
        addDrawableImages();
        adapter = new ImageAdapter(imageList, selectedImages, this::deleteImage, this::selectImage);
        recyclerView.setAdapter(adapter);
    }

    private void showImageSourceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);

        Button cameraButton = dialog.findViewById(R.id.cameraButton);
        Button galleryButton = dialog.findViewById(R.id.galleryButton);

        cameraButton.setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermission();
        });

        galleryButton.setOnClickListener(v -> {
            dialog.dismiss();
            checkGalleryPermission();
        });

        dialog.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                }
            });

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            imageUri = FileProvider.getUriForFile(this, "teacher.retirementcountdown.app.fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraLauncher.launch(cameraIntent);
        }
    }

    private File createImageFile() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (imageUri != null) {
                        imageList.add(imageUri.toString());
                        selectedImages.add(imageUri.toString());
                        saveSelectedImages();
                        adapter.notifyDataSetChanged();
                        saveImageUri(imageUri);
                    }
                }
            });

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openGallery();
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                }
            });

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        imageList.add(selectedImage.toString());
                        selectedImages.add(selectedImage.toString());
                        saveSelectedImages();
                        adapter.notifyDataSetChanged();
                        saveImageUri(selectedImage);
                    }
                }
            });

    private void saveImageUri(Uri imageUri) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedImages = sharedPreferences.getStringSet(KEY_SAVED_IMAGES, new HashSet<>());
        savedImages.add(imageUri.toString());
        sharedPreferences.edit().putStringSet(KEY_SAVED_IMAGES, savedImages).apply();
    }

    private void loadSavedImages() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedImages = sharedPreferences.getStringSet(KEY_SAVED_IMAGES, new HashSet<>());
        imageList.addAll(savedImages);
    }

    private void addDrawableImages() {
        imageList.add("android.resource://" + getPackageName() + "/" + R.drawable.slide1);
        imageList.add("android.resource://" + getPackageName() + "/" + R.drawable.slide2);
        imageList.add("android.resource://" + getPackageName() + "/" + R.drawable.slide3);
        imageList.add("android.resource://" + getPackageName() + "/" + R.drawable.slide4);
        imageList.add("android.resource://" + getPackageName() + "/" + R.drawable.slide5);
        adapter.notifyDataSetChanged();
    }

    private void deleteImage(String imageUri) {
        if (!imageUri.startsWith("android.resource")) {
            imageList.remove(imageUri);
            adapter.notifyDataSetChanged();
            removeImageUri(imageUri);
        }
    }

    private void removeImageUri(String imageUri) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedImages = sharedPreferences.getStringSet(KEY_SAVED_IMAGES, new HashSet<>());
        savedImages.remove(imageUri);
        sharedPreferences.edit().putStringSet(KEY_SAVED_IMAGES, savedImages).apply();
    }

    private void selectImage(String imageUri) {
        if (selectedImages.contains(imageUri)) {
            selectedImages.remove(imageUri);
        } else {
            selectedImages.add(imageUri);
        }
        saveSelectedImages();
        adapter.notifyDataSetChanged();
    }

    private void toggleSelectAll() {
        if (selectedImages.size() == imageList.size()) {
            selectedImages.clear();
        } else {
            selectedImages.addAll(imageList);
        }
        saveSelectedImages();
        adapter.notifyDataSetChanged();
    }

    private void saveSelectedImages() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        sharedPreferences.edit().putStringSet("selected_images", selectedImages).apply();
    }

    private void loadSelectedImages() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedSelectedImages = sharedPreferences.getStringSet("selected_images", new HashSet<>());
        selectedImages.addAll(savedSelectedImages);
        ArrayList<String> flower_array = new ArrayList<>(selectedImages);

    }

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Intent intent = new Intent(ImagesActivity.this, MainActivity.class);
            intent.putExtra("checkUpdated", false);
            startActivity(intent);
            finish();
        }
    };

    private void showDurationPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.duration_picker_dialog);

        NumberPicker numberPicker = dialog.findViewById(R.id.number_picker);
        Button saveButton = dialog.findViewById(R.id.save_button);

        String[] durationValues = new String[31];
        for (int i = 0; i < 30; i++) {
            durationValues[i] = (i + 1) + "";
        }
        durationValues[30] = "No Duration";

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(durationValues.length - 1);
        numberPicker.setDisplayedValues(durationValues);

        saveButton.setOnClickListener(v -> {
            int selectedIndex = numberPicker.getValue();
            String selectedValue = durationValues[selectedIndex];

            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("DURATION_KEY", selectedValue);
            editor.apply();

            dialog.dismiss();
        });

        dialog.show();
    }

    private void showBluringPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.bluring_picker_dialog);

        NumberPicker numberPicker = dialog.findViewById(R.id.number_picker);
        Button saveButton = dialog.findViewById(R.id.save_button);

        String[] durationValues = new String[26];
        for (int i = 0; i < 25; i++) {
            durationValues[i] = (i + 1) + "";
        }
        durationValues[25] = "No Bluring";

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(durationValues.length - 1);
        numberPicker.setDisplayedValues(durationValues);

        saveButton.setOnClickListener(v -> {
            int selectedIndex = numberPicker.getValue();
            String selectedValue = durationValues[selectedIndex];


            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("BlUR_KEY", selectedValue);
            editor.apply();

            dialog.dismiss();
        });
        dialog.show();
    }

}



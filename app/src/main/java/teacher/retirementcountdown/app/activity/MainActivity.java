package teacher.retirementcountdown.app.activity;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import jp.wasabeef.glide.transformations.BlurTransformation;
import teacher.retirementcountdown.app.R;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 100;
    private AppUpdateManager appUpdateManager;

    private AdView adView;

    private ArrayList<String> flower_array;
    private SharedPreferences sharedPreferences;

    private ConstraintLayout layout;

    private FrameLayout changeBackgroundButton, shareButton, changeDateButton, buketButton, privacyButton;
    private ImageView backgroundImageView;
    private LinearLayout retirementDayTextView;
    private TextView yearsTextView, monthsTextView, daysTextView, hoursTextView, workDaysTextView, weekDaysTextView, date;

    private int currentImageIndex;

    private static final String PREFS_NAME = "BackgroundPrefs";
    private static final String PREFS_NAME2 = "BackgroundPrefs2";
    private static final String IMAGE_INDEX_KEY = "imageIndex";

    private Handler handler = new Handler(Looper.getMainLooper());
    private int INTERVAL_MS;
    private int bluring = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(() -> false);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        loadSelectedImages();
        onClickListeners();
        loadDuration();
        setBackgroundImage(currentImageIndex);
        selectType(sharedPreferences.getString("type", ""));

        MobileAds.initialize(this, initializationStatus -> {
        });
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        appUpdateManager = AppUpdateManagerFactory.create(this);
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("checkUpdated")) {
            checkForAppUpdate();
        }

        checkGalleryPermission();
    }

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
            else {
                checkCameraPermission();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            else {
                checkCameraPermission();
            }
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
        }
    }

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {

                }
            });

    private void checkForAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            MainActivity.this,
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(e -> {
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Toast.makeText(this, "An update has been downloaded. Please restart the app.", Toast.LENGTH_LONG).show();
                appUpdateManager.completeUpdate();
            }
        });

    }

    private void setDate(int year, int month, int day) {

        boolean type = sharedPreferences.getString("type", "").equals("work");

        if (type) {

            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            LocalDate givenDate = LocalDate.of(year, month, day);

            Period difference = Period.between(currentDate, givenDate);

            int years = difference.getYears();
            int months = difference.getMonths();
            int days = difference.getDays();

            int totalDays = (int)((years * 192) + (months * 16.167) + (days));

            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(totalDays);
            Period period = Period.between(startDate, endDate);

            int years2 = period.getYears();
            int months2 = period.getMonths();
            int days2 = period.getDays();

            int hours2 = 0;
            if (days2 > 0) {
                hours2 = 23 - currentTime.getHour();
                days2 = days2 - 1;
            }

            yearsTextView.setText(years2 + "");
            monthsTextView.setText(months2 + "");
            daysTextView.setText(days2 + "");
            hoursTextView.setText(hours2 + "");
        }
        else {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            LocalDate givenDate = LocalDate.of(year, month, day);

            Period difference = Period.between(currentDate, givenDate);

            int years = difference.getYears();
            int months = difference.getMonths();
            int days = difference.getDays();

            int hours = 0;
            if (days > 0) {
                hours = 23 - currentTime.getHour();
                days = days - 1;
            }
            yearsTextView.setText(years + "");
            monthsTextView.setText(months + "");
            daysTextView.setText(days + "");
            hoursTextView.setText(hours + "");
        }
    }

    private void init() {

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentImageIndex = sharedPreferences.getInt(IMAGE_INDEX_KEY, 0);

        changeBackgroundButton = findViewById(R.id.changeBackgroundButton);
        backgroundImageView = findViewById(R.id.backgroundImageView);
        shareButton = findViewById(R.id.shareButton);
        changeDateButton = findViewById(R.id.changeDateButton);
        buketButton = findViewById(R.id.bucketButton);
        privacyButton = findViewById(R.id.privacyButton);
        retirementDayTextView = findViewById(R.id.retirementDayTextView);
        yearsTextView = findViewById(R.id.yearsTextView);
        date = findViewById(R.id.date);
        daysTextView = findViewById(R.id.daysTextView);
        hoursTextView = findViewById(R.id.hoursTextView);
        monthsTextView = findViewById(R.id.monthsTextView);
        workDaysTextView = findViewById(R.id.workDaysTextView);
        weekDaysTextView = findViewById(R.id.weekDaysTextView);
        layout = findViewById(R.id.main);

        currentImageIndex = 0;
    }

    private Runnable backgroundChanger = new Runnable() {
        @Override
        public void run() {
            currentImageIndex = (currentImageIndex + 1) % flower_array.size();
            setBackgroundImage(currentImageIndex);
            saveBackgroundIndex(currentImageIndex);
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
        handler.removeCallbacks(backgroundChanger);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void onClickListeners() {
        changeBackgroundButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ImagesActivity.class);
            handler.removeCallbacks(backgroundChanger);
            startActivity(i);
            finish();
        });

        shareButton.setOnClickListener(v -> {
            showImageSourceDialog();
        });

        retirementDayTextView.setOnClickListener(v -> {
            showCustomDatePicker(sharedPreferences.getString("type", "").equals("work"));
        });

        changeDateButton.setOnClickListener(v -> {
            showCustomDatePicker(sharedPreferences.getString("type", "").equals("work"));
        });

        buketButton.setOnClickListener(v -> {
            Intent i = new Intent(this, BucketActivity.class);
            handler.removeCallbacks(backgroundChanger);
            startActivity(i);
            finish();
         });

        privacyButton.setOnClickListener(v -> {
            openLink();
        });

        workDaysTextView.setOnClickListener(v -> {
            selectType("work");
        });

        weekDaysTextView.setOnClickListener(v -> {
            selectType("week");
        });

    }

    private void showImageSourceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source2);
        Button cameraButton = dialog.findViewById(R.id.cameraButton);
        Button galleryButton = dialog.findViewById(R.id.galleryButton);
        Button bothButton = dialog.findViewById(R.id.bothButton);

        cameraButton.setOnClickListener(v -> {
            dialog.dismiss();
            shareCurrentScreen3();

        });

        galleryButton.setOnClickListener(v -> {
            dialog.dismiss();

            shareCurrentScreen2();

        });

        bothButton.setOnClickListener(v -> {
            dialog.dismiss();

            shareCurrentScreen();

        });

        dialog.show();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void selectType(String type) {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("type", type);
        editor.apply();

        restoreSavedValues();

        switch (type) {
            case "work":
                weekDaysTextView.setTextColor(getColor(R.color.black));
                weekDaysTextView.setBackground(getDrawable(R.drawable.shape2white));
                workDaysTextView.setTextColor(getColor(R.color.white));
                workDaysTextView.setBackground(getDrawable(R.drawable.shape3));
                break;
            case "week":
                workDaysTextView.setTextColor(getColor(R.color.black));
                workDaysTextView.setBackground(getDrawable(R.drawable.shape3white));
                weekDaysTextView.setTextColor(getColor(R.color.white));
                weekDaysTextView.setBackground(getDrawable(R.drawable.shape2));
                break;
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setBackgroundImage(int index) {

        DrawableCrossFadeFactory factory =
                new DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build();

        String uriString = flower_array.get(index);
        RequestOptions requestOptions = new RequestOptions();

        if (bluring != 0) {
            requestOptions = requestOptions.transform(new BlurTransformation(bluring));
        }

        if (uriString.startsWith("android.resource")) {
            int resourceId = getResourceIdFromUri(uriString);
            Glide.with(this)
                    .load(resourceId)
                    .transition(withCrossFade(factory))
                    .error(R.drawable.slide2)
                    //.apply(requestOptions)
                    .into(backgroundImageView);

        } else if (uriString.startsWith("content://")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d("test22", "TIRAMISU");

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d("test22", "READ_MEDIA_IMAGES");
                    loadContentUri(Uri.parse(uriString), requestOptions);
                    Log.d("test222", Uri.parse(uriString) + "");
                } else {
                    Log.d("test22", "READ_MEDIA_IMAGES else");
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d("test22", "READ_EXTERNAL_STORAGE");
                    loadContentUri(Uri.parse(uriString), requestOptions);
                } else {
                    Log.d("test22", "READ_EXTERNAL_STORAGE else");
                    // Request permission if not granted
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        }
    }

    private void loadContentUri(Uri uri, RequestOptions requestOptions) {
        DrawableCrossFadeFactory factory =
                new DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build();
        Glide.with(this)
                .load(uri)
                .transition(withCrossFade(factory))
                .error(R.drawable.slide2)
                //.apply(requestOptions)
                .into(backgroundImageView);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (flower_array.size() > 0) {
                        setBackgroundImage(0);
                    }
                } else {
                }
            });

    private int getResourceIdFromUri(String uriString) {
        Uri uri = Uri.parse(uriString);
        return Integer.parseInt(uri.getLastPathSegment());
    }

    private void saveBackgroundIndex(int index) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(IMAGE_INDEX_KEY, index);
        editor.apply();
    }

    private void shareCurrentScreen() {
        Bitmap bitmap = getScreenshot(layout);

        try {
            File file = saveBitmap(bitmap);
            if (file != null) {
                Uri uri = FileProvider.getUriForFile(this, "teacher.retirementcountdown.app.fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Look I only have " + yearsTextView.getText().toString() + " years " + monthsTextView.getText().toString() + " months " + daysTextView.getText().toString() + " days and " + hoursTextView.getText().toString() + " hours left what about you?\n" + appLink);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareCurrentScreen2() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");


        String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();
        String message = "Look I only have " + yearsTextView.getText().toString() + " years " +
                monthsTextView.getText().toString() + " months " +
                daysTextView.getText().toString() + " days and " +
                hoursTextView.getText().toString() + " hours left. What about you?\n" + appLink;

        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void shareCurrentScreen3() {
        Bitmap bitmap = getScreenshot(layout);

        try {
            File file = saveBitmap(bitmap);
            if (file != null) {
                Uri uri = FileProvider.getUriForFile(this, "teacher.retirementcountdown.app.fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");

                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getScreenshot(View view) {
        Bitmap screenshot = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        view.draw(canvas);
        return screenshot;
    }

    private File saveBitmap(Bitmap bitmap) throws IOException {
        String fileName = "shared_image.png";
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SharedImages");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        return file;
    }

    private void showCustomDatePicker(boolean isCustomYear) {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        Calendar calendar = Calendar.getInstance();
        long today = calendar.getTimeInMillis();
        constraintsBuilder.setStart(today);

        calendar.set(2100, Calendar.DECEMBER, 31);
        long endDate = calendar.getTimeInMillis();
        constraintsBuilder.setEnd(endDate);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setTheme(R.style.CustomDatePicker)
                .setSelection(today)
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTimeInMillis(selection);

            int selectedYear = selectedCalendar.get(Calendar.YEAR);
            int selectedMonth = selectedCalendar.get(Calendar.MONTH) + 1;
            int selectedDay = selectedCalendar.get(Calendar.DAY_OF_MONTH);



            if (selection < today) {
                return;
            }

            saveDate(selectedYear, selectedMonth, selectedDay);

            setDate(selectedYear, selectedMonth, selectedDay);

            date.setText("Your retirement date: " + selectedDay + "-" + selectedMonth + "-" + selectedYear);

        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void restoreSavedValues() {
        LocalDate currentDate = LocalDate.now();
        int savedYear = sharedPreferences.getInt("saved_year", currentDate.getYear());
        int savedMonth = sharedPreferences.getInt("saved_month", currentDate.getMonthValue());
        int savedDay = sharedPreferences.getInt("saved_day", currentDate.getDayOfMonth());
        setDate(savedYear, savedMonth, savedDay);
        date.setText("Your retirement date: " + savedDay + "-" + savedMonth + "-" + savedYear);
    }

    private void saveDate(int year, int month, int day) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("saved_year", year);
        editor.putInt("saved_month", month);
        editor.putInt("saved_day", day);
        editor.apply();
    }

    private void loadSelectedImages() {
        Set<String> selectedImages = new HashSet<>();
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME2, Context.MODE_PRIVATE);
        Set<String> savedSelectedImages = sharedPreferences.getStringSet("selected_images", new HashSet<>());
        selectedImages.addAll(savedSelectedImages);

        flower_array = new ArrayList<>(selectedImages);

        if (flower_array.isEmpty()) {

            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide2);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide1);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide4);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide3);
            flower_array.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide5);

            selectedImages.add("android.resource://teacher.retirementcountdown.app/" + R.drawable.slide2);

            sharedPreferences.edit().putStringSet("selected_images", selectedImages).apply();
        }

    }

    private void loadDuration() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedDuration = sharedPreferences.getString("DURATION_KEY", "No Duration");

        if(savedDuration != null && !savedDuration.equals("") && !savedDuration.equals("No Duration")) {

            INTERVAL_MS = Integer.parseInt(savedDuration) * 1000;
            handler.postDelayed(backgroundChanger, INTERVAL_MS);
        }
        else if (!flower_array.isEmpty() && flower_array.size() > 1){

        }
        else {
            handler.removeCallbacks(backgroundChanger);
        }
    }

    private void openLink() {
        String url = "https://doc-hosting.flycricket.io/teacher-retirement-countdown/65cfef7d-3066-4fea-83d7-79b8f3fa63ae/privacy";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
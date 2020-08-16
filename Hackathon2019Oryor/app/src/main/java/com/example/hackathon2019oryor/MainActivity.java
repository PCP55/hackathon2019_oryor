package com.example.hackathon2019oryor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    //FAB
    private FloatingActionButton fab_main, fab1_camera, fab2_image, fab3_text;
    private Animation fab_open, fab_close, fab_clock, fab_anticlock;
    Boolean isOpen = false;

    // Data variables
    EditText text_input;
    private ArrayList<Oryor> mOryor = new ArrayList<>();
    private String textImage;
    ListAdapter adapter = new ListAdapter(this, mOryor);
    RecyclerView recyclerView;
    private String oryorID;
    private JSONObject output_json;
    Button btnSearch;

    // Camera and Image variables
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_GET_PHOTO = 2;
    static final int REQUEST_IMAGE_GALLERY = 2;
    private Bitmap mSelectedImage;
    private String mCurrentPhotoPath;

    private ProgressBar spinner;
    private final ColorDrawable background = new ColorDrawable(0xFFFF6666);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestMultiplePermissions();
        fab_main = findViewById(R.id.fab);
        fab1_camera = findViewById(R.id.fab1);
        fab2_image = findViewById(R.id.fab2);
        fab3_text = findViewById(R.id.fab3);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_clock = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_clock);
        fab_anticlock = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_anticlock);

        text_input = findViewById(R.id.searchInput);
        btnSearch = findViewById(R.id.btnSearch);

        spinner = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.INVISIBLE);

        initData();

        text_input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Log.d("focus", "focus lost");
                    // Do whatever you want here
                    closeKeyboard();
                } else {
                    Log.d("focus", "focused");
                    showKeyboard();
                }
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!text_input.getText().equals("")){
                    oryorID = text_input.getText().toString();
                    spinner.setVisibility(View.VISIBLE);
                    makePost();
                }
            }
        });

        fab_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isOpen) {
                    fab1_camera.startAnimation(fab_close);
                    fab2_image.startAnimation(fab_close);
                    fab3_text.startAnimation(fab_close);
                    fab1_camera.setClickable(false);
                    fab2_image.setClickable(false);
                    fab3_text.setClickable(false);
                    isOpen = false;
                } else {
                    fab1_camera.startAnimation(fab_open);
                    fab2_image.startAnimation(fab_open);
                    fab3_text.startAnimation(fab_open);
                    fab1_camera.setClickable(true);
                    fab2_image.setClickable(true);
                    fab3_text.setClickable(true);
                    isOpen = true;
                }

            }
        });

        fab1_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard();
                dispatchTakePictureIntent();
            }
        });


        fab2_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard();
                choosePhotoFromGallery();
            }
        });


        fab3_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text_input.requestFocus();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    Log.d("focus", "touchevent");
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, REQUEST_GET_PHOTO);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE: {
                    if (resultCode == RESULT_OK) {
                        File file = new File(mCurrentPhotoPath);
                        Bitmap bitmap = MediaStore.Images.Media
                                .getBitmap(this.getContentResolver(), Uri.fromFile(file));
                        if (bitmap != null) {
                            bitmap = modifyOrientation(bitmap, mCurrentPhotoPath);
                            mSelectedImage = bitmap;
                            runTextRecognition();
                        }
                    }
                    break;
                }
                case REQUEST_IMAGE_GALLERY: {
                    if (intent != null) {
                        Uri contentURI = intent.getData();
                        try {
                            mCurrentPhotoPath = getPath( getApplicationContext( ), contentURI);
                            Log.d("info", mCurrentPhotoPath);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                            bitmap = modifyOrientation(bitmap, mCurrentPhotoPath);
                            mSelectedImage = bitmap;
                            runTextRecognition();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("info",e.toString());
                            Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }

        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void runTextRecognition() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
                                if (blocks.size() == 0) {
                                    showToast("ไม่พบหมายเลขอย. กรุณาลองใหม่อีกครั้ง");
                                    return ;
                                }
                                else{
                                    textImage = "";
                                    for (int i = 0; i < blocks.size(); i++) {
                                        List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
                                        for (int j = 0; j < lines.size(); j++) {
                                            List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                                            for (int k = 0; k < elements.size(); k++) {
//                                                Log.i("info", elements.get(k).getText());
                                                textImage = textImage + elements.get(k).getText();
                                            }
                                        }
                                    }
                                    String pattern = "(\\d{2}[-]{1}\\d{1}[-]{1}\\d{5}[-]{1}\\d{1}[-]{1}\\d{4})";
                                    Pattern r = Pattern.compile(pattern);
                                    Matcher m = r.matcher(textImage);
                                    if (m.find()) {
                                        Log.i("info", "Found value: " + m.group(0));
                                        Log.i("info", "Found value: " + m.group(1));
                                        oryorID = m.group(1);
                                        spinner.setVisibility(View.VISIBLE);
                                        makePost();
                                    }
//                                    else{
//                                        showToast("ไม่พบหมายเลขอย. กรุณาลองใหม่อีกครั้ง");
//                                    }
                                    else {
                                        pattern = "(\\d{2}[-]{1}\\d{1}[-]{1}\\d{2}[-]{1}\\d{5})";
                                        r = Pattern.compile(pattern);
                                        m = r.matcher(textImage);
                                        if (m.find()) {
                                            Log.i("info", "Found value: " + m.group(0));
                                            Log.i("info", "Found value: " + m.group(1));
                                            oryorID = m.group(1);
                                            spinner.setVisibility(View.VISIBLE);
                                            makePost();
                                        }
                                        else{
                                            pattern = "(\\d{1}[A]{1}[ ]{1}\\d+[\\/]{1}\\d+)";
                                            r = Pattern.compile(pattern);
                                            m = r.matcher(textImage);
                                            if (m.find()) {
                                                Log.i("info", "Found value: " + m.group(0));
                                                Log.i("info", "Found value: " + m.group(1));
                                                oryorID = m.group(1);
                                                spinner.setVisibility(View.VISIBLE);
                                                makePost();
                                            }
                                            else{
                                                pattern = "(\\d{2}[\\/]\\d{4})";
                                                r = Pattern.compile(pattern);
                                                m = r.matcher(textImage);
                                                if (m.find()) {
                                                    Log.i("info", "Found value: " + m.group(0));
                                                    Log.i("info", "Found value: " + m.group(1));
                                                    oryorID = m.group(1);
                                                    spinner.setVisibility(View.VISIBLE);
                                                    makePost();
                                                }
                                                else{
                                                    showToast("ไม่พบหมายเลขอย. กรุณาลองใหม่อีกครั้ง");
                                                }
//                                                else{
//                                                    pattern = "((วอส.){1}\\d{2}[\\/]\\d{4})";
//                                                    r = Pattern.compile(pattern);
//                                                    m = r.matcher(textImage);
//                                                    if (m.find()) {
//                                                        Log.i("info", "Found value: " + m.group(0));
//                                                        Log.i("info", "Found value: " + m.group(1));
//                                                        oryorID = m.group(1);
//                                                        spinner.setVisibility(View.VISIBLE);
//                                                        makePost();
//                                                    }
//                                                    else{
//                                                        pattern = "((ผ.){1}\\d{3}[\\/]\\d{4})";
//                                                        r = Pattern.compile(pattern);
//                                                        m = r.matcher(textImage);
//                                                        if (m.find()) {
//                                                            Log.i("info", "Found value: " + m.group(0));
//                                                            Log.i("info", "Found value: " + m.group(1));
//                                                            oryorID = m.group(1);
//                                                            spinner.setVisibility(View.VISIBLE);
//                                                            makePost();
//                                                        }
//                                                        else{
//                                                            pattern = "((น.){1}\\d{3}[\\/]\\d{4})";
//                                                            r = Pattern.compile(pattern);
//                                                            m = r.matcher(textImage);
//                                                            if (m.find()) {
//                                                                Log.i("info", "Found value: " + m.group(0));
//                                                                Log.i("info", "Found value: " + m.group(1));
//                                                                oryorID = m.group(1);
//                                                                spinner.setVisibility(View.VISIBLE);
//                                                                makePost();
//                                                            }
//                                                            else{
//                                                                showToast("ไม่พบหมายเลขอย. กรุณาลองใหม่อีกครั้ง");
//                                                            }
//                                                        }
//                                                    }
//                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static String getPath( Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    public String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("searchlist.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void makePost() {
        final TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] x509Certificates = new X509Certificate[0];
                return x509Certificates;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain,
                                           final String authType) {
                Log.i("info", "authType: " + authType);
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain,
                                           final String authType) {
                Log.i("info", "authType: " + authType);
            }
        }};

        X509TrustManager x509TrustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] x509Certificates = new X509Certificate[0];
                return x509Certificates;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain,
                                           final String authType) {
                Log.i("info", "authType: " + authType);
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain,
                                           final String authType) {
                Log.i("info", "authType: " + authType);
            }
        };

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        try {
            final String PROTOCOL = "SSL";
            SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
            KeyManager[] keyManagers = null;
            SecureRandom secureRandom = new SecureRandom();
            sslContext.init(keyManagers, trustManagers, secureRandom);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            okHttpClientBuilder.sslSocketFactory(sslSocketFactory, x509TrustManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        OkHttpClient client = okHttpClientBuilder.build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("number_src", oryorID)
                .addFormDataPart("type", "0")
                .build();

        Request request = new Request.Builder()
                .url("https://oryor.com/oryor2015/ajax-check-product.php")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.i("info","Error");
                Log.i("info",e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.isSuccessful()){
                    spinner.setVisibility(View.INVISIBLE);

                    final String myResponse = response.body().string();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject json = new JSONObject(myResponse);
                                String output = json.get("output").toString();


                                if (output.equals("[]")){
//                                    output_json = new JSONObject("{\"lcnno\": \""+ oryorID +"\"}");
                                    showToast("ไม่พบรายการที่ค้นหา");
                                }
                                else{
                                    output_json = new JSONObject(output.replace("[","")
                                            .replace("]",""));
                                    //Add data to file
                                    addJson(output);
                                    recyclerView.smoothScrollToPosition(0);
//                                adapter.notifyDataSetChanged();
                                    adapter.notifyItemInserted(0);

                                    Log.i("infodata", output_json.toString());
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void initData() {
        String list = loadJSONFromAsset(this);
        addJson(list);
        initRecyclerView();
    }

    private void addJson(String list){
        try {
            JSONArray obj = new JSONArray(list);
            Log.i("DATA",obj.toString());

            for (int i = 0; i < obj.length(); ++i) {
                JSONObject rec = obj.getJSONObject(i);
                String number = rec.getString("lcnno");
                String nameth = rec.getString("productha");
                String type = rec.getString("typepro");
                String status = rec.getString("cncnm");
                String requiredString;
                try{
                    requiredString = status.substring(status.indexOf(">(") + 2, status.indexOf(")<"));
                }catch (Exception e){
                    requiredString = "";
                }

                if(mOryor.size() >= 20){
                    mOryor.remove(mOryor.size()-1);
                    adapter.notifyDataSetChanged();
                }
                mOryor.add(0,new Oryor(number,nameth,type,requiredString));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new ItemTouchHelper(itemTouchHelperCallBack).attachToRecyclerView(recyclerView);
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallBack = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            mOryor.remove(viewHolder.getAdapterPosition());

            adapter.notifyDataSetChanged();

        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX,
                    dY, actionState, isCurrentlyActive);
            View itemView = viewHolder.itemView;
            int backgroundCornerOffset = 20;

            if (dX < 0) { // Swiping to the left
                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop() + 18, itemView.getRight(), itemView.getBottom() - 2);
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0);
            }
            background.draw(c);
        }

    };

    @Override
    protected void onPause() {
        super.onPause();
        closeKeyboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeKeyboard();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void  requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    public void showKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }



}

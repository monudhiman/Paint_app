package com.example.paintapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paintapp.Interface.ToolsListener;
import com.example.paintapp.adabters.ToolsAdabters;
import com.example.paintapp.common.Common;
import com.example.paintapp.model.ToolsItem;
import com.example.paintapp.viewHolder.widget.PaintView;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ToolsListener {

    private static final int REQUEST_PERMISSION = 1001;
    PaintView mPaintView;
    int colorBackground,colorBrush;
    int brushSize,eraserSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTools();


    }


    private void initTools() {

        colorBackground = Color.WHITE;
        colorBrush = Color.BLACK;

        eraserSize = brushSize = 12;
        mPaintView = findViewById(R.id.paint_view);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_tools);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));
        ToolsAdabters toolsAdabters = new ToolsAdabters(loadTools(),this);
        recyclerView.setAdapter(toolsAdabters);
    }

    private List<ToolsItem> loadTools() {
        List<ToolsItem> result =new ArrayList<>();

        result.add(new ToolsItem(R.drawable.ic_baseline_brush_24, Common.BRUSH));
        result.add(new ToolsItem(R.drawable.eraser_white,Common.ERASER));
        result.add(new ToolsItem(R.drawable.ic_baseline_palette_24,Common.COLORS));
        result.add(new ToolsItem(R.drawable.paint_white,Common.BACKGROUND));
        result.add(new ToolsItem(R.drawable.ic_baseline_undo_24,Common.RETURN));
        result.add(new ToolsItem(R.drawable.ic_clear_all,Common.CLEAR));

        return result;
    }

    public void finishPaint(View view) {
        finish();
    }



    public void showFiles(View view) {
        startActivity(new Intent(this, ListFilesAct.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void saveFile(View view) {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_PERMISSION);

        }else {

            try {
                saveBitmap();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private void saveBitmap() throws IOException {

        Bitmap bitmap = mPaintView.getBitmap();
        String file_name = UUID.randomUUID() + ".png";
        OutputStream outputStream;
        boolean saved;
        File folder;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + getString(R.string.app_name));

        }else {
            folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + getString(R.string.app_name));
        }
        if(!folder.exists()){
            folder.mkdirs();
        }

        File image = new File(folder + File.separator + file_name);
        Uri imageUri = Uri.fromFile(image);

        outputStream = new FileOutputStream(image);
        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,file_name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/.png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+File.separator+getString(R.string.app_name));
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);
            outputStream = resolver.openOutputStream(uri);
            saved = bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);

        }else {

            savedPictureToGallery(imageUri);

        }

        if(saved)
        Toast.makeText(this, "Picture saved", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Picture not saved", Toast.LENGTH_SHORT).show();

        outputStream.flush();
        outputStream.close();

    }

    private void savedPictureToGallery(Uri imageUri) {

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(imageUri);
        sendBroadcast(intent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if(requestCode == REQUEST_PERMISSION) {
                try {
                    saveBitmap();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSelected(String name) {

        switch (name){
            case Common.BRUSH:
                mPaintView.disableEraser();
                mPaintView.invalidate();
                showDialogSize(false);
                break;

            case Common.ERASER:
                mPaintView.enableEraser();
                showDialogSize(true);
                break;

            case Common.RETURN:
                mPaintView.returnLastAction();
                break;

            case Common.BACKGROUND:

            case Common.COLORS:
                updateColor(name);
                break;

            case Common.CLEAR:
                mPaintView.clearAll();
                break;

        }

    }

    private void updateColor(final String name) {

        int color;

        if(name.equals(Common.BACKGROUND)){
            color = colorBackground;
        }else {
            color = colorBrush;
        }

        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(color)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(12)
                .setPositiveButton("OK", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int lastSelectedColor, Integer[] allColors) {

                        if(name.equals(Common.BACKGROUND)){
                            colorBackground = lastSelectedColor;
                            mPaintView.setBackgroundColor(colorBackground);
                        }else {
                            colorBrush = lastSelectedColor;
                            mPaintView.setBrushColor(colorBrush);
                        }

                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {



                    }
                }).build()
                .show();
    }

    private void showDialogSize(boolean isEraser) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null, false);

        TextView toolsSelected = view.findViewById(R.id.status_tools_selected);
        TextView statusSize = view.findViewById(R.id.status_size);
        ImageView ivTools = view.findViewById(R.id.iv_tools);
        SeekBar seekBar = view.findViewById(R.id.seekbar_size);
        seekBar.setMax(99);

        if(isEraser){

            toolsSelected.setText("Eraser Size");
            ivTools.setImageResource(R.drawable.eraser_black);
            statusSize.setText("Selected Size: "+eraserSize);

        }else {

            toolsSelected.setText("Brushr Size");
            ivTools.setImageResource(R.drawable.ic_baseline_brush);
            statusSize.setText("Selected Size: "+brushSize);

        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {

                if(isEraser){

                    eraserSize = i+1;
                    statusSize.setText("Selected Size: "+eraserSize);
                    mPaintView.setSizeEraser(eraserSize);

                }else {

                    brushSize = i+1;
                    statusSize.setText("Selected Size: "+brushSize);
                    mPaintView.setSizeBrush(brushSize);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setView(view);
        builder.show();
    }
}
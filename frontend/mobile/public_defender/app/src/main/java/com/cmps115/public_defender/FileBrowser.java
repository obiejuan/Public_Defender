package com.cmps115.public_defender;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cmps115.public_defender.fileUtil.MediaFile;
import com.cmps115.public_defender.fileUtil.MimeUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Author: Payton
 */

public class FileBrowser extends AppCompatActivityWithPDMenu {
    private String path;
    private File dir;
    private List<String> values = new ArrayList<String>();
    private final int PICK_FILE_RESULT_CODE = 999;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the "view" to be the following XML file.
        setContentView(R.layout.file_browser_activity);
        path = getExternalCacheDir().getAbsolutePath();
        Log.d("PATH IS: ", path);

        if (getIntent().hasExtra("path")) {
            path = getIntent().getStringExtra("path");
        }
        setTitle(path);
        dir = new File(path);

        FileFilter filter_wav_file = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().endsWith(".wav") && pathname.canRead()) {
                    return true;
                }
                return false;
            }
        };

        File f_list[] = dir.listFiles(filter_wav_file);
        if (f_list != null) {
            for (File file : f_list) {
                values.add(file.getName());
            }
        }
        Collections.sort(values);
        Collections.reverse(values); //so the newest show up first

        setContentView(R.layout.file_browser_activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        final ListView lv = (ListView)findViewById(android.R.id.list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> a, View v,int position, long id)
            {
                String filename = (String) lv.getAdapter().getItem(position);
                File temp_file = new File(dir, values.get(position));


                if (!temp_file.isFile()) {      // If it's a folder.
                    File list[] = temp_file.listFiles();

                    if (list != null && list.length > 0) {
                        dir = temp_file;
                        values.clear();
                        for (int i = 0; i < list.length; i++) {
                            values.add(list[i].getName());
                        }
                        ArrayAdapter<String> adapt = new ArrayAdapter<>(v.getContext(),
                                android.R.layout.simple_list_item_2, android.R.id.text1, values);
                        lv.setAdapter(adapt);
                    }
                } else {                        // If it's a file.
                    String mimeType = MediaFile.getMimeTypeForFile(temp_file.toString());
                    String fileName = temp_file.getName();
                    int dot_position = fileName.lastIndexOf(".");
                    String file_extension = "";

                    if (dot_position != -1) {
                        String filename_wo_ext = filename.substring(0, dot_position);
                        file_extension = fileName.substring(dot_position + 1, fileName.length());
                    }
                    if (mimeType == null) {
                        mimeType = MimeUtils.guessMimeTypeFromExtension(file_extension);
                    }

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + temp_file), mimeType);
                    ResolveInfo info = getPackageManager().resolveActivity(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        startActivity(Intent.createChooser(intent, "Complete action using"));
                    }
                }
            }
        });
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_FILE_RESULT_CODE: {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    String theFilePath = data.getData().getPath();
                }
                break;
            }
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

}

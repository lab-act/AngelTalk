package angeltalk.plus.presentation.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import angeltalk.plus.data.sqlite.DatabaseHelper;
import angeltalk.plus.presentation.util.ContentsUtil;
import angeltalk.plus.presentation.util.FileUtil;

import static angeltalk.plus.presentation.util.ContentsUtil.getContentFolder;

public class ApplicationInitializer {

    private Context context;

    public ApplicationInitializer(Context context) {
        this.context = context;
    }

    public void initializeApplication() {
        initExternalStorageFolder();
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        databaseHelper.getWritableDatabase();
        if(isFirstLaunched() && isNewInstall()){
            copyDefaultAssetImagesToImageFolder(context);
        }
    }

    private boolean isFirstLaunched(){
        SharedPreferences preferences = context.getSharedPreferences(ApplicationConstants.PRIVATE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(ApplicationConstants.FIRST_LAUNCH, true);
    }
    private boolean isNewInstall(){
        SharedPreferences preferences = context.getSharedPreferences(ApplicationConstants.PRIVATE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(ApplicationConstants.NEW_INSTALL, true);
    }

    private void initExternalStorageFolder() {

        File rootFolder = new File(context.getApplicationContext().getFilesDir() + File.separator + ContentsUtil.ANGELMAN_FOLDER);

        if (!rootFolder.exists()) {
            rootFolder.mkdir();
        }

        File imageFolder = new File(ContentsUtil.getContentFolder(context));

        if (!imageFolder.exists()) {
            imageFolder.mkdir();
        }

        File voiceFolder = new File(ContentsUtil.getVoiceFolder(context));
        if (!voiceFolder.exists()) {
            voiceFolder.mkdir();
        }

        File tempFolder = new File(ContentsUtil.getTempFolder(context));
        if(!tempFolder.exists()) {
            tempFolder.mkdir();
        }
    }

    private void copyDefaultAssetImagesToImageFolder(Context context) {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("contents");
        } catch (IOException e) {
            Log.e("AngelmanApplication", "Failed to get asset file list.", e);
        }
        if (files != null) for (String fileName : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open("contents" + File.separator + fileName);
                File outFile = new File(getContentFolder(context), fileName);
                out = new FileOutputStream(outFile);
                FileUtil.copyFile(in, out);
            } catch(IOException e) {
                Log.e("AngelmanApplication", "Failed to copy asset file: " + fileName, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e("AngelmanApplication", "Failed to close image input file.", e);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e("AngelmanApplication", "Failed to close image output file.", e);
                    }
                }
            }
        }
    }
}

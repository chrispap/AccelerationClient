package vvr.breathrecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import android.os.Environment;
import android.util.Log;

public class Utils {
    private static String LOG_TAG  = "accel.utils";
    private static String DIR_NAME = "notremor";

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean writeToSDFile(String data, String filename, String extension) {
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/" + DIR_NAME);
        dir.mkdirs();

        String flnm = filename + "." + extension;
        File file = new File(dir, flnm);

        int fi = 1;
        while (file.exists()) {
            flnm = filename + fi + "." + extension;
            ++fi;
            file = new File(dir, flnm);
        }

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.print(data);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(LOG_TAG, "File " + flnm + " not found.");
            return false;
        } catch (IOException exc) {
            exc.printStackTrace();
            return false;
        }

        return true;
    }

}

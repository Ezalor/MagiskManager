package com.topjohnwu.magisk.receivers;

import android.net.Uri;

import com.topjohnwu.magisk.R;
import com.topjohnwu.magisk.asyncs.FlashZIP;
import com.topjohnwu.magisk.utils.ByteArrayInOutStream;
import com.topjohnwu.magisk.utils.ZipUtils;

import java.io.OutputStream;

public class RepoDlReceiver extends DownloadReceiver {
    @Override
    public void onDownloadDone(Uri uri) {
        // Flash the zip
        new FlashZIP(activity, uri, mFilename){
            @Override
            protected void preProcessing() throws Throwable {
                // Process and sign the zip
                publishProgress(activity.getString(R.string.zip_install_process_zip_msg));
                ByteArrayInOutStream buffer = new ByteArrayInOutStream();

                // First remove top folder (the folder with the repo name) in Github source zip
                ZipUtils.removeTopFolder(activity.getContentResolver().openInputStream(mUri), buffer);

                // Then sign the zip for the first time
                ZipUtils.signZip(activity, buffer.getInputStream(), buffer, false);

                // Adjust the zip to prevent unzip issues
                ZipUtils.adjustZip(buffer);

                // Finally, sign the whole zip file again
                ZipUtils.signZip(activity, buffer.getInputStream(), buffer, true);

                // Write it back to the downloaded zip
                try (OutputStream out = activity.getContentResolver().openOutputStream(mUri)) {
                    buffer.writeTo(out);
                }
            }
        }.exec();
    }
}

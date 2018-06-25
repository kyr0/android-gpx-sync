package de.aron_homberg.gpxsync.util;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper {

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFileContents(ParcelFileDescriptor parcelFileDescriptor) {

        StringBuilder sb = new StringBuilder();

        try {

            String content;
            FileInputStream fis;
            fis = new FileInputStream(parcelFileDescriptor.getFileDescriptor());

            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            while ((content = br.readLine()) != null) {
                sb.append(content);
            }
            fis.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return sb.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String getISOTimeNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", LocaleList.getDefault().get(0));
        return dateFormat.format(new Date());
    }

    public static Drawable bitmapDataToDrawable(InputStream in, int sizeMax) {

        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            baos.close();

            InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
            InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is1, null, o);

            System.out.println("h:" + o.outHeight + " w:" + o.outWidth);

            int scale = 1;
            if (o.outHeight > sizeMax || o.outWidth > sizeMax) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(sizeMax /
                        (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            o2.inPreferQualityOverSpeed = true;

            return new BitmapDrawable(Resources.getSystem(),BitmapFactory.decodeStream(is2, null, o2));

        } catch (Exception e) {
            return null;
        }
    }
}

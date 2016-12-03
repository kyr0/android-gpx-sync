package de.aron_homberg.gpxsync;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.byteSources.ByteSourceArray;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageParser;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import de.aron_homberg.gpxsync.db.LogEntryPool;
import de.aron_homberg.gpxsync.entities.LogEntry;
import de.aron_homberg.gpxsync.util.Helper;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;


public class AddLogEntryActivity extends AppCompatActivity {

    final String TAG = "AddLogActivity";
    static final int PICK_SNAPSHOT = 1;
    byte[] snapshotImageBlob = null;
    BitmapDrawable snapshot = null;
    protected int trackId;
    protected Location recentLocation;
    String mCurrentPhotoPath = null;
    JpegImageMetadata jpegMetadata = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_log_entry);

        trackId = getIntent().getIntExtra("trackId", 0);

        handleRotateImage();
        handleSnapshotChoose();
        handleSaveNewEntry();
        //triggerLocationUpdates();
    }

    protected void handleRotateImage() {

        Button rotateBtn = (Button) findViewById(R.id.rotateImageButton);
        rotateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            Log.d(TAG, "Rotating drawable");

            // rotate by 90° and replace snapshot BitmapDrawable
            Bitmap image = snapshot.getBitmap();
            Bitmap mutableImage = image.copy(Bitmap.Config.ARGB_8888, true);

            Matrix matrix = new Matrix();
            matrix.setRotate(90, mutableImage.getWidth() / 2, mutableImage.getHeight() / 2);
            Bitmap rotatedImage = Bitmap.createBitmap(mutableImage, 0, 0, mutableImage.getWidth(), mutableImage.getHeight(), matrix, true);

            try {
                exifBitmap(rotatedImage);
            } catch (IOException | ImageReadException | ImageWriteException e) {
                e.printStackTrace();
            }

            // re-render image
            renderDrawable();
            }
        });
    }

    /*
    protected void triggerLocationUpdates() {

        OnLocationUpdatedListener locListener = new OnLocationUpdatedListener() {

            @Override
            public void onLocationUpdated(Location location) {

            recentLocation = location;

            Toast.makeText(AddLogEntryActivity.this, "Location detected now.", Toast.LENGTH_SHORT).show();

            Button saveButton = (Button) findViewById(R.id.saveButton);
            saveButton.setEnabled(true);
        }
    };
        SmartLocation.with(AddLogEntryActivity.this).location().start(locListener);
    }
    */

    protected void handleSaveNewEntry() {

        Button saveButton = (Button) findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (snapshotImageBlob != null) {


                    // fetch data from UI
                    EditText messageEditor = (EditText) findViewById(R.id.messageEditor);
                    String message = messageEditor.getText().toString();

                    // create new entry
                    LogEntry entry = new LogEntry();
                    entry.setGpxTrackId(trackId);
                    entry.setMessage(message);
                    entry.setTime(Helper.getISOTimeNow());
                    entry.setPicture(snapshotImageBlob);

                    // save in DB
                    long entryId = new LogEntryPool(AddLogEntryActivity.this).add(entry);

                    // back to LogsActivity
                    Intent intent = new Intent(AddLogEntryActivity.this, LogsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("BACK", true);
                    intent.putExtra("trackId", trackId);
                    intent.putExtra("entryId", entryId);
                    startActivity(intent);

                } else {

                    Toast.makeText(AddLogEntryActivity.this, "No Image? Cancelling!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected void handleSnapshotChoose() {

        Button chooseSnapshotButton = (Button) findViewById(R.id.chooseSnapshotButton);

        chooseSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_SNAPSHOT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_SNAPSHOT && resultCode == Activity.RESULT_OK) {

            if (data == null) {
                //Display an error
                Log.d(TAG, "No data!");
                return;
            }

            Log.d(TAG, "Image data stored in: " + mCurrentPhotoPath);

            try {

                // make byte[] of image data
                InputStream inputStream = AddLogEntryActivity.this.getContentResolver().openInputStream(data.getData());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > -1 ) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();

                // dup input stream
                InputStream renderInputStream = new ByteArrayInputStream(baos.toByteArray());

                // get metadata from file
                JpegImageParser imageParser = new JpegImageParser();
                IImageMetadata sansMetaData = imageParser.getMetadata(new ByteSourceArray(baos.toByteArray()), null);
                jpegMetadata = (JpegImageMetadata) sansMetaData;

                // scale image
                snapshot = (BitmapDrawable) Helper.bitmapDataToDrawable(renderInputStream, 1024);

                renderDrawable();

                // generate snapshot image
                Bitmap snapshotBitmap = snapshot.getBitmap();

                if (snapshotBitmap != null) {

                    exifBitmap(snapshotBitmap);

                    // enable the rotate button
                    Button rotateBtn = (Button) findViewById(R.id.rotateImageButton);
                    rotateBtn.setEnabled(true);
                }

            } catch (IOException | ImageReadException | ImageWriteException e) {
                e.printStackTrace();
            }
        }
    }

    protected void exifBitmap(Bitmap snapshotBitmap) throws IOException, ImageReadException, ImageWriteException {

        // transform and store snapshot data blob
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        //Log.d(TAG, "jpegMetadata: " + jpegMetadata);

        byte[] imageData = stream.toByteArray();
        //Log.d(TAG, "length of imageData: " + imageData.length);

        if (null != jpegMetadata) {

            TiffImageMetadata exif = jpegMetadata.getExif();
            TiffOutputSet outputSet = exif.getOutputSet();

            // in-memory Exif data update
            ByteArrayOutputStream imageWithExifBos = new ByteArrayOutputStream();
            ExifRewriter metaDataWriter = new ExifRewriter();
            metaDataWriter.updateExifMetadataLossless(imageData, imageWithExifBos, outputSet);

            imageData = imageWithExifBos.toByteArray();
        }

        InputStream convInputStream = new ByteArrayInputStream(imageData);
        BitmapFactory.Options o = new BitmapFactory.Options();

        snapshotImageBlob = imageData;
        snapshot = new BitmapDrawable(BitmapFactory.decodeStream(convInputStream, null, o));
    }

    protected void renderDrawable() {

        // show image
        ImageView previewImageView = (ImageView) findViewById(R.id.previewImageView);
        previewImageView.setImageDrawable(null);
        previewImageView.setImageDrawable(snapshot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_log_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }
}

package de.aron_homberg.gpxsync.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Button;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.aron_homberg.gpxsync.MainActivity;
import de.aron_homberg.gpxsync.R;
import de.aron_homberg.gpxsync.db.LogEntryPool;
import de.aron_homberg.gpxsync.entities.GpxTrack;
import de.aron_homberg.gpxsync.entities.LogEntry;
import de.aron_homberg.gpxsync.service.SyncPosService;
import de.aron_homberg.gpxsync.util.Helper;

public class RemoteApiAdapter {

    static final String BASE_URL = "...";
    static final String TAG = "RemoteApiAdapter";
    static final String API_KEY = "...";
    static final String UPLOAD_URL = BASE_URL + "/sync.php";
    static final String POS_SEND_URL = BASE_URL + "/syncCurrentPos.php";
    static final String LOG_SYNC_URL = BASE_URL + "/syncLog.php";
    static final String GET_LOG_IDS_URL = BASE_URL + "/getLogIds.php";

    protected static boolean precheck(final MainActivity ctx) {

        if (!isOnline(ctx)) {
            ctx.runOnUiThread(new Runnable() {
                public void run() {
                    ctx.toast("Sorry, I'm offline :-(");
                }
            });
            return false;
        }
        return true;
    }


    public static void callSyncTracksApi(final ArrayList<GpxTrack> trackList, final MainActivity ctx) {

        if (!precheck(ctx)) return;

        Log.d(TAG, "Sync tracks: " + trackList.size());

        new Thread(new Runnable() {

            public boolean isAlreadySynced(JSONArray logIds, String logId) {

                for (int i = 0; i < logIds.length(); i++)
                {
                    String logHashId;
                    try {
                        logHashId = logIds.getString(i);

                        if (logHashId.equals(logId)) {
                            return true;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            public void run() {

                ctx.runOnUiThread(new Runnable() {
                    public void run() {
                        ctx.toast("Checking synced...");
                    }
                });

                // sync track
                AsyncHttpClient preClient = new SyncHttpClient();
                preClient.get(GET_LOG_IDS_URL, new RequestParams(), new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, final byte[] response) {


                            try {
                                String json = new String(response, "UTF-8");

                                JSONArray syncedIds = new JSONArray(json);

                                ctx.runOnUiThread(new Runnable() {
                                    public void run() {
                                        ctx.toast("Detected synced log entries!");
                                    }
                                });

                                final LogEntryPool logEntryPool = new LogEntryPool(ctx);

                                ctx.runOnUiThread(new Runnable() {
                                    public void run() {

                                        Button syncButton = (Button) ctx.findViewById(R.id.syncButton);
                                        syncButton.setEnabled(false);
                                    }
                                });

                                for (final GpxTrack t : trackList) {

                                    // sync track
                                    AsyncHttpClient client = new SyncHttpClient();
                                    RequestParams params = new RequestParams();
                                    params.put("apiKey", API_KEY);
                                    params.put("track", t.getHash());

                                    InputStream gpxStream = new ByteArrayInputStream(t.getMarkup().getBytes(StandardCharsets.UTF_8));
                                    params.put("uploaded_file", gpxStream, t.getHash() + ".xml");

                                    client.post(UPLOAD_URL, params, new AsyncHttpResponseHandler() {

                                        @Override
                                        public void onStart() {
                                            // called before request is started
                                        }

                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                                            //ctx.getDbh().deleteGpxTrack(t);

                                            ctx.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    ctx.toast("Upload OK: " + t.getName());
                                                    ctx.populateGPXGrid();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(final int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

                                            ctx.runOnUiThread(new Runnable() {
                                                public void run() {

                                                    ctx.toast("Upload FAILED: " + t.getName() + "; HTTP status: " + statusCode);
                                                    ctx.populateGPXGrid();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onRetry(int retryNo) {
                                            // called when request is retried
                                        }
                                    });

                                    // sync log entries for track
                                    ArrayList<LogEntry> logEntries = (ArrayList<LogEntry>) logEntryPool.getByGpxTrack(t.getId());

                                    for (final LogEntry l : logEntries) {

                                        String logId = Helper.md5(l.getTime());

                                        if (isAlreadySynced(syncedIds, logId)) {

                                            ctx.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    ctx.toast("Log entry already synced: " + l.getMessage());
                                                }
                                            });
                                            continue;
                                        }

                                        RequestParams logParams = new RequestParams();
                                        logParams.put("apiKey", API_KEY);

                                        byte[] pictureBuffer = l.getPicture();

                                        if (pictureBuffer != null) {
                                            InputStream snapshotStream = new ByteArrayInputStream(pictureBuffer);
                                            logParams.put("uploaded_file", snapshotStream, "snapshot_" + logId + ".jpg");
                                        }

                                        logParams.put("id", logId);
                                        logParams.put("day", l.getNr());
                                        logParams.put("message", l.getMessage());
                                        logParams.put("origin", l.getOrigin());
                                        logParams.put("time", l.getTime());
                                        logParams.put("track", t.getHash());
                                        logParams.put("type", l.getType());
                                        logParams.put("lat", l.getLat());
                                        logParams.put("lng", l.getLng());
                                        logParams.put("message_en", l.getMessage_en());

                                        client.post(LOG_SYNC_URL, logParams, new AsyncHttpResponseHandler() {

                                            @Override
                                            public void onStart() {
                                                // called before request is started
                                            }

                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                                                //logEntryPool.delete(l.getId());

                                                ctx.runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ctx.toast("Log entry upload OK: " + l.getMessage());
                                                        ctx.populateGPXGrid();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(final int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

                                                ctx.runOnUiThread(new Runnable() {
                                                    public void run() {

                                                        ctx.toast("Log entry upload FAILED: " + l.getMessage() + "; HTTP status: " + statusCode);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onRetry(int retryNo) {
                                                // called when request is retried
                                            }
                                        });
                                    }
                                }

                                ctx.runOnUiThread(new Runnable() {
                                    public void run() {

                                        Button syncButton = (Button) ctx.findViewById(R.id.syncButton);
                                        syncButton.setEnabled(true);
                                    }
                                });


                            } catch (UnsupportedEncodingException | JSONException e1) {
                                e1.printStackTrace();
                            }
                    }

                    @Override
                    public void onFailure(final int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

                        ctx.runOnUiThread(new Runnable() {
                            public void run() {

                                ctx.toast("Failed sync precheck.");
                            }
                        });
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });


            }
        }).start();
    }

    public static void callSyncCurrentPosApi(final double latitude, final double longitude, final SyncPosService ctx) {

        if (!isOnline(ctx)) {
            ctx.onFailedLocationUpdate("Device offline", -1);
            return;
        }

        Log.d(TAG, "Sending lat,lng: " + latitude + "," + longitude);

        new Thread(new Runnable() {
            public void run() {

                AsyncHttpClient client = new SyncHttpClient();
                RequestParams params = new RequestParams();
                params.put("apiKey", API_KEY);
                params.put("lat", latitude);
                params.put("long", longitude);

                client.post(POS_SEND_URL, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                        ctx.onSuccessLocationUpdate();
                    }

                    @Override
                    public void onFailure(final int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

                        ctx.onFailedLocationUpdate("HTTP request failed", statusCode);
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });
            }
        }).start();
    }

    public static void callSyncCurrentPosApi(final double latitude, final double longitude, final MainActivity ctx) {

        if (!isOnline(ctx)) {
            ctx.onFailedLocationUpdate("Device offline", -1);
            return;
        }

        Log.d(TAG, "Sending lat,lng: " + latitude + "," + longitude);

        new Thread(new Runnable() {
            public void run() {

                AsyncHttpClient client = new SyncHttpClient();
                RequestParams params = new RequestParams();
                params.put("apiKey", API_KEY);
                params.put("lat", latitude);
                params.put("long", longitude);

                client.post(POS_SEND_URL, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                        ctx.onSuccessLocationUpdate();
                    }

                    @Override
                    public void onFailure(final int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

                        ctx.onFailedLocationUpdate("HTTP request failed", statusCode);
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });
            }
        }).start();
    }

    public static boolean isOnline(Context ctx) {

        ConnectivityManager conMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }
}

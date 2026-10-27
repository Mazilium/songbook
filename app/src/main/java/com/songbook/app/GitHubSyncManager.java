package com.songbook.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitHubSyncManager {

    private static final String PREF_NAME = "songbook_prefs";
    private static final String KEY_GITHUB_URL = "github_songs_url";

    public static final String DEFAULT_URL = "https://raw.githubusercontent.com/Mazilium/songbook/main/songs.json";

    public interface SyncCallback {
        void onSuccess(SongStorage.SyncReport report);
        void onError(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String getSavedUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_GITHUB_URL, DEFAULT_URL);
    }

    public static void saveUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GITHUB_URL, url.trim()).apply();
    }

    public static void syncSongsFromGitHub(Context context, List<Song> currentSongs, SyncCallback callback) {
        String baseTargetUrl = getSavedUrl(context);

        // Convert github.com blob/raw web links to real raw.githubusercontent.com if needed
        if (baseTargetUrl.contains("github.com") && baseTargetUrl.contains("/blob/")) {
            baseTargetUrl = baseTargetUrl.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/");
        }

        // Add cache-busting timestamp query parameter so GitHub CDN never serves stale cache
        String targetUrl = baseTargetUrl + (baseTargetUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                conn.setRequestProperty("Pragma", "no-cache");
                conn.setRequestProperty("User-Agent", "Songbook-Android-App");

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Помилка сервера HTTP " + responseCode);
                }

                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                String rawJson = sb.toString().trim();

                // Auto-repair common syntax typos:
                // 1. Missing comma between song objects: } { -> }, {
                rawJson = rawJson.replaceAll("\\}\\s*\\{", "},\n{");
                // 2. Trailing comma before closing bracket: , ] -> ]
                rawJson = rawJson.replaceAll(",\\s*\\]", "\n]");

                JSONArray jsonArray = new JSONArray(rawJson);
                List<Song> remoteSongs = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    remoteSongs.add(Song.fromJsonObject(obj));
                }

                // Complete Mirror Sync: additions, deletions, updates
                SongStorage.SyncReport report = SongStorage.syncWithRemoteSongs(context, currentSongs, remoteSongs);

                mainHandler.post(() -> callback.onSuccess(report));

            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "Не вдалося з'єднатися з GitHub";
                if (errMsg.contains("Unterminated array")) {
                    errMsg = "Пропущено кому між піснями у файлі songs.json або незакриту дужку ']'";
                }
                final String finalMsg = errMsg;
                mainHandler.post(() -> callback.onError(finalMsg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}

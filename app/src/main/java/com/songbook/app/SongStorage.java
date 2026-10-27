package com.songbook.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SongStorage {

    private static final String FILE_NAME = "songs_cache.json";

    public static class SyncReport {
        public int added = 0;
        public int removed = 0;
        public int total = 0;
    }

    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);

        if (!file.exists()) {
            songs = SongRepository.getSongs();
            saveSongs(context, songs);
            return songs;
        }

        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                songs.add(Song.fromJsonObject(obj));
            }
        } catch (Exception e) {
            songs = SongRepository.getSongs();
        }

        return songs;
    }

    public static void saveSongs(Context context, List<Song> songs) {
        try {
            JSONArray arr = new JSONArray();
            for (Song s : songs) {
                arr.put(s.toJsonObject());
            }

            try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
                fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Mirrors the remote list from GitHub into the app:
     * - Deletes songs that were removed from GitHub.
     * - Adds newly added songs.
     * - Updates modified lyrics or keys.
     */
    public static SyncReport syncWithRemoteSongs(Context context, List<Song> currentSongs, List<Song> remoteSongs) {
        SyncReport report = new SyncReport();

        if (remoteSongs == null || remoteSongs.isEmpty()) {
            report.total = currentSongs.size();
            return report;
        }

        // Count removals (songs present in app but not in remote)
        for (Song cur : currentSongs) {
            boolean existsInRemote = false;
            for (Song rem : remoteSongs) {
                if (cur.getTitle().trim().equalsIgnoreCase(rem.getTitle().trim()) &&
                    cur.getAuthor().trim().equalsIgnoreCase(rem.getAuthor().trim())) {
                    existsInRemote = true;
                    break;
                }
            }
            if (!existsInRemote) {
                report.removed++;
            }
        }

        // Count additions (songs present in remote but not in app)
        for (Song rem : remoteSongs) {
            boolean existsInCurrent = false;
            for (Song cur : currentSongs) {
                if (rem.getTitle().trim().equalsIgnoreCase(cur.getTitle().trim()) &&
                    rem.getAuthor().trim().equalsIgnoreCase(cur.getAuthor().trim())) {
                    existsInCurrent = true;
                    break;
                }
            }
            if (!existsInCurrent) {
                report.added++;
            }
        }

        // Exact mirror update: replace current list with remote list
        currentSongs.clear();
        currentSongs.addAll(remoteSongs);

        saveSongs(context, currentSongs);
        report.total = currentSongs.size();

        return report;
    }
}

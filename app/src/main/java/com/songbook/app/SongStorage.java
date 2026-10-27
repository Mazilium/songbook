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

    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);

        if (!file.exists()) {
            // First run: load initial songs and persist them to cache
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
            // Fallback to default bundled songs
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
     * Merges newly downloaded songs into the existing list.
     * Returns the number of brand new songs added.
     */
    public static int mergeNewSongs(Context context, List<Song> currentSongs, List<Song> remoteSongs) {
        int addedCount = 0;

        for (Song remoteSong : remoteSongs) {
            boolean exists = false;
            for (Song current : currentSongs) {
                if (current.getTitle().trim().equalsIgnoreCase(remoteSong.getTitle().trim()) &&
                    current.getAuthor().trim().equalsIgnoreCase(remoteSong.getAuthor().trim())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                currentSongs.add(remoteSong);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            saveSongs(context, currentSongs);
        }

        return addedCount;
    }
}

package com.songbook.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Song {
    private final String title;
    private final String author;
    private final String defaultKey;
    private final String content;

    public Song(String title, String author, String defaultKey, String content) {
        this.title = title;
        this.author = author;
        this.defaultKey = defaultKey;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDefaultKey() { return defaultKey; }
    public String getContent() { return content; }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("author", author);
            obj.put("defaultKey", defaultKey);
            obj.put("content", content);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static Song fromJsonObject(JSONObject obj) {
        String title = obj.optString("title", "Без назви");
        String author = obj.optString("author", "Невідомий");
        String defaultKey = obj.optString("defaultKey", "Am");
        String content = obj.optString("content", "");
        return new Song(title, author, defaultKey, content);
    }
}

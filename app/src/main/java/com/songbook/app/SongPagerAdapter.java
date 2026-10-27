package com.songbook.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongPagerAdapter extends RecyclerView.Adapter<SongPagerAdapter.SongViewHolder> {

    private final List<Song> songs;
    private float fontSizeSp = 16.0f;
    private int transposeSemitones = 0;

    public SongPagerAdapter(List<Song> songs) {
        this.songs = songs;
    }

    public void setFontSizeSp(float size) {
        this.fontSizeSp = size;
        notifyDataSetChanged();
    }

    public float getFontSizeSp() {
        return fontSizeSp;
    }

    public void setTransposeSemitones(int semitones) {
        this.transposeSemitones = semitones;
        notifyDataSetChanged();
    }

    public int getTransposeSemitones() {
        return transposeSemitones;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_page, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvAuthor.setText(song.getAuthor());

        String key = ChordHelper.transposeChord(song.getDefaultKey(), transposeSemitones);
        holder.tagKey.setText("Тон: " + key);

        holder.tvPageNumber.setText(String.format("— Стор. %d з %d —", position + 1, songs.size()));

        holder.tvContent.setTextSize(fontSizeSp);
        holder.tvContent.setText(ChordHelper.formatLyricsWithChords(song.getContent(), transposeSemitones));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tagKey, tvContent, tvPageNumber;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvAuthor = itemView.findViewById(R.id.tvSongAuthor);
            tagKey = itemView.findViewById(R.id.tagKey);
            tvContent = itemView.findViewById(R.id.tvSongContent);
            tvPageNumber = itemView.findViewById(R.id.tvPageNumber);
        }
    }
}

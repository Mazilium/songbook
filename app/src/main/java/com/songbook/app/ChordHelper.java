package com.songbook.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChordHelper {

    private static final String[] NOTES_SHARP = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] NOTES_FLAT  = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

    public static String transposeChord(String chord, int semitones) {
        if (semitones == 0 || chord.isEmpty()) return chord;

        // Extract base root note (e.g. C, C#, Bb)
        String root = "";
        String suffix = "";

        if (chord.length() >= 2 && (chord.charAt(1) == '#' || chord.charAt(1) == 'b')) {
            root = chord.substring(0, 2);
            suffix = chord.substring(2);
        } else {
            root = chord.substring(0, 1);
            suffix = chord.substring(1);
        }

        int index = -1;
        for (int i = 0; i < 12; i++) {
            if (NOTES_SHARP[i].equalsIgnoreCase(root) || NOTES_FLAT[i].equalsIgnoreCase(root)) {
                index = i;
                break;
            }
        }

        if (index == -1) return chord; // Not recognized, return as is

        int newIndex = (index + semitones) % 12;
        if (newIndex < 0) newIndex += 12;

        return NOTES_SHARP[newIndex] + suffix;
    }

    public static SpannableStringBuilder formatLyricsWithChords(String rawContent, int semitones) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        Pattern pattern = Pattern.compile("\\[([A-Ga-g][#b]?[a-zA-Z0-9/]*)\\]");

        String[] lines = rawContent.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.startsWith("Приспів:")) {
                int start = ssb.length();
                ssb.append("♪ Приспів:\n");
                ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#92400E")), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) {
                ssb.append(line).append("\n");
                continue;
            }

            // Line has chords! Let's display chords clearly
            // We can replace [Chord] with transposed and styled Chord
            matcher.reset();
            int lastEnd = 0;

            while (matcher.find()) {
                // Append text before chord
                ssb.append(line.substring(lastEnd, matcher.start()));

                // Transpose and format chord
                String originalChord = matcher.group(1);
                String chord = transposeChord(originalChord, semitones);

                int chordStart = ssb.length();
                ssb.append(" ").append(chord).append(" ");

                // Style chord
                ssb.setSpan(new StyleSpan(Typeface.BOLD), chordStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#C2410C")), chordStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                lastEnd = matcher.end();
            }

            if (lastEnd < line.length()) {
                ssb.append(line.substring(lastEnd));
            }
            ssb.append("\n");
        }

        return ssb;
    }
}

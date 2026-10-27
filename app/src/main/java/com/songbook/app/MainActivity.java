package com.songbook.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SongPagerAdapter adapter;
    private List<Song> songList;

    private TextView tvBottomPageIndicator;
    private ImageButton btnSoundToggle, btnSync;
    private Button btnPrevPage, btnNextPage;

    private SoundPool soundPool;
    private int soundPageFlip = 0;
    private boolean isSoundEnabled = true;
    private int lastPageIndex = 0;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        initSoundPool();

        // Load cached songs or defaults
        songList = SongStorage.loadSongs(this);
        adapter = new SongPagerAdapter(songList);

        viewPager = findViewById(R.id.viewPagerSongs);
        viewPager.setAdapter(adapter);
        viewPager.setPageTransformer(new BookPageTransformer());

        tvBottomPageIndicator = findViewById(R.id.tvBottomPageIndicator);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);
        btnSync = findViewById(R.id.btnSync);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);

        setupListeners();
        updatePageIndicator(0);
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();

        soundPageFlip = soundPool.load(this, R.raw.page_flip, 1);
    }

    private void playPageFlipSound() {
        if (isSoundEnabled && soundPageFlip != 0) {
            soundPool.play(soundPageFlip, 0.9f, 0.9f, 1, 0, 1.0f);
        }
        provideHapticFeedback();
    }

    private void provideHapticFeedback() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(12);
            }
        }
    }

    private void setupListeners() {
        // Page change listener to trigger rustling sound and update indicators
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position != lastPageIndex) {
                    playPageFlipSound();
                    lastPageIndex = position;
                }
                updatePageIndicator(position);
            }
        });

        // Bottom navigation buttons
        btnPrevPage.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1, true);
            }
        });

        btnNextPage.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < songList.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            }
        });

        // Sync with GitHub button
        btnSync.setOnClickListener(v -> triggerGitHubSync());

        // Table of contents
        findViewById(R.id.btnContents).setOnClickListener(v -> showContentsDialog());

        // Quick font zoom in / out
        findViewById(R.id.btnZoomIn).setOnClickListener(v -> changeFontSize(1.5f));
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> changeFontSize(-1.5f));

        // Sound toggle
        btnSoundToggle.setOnClickListener(v -> toggleSound());

        // Full Settings Dialog
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsDialog());
    }

    private void triggerGitHubSync() {
        provideHapticFeedback();
        Toast.makeText(this, "🔄 З'єднання з GitHub... Перевірка нових пісень", Toast.LENGTH_SHORT).show();

        GitHubSyncManager.syncSongsFromGitHub(this, songList, new GitHubSyncManager.SyncCallback() {
            @Override
            public void onSuccess(int addedCount, int totalCount) {
                adapter.notifyDataSetChanged();
                updatePageIndicator(viewPager.getCurrentItem());

                if (addedCount > 0) {
                    playPageFlipSound();
                    Toast.makeText(MainActivity.this, "🎉 Додано " + addedCount + " нових пісень! Всього в книзі: " + totalCount, Toast.LENGTH_LONG).show();
                    // Flip to the newly added song!
                    viewPager.setCurrentItem(totalCount - addedCount, true);
                } else {
                    Toast.makeText(MainActivity.this, "✓ Усі пісні з GitHub уже завантажені (всього: " + totalCount + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Помилка оновлення: " + errorMessage + "\n(Додаток працює на збережених піснях)", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePageIndicator(int position) {
        tvBottomPageIndicator.setText(String.format("Стор. %d з %d", position + 1, songList.size()));
        btnPrevPage.setEnabled(position > 0);
        btnNextPage.setEnabled(position < songList.size() - 1);
    }

    private void toggleSound() {
        isSoundEnabled = !isSoundEnabled;
        if (isSoundEnabled) {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            Toast.makeText(this, "Шелест сторінок: Увімкнено 🔊", Toast.LENGTH_SHORT).show();
            playPageFlipSound();
        } else {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
            Toast.makeText(this, "Шелест сторінок: Вимкнено 🔇", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeFontSize(float delta) {
        float newSize = Math.max(12.0f, Math.min(26.0f, adapter.getFontSizeSp() + delta));
        adapter.setFontSizeSp(newSize);
        Toast.makeText(this, String.format("Розмір шрифту: %.0f sp", newSize), Toast.LENGTH_SHORT).show();
    }

    private void showContentsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📖 Зміст пісенника");

        String[] titles = new String[songList.size()];
        for (int i = 0; i < songList.size(); i++) {
            Song s = songList.get(i);
            titles[i] = (i + 1) + ". " + s.getTitle() + " (" + s.getAuthor() + ")";
        }

        builder.setItems(titles, (dialog, which) -> {
            viewPager.setCurrentItem(which, true);
        });

        builder.setPositiveButton("Закрити", null);
        builder.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Налаштування пісенника");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        TextView tvFontVal = dialogView.findViewById(R.id.tvSettingFontSize);
        SeekBar sbFont = dialogView.findViewById(R.id.sbFontSize);

        int currentFont = (int) adapter.getFontSizeSp();
        tvFontVal.setText(currentFont + " sp");
        sbFont.setProgress(currentFont - 12);

        sbFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = progress + 12;
                tvFontVal.setText(size + " sp");
                adapter.setFontSizeSp(size);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Transpose Controls
        TextView tvTransposeVal = dialogView.findViewById(R.id.tvTransposeVal);
        int currentTranspose = adapter.getTransposeSemitones();
        tvTransposeVal.setText((currentTranspose > 0 ? "+" : "") + currentTranspose);

        dialogView.findViewById(R.id.btnTransposeMinus).setOnClickListener(v -> {
            int t = adapter.getTransposeSemitones() - 1;
            adapter.setTransposeSemitones(t);
            tvTransposeVal.setText((t > 0 ? "+" : "") + t);
        });

        dialogView.findViewById(R.id.btnTransposePlus).setOnClickListener(v -> {
            int t = adapter.getTransposeSemitones() + 1;
            adapter.setTransposeSemitones(t);
            tvTransposeVal.setText((t > 0 ? "+" : "") + t);
        });

        // Sound switch
        androidx.appcompat.widget.SwitchCompat swSound = dialogView.findViewById(R.id.switchSound);
        swSound.setChecked(isSoundEnabled);
        swSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSoundEnabled = isChecked;
            if (isSoundEnabled) {
                btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                playPageFlipSound();
            } else {
                btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
            }
        });

        // GitHub URL input
        EditText etGitHubUrl = dialogView.findViewById(R.id.etGitHubUrl);
        etGitHubUrl.setText(GitHubSyncManager.getSavedUrl(this));

        dialogView.findViewById(R.id.btnSyncNow).setOnClickListener(v -> {
            String newUrl = etGitHubUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                GitHubSyncManager.saveUrl(this, newUrl);
            }
            triggerGitHubSync();
        });

        builder.setPositiveButton("Готово", (dialog, which) -> {
            String newUrl = etGitHubUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                GitHubSyncManager.saveUrl(this, newUrl);
            }
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}

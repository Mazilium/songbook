package com.songbook.app;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class BookPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        page.setCameraDistance(20000);

        if (position < -1) { // [-Infinity, -1)
            page.setAlpha(0f);
        } else if (position <= 0) { // [-1, 0]: Page turning to the left
            page.setAlpha(1f);
            page.setPivotX(page.getWidth());
            page.setPivotY(page.getHeight() * 0.5f);
            
            // 3D rotation on Y axis like a turning book page
            float rotation = position * 65f;
            page.setRotationY(rotation);

            // Subtle depth scale
            float scale = 1.0f + position * 0.08f;
            page.setScaleX(scale);
            page.setScaleY(scale);

            // Shadow / dim effect as page turns away
            page.setTranslationX(-position * (page.getWidth() * 0.15f));

        } else if (position <= 1) { // (0, 1]: Page emerging from the right
            page.setAlpha(1f);
            page.setPivotX(0);
            page.setPivotY(page.getHeight() * 0.5f);

            float rotation = position * -30f;
            page.setRotationY(rotation);

            float scale = 1.0f - position * 0.05f;
            page.setScaleX(scale);
            page.setScaleY(scale);

            page.setTranslationX(0);
        } else { // (1, +Infinity]
            page.setAlpha(0f);
        }
    }
}

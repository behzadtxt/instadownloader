
package com.example.instadownloader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// The compatibility artifact keeps the familiar YoutubeDL Java API.
import dev.ffmpegkit_maintained.ytdlp.YtDlp;
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest;

public class MainActivity extends Activity {
    private EditText urlInput;
    private TextView status;
    private Button downloadButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(24, 28, 58));
        getWindow().setNavigationBarColor(Color.rgb(24, 28, 58));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(22), dp(34), dp(22), dp(22));
        page.setBackgroundColor(Color.rgb(246, 247, 252));

        TextView title = new TextView(this);
        title.setText("Insta Downloader");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(37, 42, 91));
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView subtitle = new TextView(this);
        subtitle.setText("دانلود عکس و ویدیو از لینک‌های عمومی اینستاگرام");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(54)));

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        urlInput.setHint("https://www.instagram.com/reel/...");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setPadding(dp(14), 0, dp(14), 0);
        urlInput.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(54));
        inputParams.topMargin = dp(24);
        page.addView(urlInput, inputParams);

        Button paste = new Button(this);
        paste.setText("چسباندن لینک");
        page.addView(paste, new LinearLayout.LayoutParams(-1, dp(48)));
        paste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) urlInput.setText(item.getText().toString());
            }
        });

        downloadButton = new Button(this);
        downloadButton.setText("دانلود");
        downloadButton.setTextColor(Color.WHITE);
        downloadButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(80, 72, 170)));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(54));
        buttonParams.topMargin = dp(12);
        page.addView(downloadButton, buttonParams);

        status = new TextView(this);
        status.setText("فایل‌های دانلودشده در پوشه برنامه ذخیره می‌شوند.");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(70, 70, 85));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(100));
        statusParams.topMargin = dp(20);
        page.addView(status, statusParams);

        TextView note = new TextView(this);
        note.setText("توجه: لینک‌های خصوصی، استوری‌های محدود و محتوای نیازمند ورود ممکن است دانلود نشوند. فقط محتوایی را ذخیره کن که اجازه‌اش را داری.");
        note.setTextSize(12);
        note.setTextColor(Color.GRAY);
        note.setGravity(Gravity.CENTER);
        page.addView(note, new LinearLayout.LayoutParams(-1, dp(90)));

        setContentView(page);

        downloadButton.setOnClickListener(v -> startDownload());
        executor.execute(() -> {
            try {YtDlp.init(getApplicationContext()); }
            catch (Exception e) { runOnUiThread(() -> status.setText("راه‌اندازی موتور دانلود ناموفق بود: " + e.getMessage())); }
        });
    }

    private void startDownload() {
        final String url = urlInput.getText().toString().trim();
        if (!(url.startsWith("https://www.instagram.com/") || url.startsWith("https://instagram.com/"))) {
            status.setText("لطفاً یک لینک معتبر اینستاگرام وارد کن.");
            return;
        }
        downloadButton.setEnabled(false);
        status.setText("در حال آماده‌سازی دانلود...");
        executor.execute(() -> {
            try {
                File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Instagram");
                if (!folder.exists() && !folder.mkdirs()) throw new Exception("ساخت پوشه دانلود ممکن نشد");
                String output = new File(folder, "%(title).80s_%(id)s.%(ext)s").getAbsolutePath();
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("-o", output);
                request.addOption("-f", "best[ext=mp4]/best");
                request.addOption("--no-playlist");
                YoutubeDL.getInstance().execute(request);
                runOnUiThread(() -> status.setText("دانلود تمام شد. مسیر ذخیره: Android/data/com.example.instadownloader/files/Download/Instagram"));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "خطای نامشخص" : e.getMessage();
                runOnUiThread(() -> status.setText("دانلود ناموفق بود: " + msg + "\nممکن است اینستاگرام ورود یا دسترسی دیگری بخواهد."));
            } finally {
                runOnUiThread(() -> downloadButton.setEnabled(true));
            }
        });
    }

    @Override protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}

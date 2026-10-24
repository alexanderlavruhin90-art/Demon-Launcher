package com.demon.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int COUNT = 24;
    private final List<AppEntry> applications = new ArrayList<>();
    private SharedPreferences preferences;
    private LinearLayout gridHost;

    private static class AppEntry {
        final String id;
        final String label;
        final ComponentName component;
        final Drawable icon;

        AppEntry(String id, String label, ComponentName component, Drawable icon) {
            this.id = id;
            this.label = label;
            this.component = component;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10, 3, 6));
        getWindow().setNavigationBarColor(Color.rgb(10, 3, 6));
        preferences = getSharedPreferences("demon_slots", MODE_PRIVATE);
        reloadApps();
        initializeSlots();
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null && gridHost != null) {
            reloadApps();
            drawSlots();
        }
    }

    private int dp(float units) {
        return (int) (getResources().getDisplayMetrics().density * units + 0.5f);
    }

    private void reloadApps() {
        PackageManager manager = getPackageManager();
        Intent launcherQuery = new Intent(Intent.ACTION_MAIN);
        launcherQuery.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> matches = manager.queryIntentActivities(launcherQuery, 0);
        Map<String, AppEntry> unique = new LinkedHashMap<>();
        for (ResolveInfo r : matches) {
            if (r.activityInfo == null) continue;
            String pkg = r.activityInfo.packageName;
            if (getPackageName().equals(pkg)) continue;
            ComponentName component = new ComponentName(pkg, r.activityInfo.name);
            String id = component.flattenToString();
            String label = String.valueOf(r.loadLabel(manager));
            try {
                Drawable icon = r.loadIcon(manager);
                unique.put(id, new AppEntry(id, label, component, icon));
            } catch (Exception ignored) { }
        }
        applications.clear();
        applications.addAll(unique.values());
        Collections.sort(applications, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
    }

    private AppEntry findId(String id) {
        for (AppEntry entry : applications) if (entry.id.equals(id)) return entry;
        return null;
    }

    private void initializeSlots() {
        if (preferences.getBoolean("initialized", false)) return;
        String[] priorities = {
            "Telegram", "ChatGPT", "Госуслуги", "Яндекс Go", "Underlords"
        };
        List<AppEntry> sorted = new ArrayList<>();
        Set<String> found = new HashSet<>();
        for (String name : priorities) {
            for (AppEntry e : applications) {
                String text = e.label.toLowerCase(Locale.ROOT);
                String pkg = e.component.getPackageName().toLowerCase(Locale.ROOT);
                boolean match = text.contains(name.toLowerCase(Locale.ROOT))
                    || (name.equals("Underlords") && pkg.contains("underlords"))
                    || (name.equals("Яндекс Go") && pkg.equals("ru.yandex.taxi"))
                    || (name.equals("Госуслуги") && pkg.contains("gosuslugi"));
                if (match && found.add(e.id)) {
                    sorted.add(e);
                    break;
                }
            }
        }
        for (AppEntry e : applications) if (found.add(e.id)) sorted.add(e);
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < COUNT && i < sorted.size(); i++) {
            editor.putString("cell_" + i, sorted.get(i).id);
        }
        editor.putBoolean("initialized", true).apply();
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        return t;
    }

    private void buildScreen() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new DemonBackground(this),
            new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(10), dp(8), dp(10), dp(9));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        TextView brand = text("D E M O N   L A U N C H E R", 14, 0xffff7435);
        brand.setLetterSpacing(0.13f);
        brand.setShadowLayer(dp(10), 0, 0, 0xfffa2b04);
        page.addView(brand, new LinearLayout.LayoutParams(-1, dp(31)));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(0xffffe8dd);
        clock.setTextSize(40);
        clock.setGravity(Gravity.CENTER);
        clock.setShadowLayer(dp(13), 0, 0, 0xffff3200);
        page.addView(clock, new LinearLayout.LayoutParams(-1, dp(51)));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextColor(0xfff4aa85);
        date.setTextSize(12);
        date.setGravity(Gravity.CENTER);
        page.addView(date, new LinearLayout.LayoutParams(-1, dp(24)));

        TextView search = text("⌕   НАЙТИ ПРИЛОЖЕНИЕ", 13, 0xffffad8a);
        search.setBackground(makePill(0xcc1b080d, 0xff9d2c13));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(43));
        searchParams.setMargins(dp(13), dp(9), dp(13), dp(4));
        page.addView(search, searchParams);
        search.setOnClickListener(v -> showPicker(-1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        gridHost = new LinearLayout(this);
        gridHost.setGravity(Gravity.CENTER);
        gridHost.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(gridHost, new ScrollView.LayoutParams(-1, -2));
        drawSlots();

        TextView drawerButton = text("🔥   ВСЕ ПРИЛОЖЕНИЯ   🔥", 15, Color.WHITE);
        drawerButton.setTypeface(null, 1);
        drawerButton.setBackground(makePill(0xe02f0b0a, 0xffec4b17));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(-1, dp(48));
        menuParams.setMargins(dp(18), dp(5), dp(18), dp(4));
        page.addView(drawerButton, menuParams);
        drawerButton.setOnClickListener(v -> showPicker(-1));

        TextView defaultButton = text("Сделать основным экраном", 11, 0xffe7a089);
        page.addView(defaultButton, new LinearLayout.LayoutParams(-1, dp(28)));
        defaultButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (Exception error) {
                Toast.makeText(this, "Откройте настройки приложений по умолчанию", Toast.LENGTH_LONG).show();
            }
        });

        setContentView(root);
    }

    private Drawable makePill(int fill, int outline) {
        android.graphics.drawable.GradientDrawable drawable =
            new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(23));
        drawable.setStroke(dp(1), outline);
        return drawable;
    }

    private void drawSlots() {
        if (gridHost == null) return;
        gridHost.removeAllViews();
        for (int row = 0; row < 6; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER);
            line.setOrientation(LinearLayout.HORIZONTAL);
            gridHost.addView(line, new LinearLayout.LayoutParams(-1, dp(84)));
            for (int column = 0; column < 4; column++) {
                final int position = row * 4 + column;
                String assigned = preferences.getString("cell_" + position, "");
                final AppEntry entry = findId(assigned);

                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                line.addView(tile, new LinearLayout.LayoutParams(0, -1, 1f));

                FrameLayout medallion = new FrameLayout(this);
                medallion.setBackground(new EmberCircle(position));
                tile.addView(medallion, new LinearLayout.LayoutParams(dp(60), dp(60)));

                if (entry != null) {
                    ImageView icon = new ImageView(this);
                    icon.setImageDrawable(entry.icon);
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER);
                    medallion.addView(icon, ip);
                } else {
                    TextView plus = text("+", 27, 0xffff9b4d);
                    medallion.addView(plus, new FrameLayout.LayoutParams(-1, -1));
                }

                TextView label = text(entry == null ? "Добавить" : entry.label, 10, 0xffffe9df);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setShadowLayer(dp(3), 0, 0, Color.BLACK);
                label.setPadding(dp(2), 0, dp(2), 0);
                tile.addView(label, new LinearLayout.LayoutParams(-1, dp(20)));
                tile.setOnClickListener(v -> {
                    if (entry != null) openApp(entry);
                    else showPicker(position);
                });
                tile.setOnLongClickListener(v -> {
                    showPicker(position);
                    return true;
                });
            }
        }
    }

    private void openApp(AppEntry entry) {
        try {
            Intent intent = Intent.makeMainActivity(entry.component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception problem) {
            Toast.makeText(this, "Не удалось открыть " + entry.label, Toast.LENGTH_SHORT).show();
            reloadApps();
            drawSlots();
        }
    }

    private void showPicker(int slot) {
        final List<AppEntry> filtered = new ArrayList<>(applications);
        LinearLayout body = new LinearLayout(this);
        body.setPadding(dp(12), dp(8), dp(12), dp(8));
        body.setOrientation(LinearLayout.VERTICAL);

        EditText search = new EditText(this);
        search.setSingleLine();
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xffaa827a);
        search.setHint("Поиск по приложениям");
        search.setTextSize(16);
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(54)));

        ListView list = new ListView(this);
        final BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return filtered.size(); }
            @Override public Object getItem(int i) { return filtered.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View recycled, ViewGroup parent) {
                AppEntry e = filtered.get(i);
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), 0, dp(8), 0);
                ImageView image = new ImageView(MainActivity.this);
                image.setImageDrawable(e.icon);
                row.addView(image, new LinearLayout.LayoutParams(dp(40), dp(40)));
                TextView name = text(e.label, 15, Color.WHITE);
                name.setGravity(Gravity.CENTER_VERTICAL);
                name.setPadding(dp(12), 0, 0, 0);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(name, new LinearLayout.LayoutParams(0, -1, 1f));
                row.setMinimumHeight(dp(57));
                return row;
            }
        };
        list.setAdapter(adapter);
        body.addView(list, new LinearLayout.LayoutParams(-1, dp(400)));
        final AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(slot >= 0 ? "Назначить приложение" : "Все приложения")
            .setView(body)
            .setNegativeButton("ЗАКРЫТЬ", null)
            .create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry selected = filtered.get(position);
            if (slot >= 0) {
                preferences.edit().putString("cell_" + slot, selected.id).apply();
                drawSlots();
            } else {
                openApp(selected);
            }
            dialog.dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                filtered.clear();
                for (AppEntry e : applications) {
                    if (e.label.toLowerCase(Locale.ROOT).contains(q)) filtered.add(e);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        dialog.show();
    }

    private static class EmberCircle extends Drawable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int seed;
        EmberCircle(int seed) { this.seed = seed; }
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect r = getBounds();
            float cx = r.exactCenterX(), cy = r.exactCenterY();
            float rad = Math.min(r.width(), r.height()) * 0.49f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(cx, cy, rad,
                new int[]{0xdd2c0708, 0xf5180608, 0xaaed4d0b, 0x007f1805},
                new float[]{0f, .64f, .80f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, rad, p);
            p.setShader(null);
            p.setColor(seed % 3 == 0 ? 0xffff9b44 : 0xffd34820);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1.2f, rad * .055f));
            canvas.drawCircle(cx, cy, rad * .75f, p);
            p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int value) { p.setAlpha(value); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { p.setColorFilter(filter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private static class DemonBackground extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator animator;
        private float pulse = 0f;
        DemonBackground(Activity context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(3600);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(a -> {
                pulse = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }
        @Override protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            super.onDetachedFromWindow();
        }
        private Path shape(float... xy) {
            Path path = new Path();
            path.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
            path.close();
            return path;
        }
        private void fill(Canvas canvas, int color, Path path) {
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            canvas.drawPath(path, p);
        }
        @Override protected void onDraw(Canvas original) {
            super.onDraw(original);
            original.save();
            original.scale(getWidth()/400f, getHeight()/860f);
            Canvas c = original;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0,0,400,860,
                new int[]{0xff110509,0xff370907,0xff130308,0xff090408},
                null, Shader.TileMode.CLAMP));
            c.drawRect(0,0,400,860,p);
            p.setShader(null);

            float lit = .72f + .28f*pulse;
            p.setShader(new RadialGradient(200,360,340,
                new int[]{0x00ef3e00,0x44d72800,0x00310000},
                new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
            c.drawCircle(200,360,340,p);
            p.setShader(null);
            for (int i=0; i<8; i++) {
                float x = (i*97+21)%400;
                float y = 95 + (i*191)%720;
                p.setShader(new RadialGradient(x,y,70+(i%3)*28,
                    new int[]{0x00ff6a00,0x33b42706,0x002d0000},null,Shader.TileMode.CLAMP));
                c.drawCircle(x,y,95,p);
            }
            p.setShader(null);

            // Horns, head, shoulders: an original hand-drawn demon silhouette.
            fill(c,0xff11080a, shape(
                145,245, 106,160, 107,68, 137,138, 185,190,
                216,190, 265,138, 295,67, 295,170, 256,245));
            fill(c,0xff080406, shape(
                128,290, 145,228, 179,210, 221,210, 255,228,
                273,289, 265,368, 230,418, 197,444, 165,416, 134,360));
            fill(c,0xff080406, shape(
                164,407, 127,425, 91,481, 85,579, 310,579,
                309,481, 269,428, 236,408, 201,452));
            p.setShader(null);
            p.setColor(0x88ad290d);
            p.setStrokeWidth(2f);
            p.setStyle(Paint.Style.STROKE);
            c.drawPath(shape(143,279, 168,243, 199,234, 232,241, 258,279,
                252,350, 220,405, 200,421, 176,402, 146,347), p);
            p.setStyle(Paint.Style.FILL);

            // Eyes with a molten red halo.
            for (int side=0; side<2; side++) {
                float ex=side==0?173:227;
                p.setShader(new RadialGradient(ex,305,40,
                    new int[]{(int)(0xaaff4b09*lit),0x77f02005,0x00ff0000},
                    null,Shader.TileMode.CLAMP));
                c.drawCircle(ex,305,40,p);
                p.setShader(null);
                fill(c,0xffff651e,side==0
                    ? shape(145,296,184,305,178,317,153,310)
                    : shape(255,296,216,305,222,317,247,310));
                fill(c,0xffffffb1,side==0
                    ? shape(164,302,179,307,171,309)
                    : shape(236,302,221,307,229,309));
            }
            // Sharp mouth.
            fill(c,0xff9e250d,shape(163,363, 178,367, 200,373, 222,367, 239,363,
                 219,384, 200,389, 180,383));
            fill(c,0xfff39d5b,shape(173,368, 183,370, 180,380));
            fill(c,0xfff39d5b,shape(227,368, 217,370, 220,380));

            // Clawed hands grip the left and right edges of the screen.
            claw(c,false);
            claw(c,true);

            // Embers float upward subtly.
            for (int i=0;i<46;i++) {
                float x=(i*89+35)%396;
                float y= (i*173+Math.round(pulse*31))%840;
                float size=.7f+(i%4)*.5f;
                p.setShader(null);
                p.setColor((i%4==0)?0xfffca65b:0x88ed340c);
                c.drawCircle(x,y,size,p);
            }
            original.restore();
        }
        private void claw(Canvas c, boolean right) {
            c.save();
            if (right) { c.translate(400,0); c.scale(-1,1); }
            fill(c,0xff090406,shape(
                -20,312, 31,326, 71,350, 109,382, 123,400,
                100,397, 71,392, 41,376, -14,369));
            fill(c,0xff100609,shape(
                -18,364, 37,371, 67,391, 96,413, 107,434,
                88,425, 52,418, 17,404, -20,410));
            // Four hooked talons curl back from the glass edge.
            for(int k=0;k<4;k++) {
                float y=334+k*23;
                fill(c,0xff080406,shape(
                    3,y, 29,y-4, 63,y+10, 79,y+22,
                    66,y+23, 38,y+12, 13,y+13, -5,y+17));
                p.setColor(0x99b83e1d);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.6f);
                c.drawPath(shape(1,y+2,32,y+2,54,y+13,69,y+19,
                    52,y+15,25,y+9),p);
                p.setStyle(Paint.Style.FILL);
            }
            c.restore();
        }
    }
}

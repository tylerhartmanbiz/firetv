package app.firetvtest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnFocusChangeListener, View.OnClickListener {
    private static final int ROWS = 3, COLS = 4;
    private static final int[] COLORS = {0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFFFB8C00,
            0xFF8E24AA, 0xFF00ACC1, 0xFF6D4C41, 0xFF3949AB, 0xFFD81B60, 0xFF7CB342, 0xFF546E7A, 0xFFFDD835};
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText("test");
        title.setTextSize(40);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Use the remote: up / down / left / right / select");
        status.setTextSize(18);
        status.setTextColor(0xFFBBBBBB);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, (int) (16 * d));
        root.addView(status);

        GridLayout grid = new GridLayout(this);
        grid.setRowCount(ROWS);
        grid.setColumnCount(COLS);
        int w = (int) (150 * d), h = (int) (90 * d), m = (int) (10 * d);
        for (int i = 0; i < ROWS * COLS; i++) {
            int n = i + 1;
            TextView box = new TextView(this);
            box.setText("Box " + n);
            box.setTextSize(22);
            box.setTextColor(Color.WHITE);
            box.setGravity(Gravity.CENTER);
            box.setFocusable(true);
            box.setClickable(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(COLORS[i]);
            bg.setCornerRadius(12 * d);
            bg.setStroke((int) (4 * d), Color.TRANSPARENT);
            box.setBackground(bg);
            box.setAlpha(0.55f);
            box.setTag(bg);
            box.setId(1000 + n);
            box.setOnFocusChangeListener(this);
            box.setOnClickListener(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = w; lp.height = h; lp.setMargins(m, m, m, m);
            grid.addView(box, lp);
        }
        root.addView(grid);
        setContentView(root);
        grid.getChildAt(0).requestFocus();
    }

    public void onFocusChange(View v, boolean has) {
        ((GradientDrawable) v.getTag()).setStroke((int) (4 * getResources().getDisplayMetrics().density), has ? Color.WHITE : Color.TRANSPARENT);
        v.setAlpha(has ? 1f : 0.55f);
        v.setScaleX(has ? 1.1f : 1f);
        v.setScaleY(has ? 1.1f : 1f);
        if (has) status.setText("Focused: Box " + (v.getId() - 1000));
    }

    public void onClick(View v) { status.setText("Selected: Box " + (v.getId() - 1000)); }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        String k = code == KeyEvent.KEYCODE_DPAD_UP ? "UP" : code == KeyEvent.KEYCODE_DPAD_DOWN ? "DOWN"
                : code == KeyEvent.KEYCODE_DPAD_LEFT ? "LEFT" : code == KeyEvent.KEYCODE_DPAD_RIGHT ? "RIGHT" : null;
        if (k != null) {
            View f = getCurrentFocus();
            String name = f instanceof TextView ? ((TextView) f).getText().toString() : "?";
            boolean moved = super.onKeyDown(code, e);
            View f2 = getCurrentFocus();
            if (f2 == f) status.setText(k + " pressed (edge) - still on " + name);
            return moved;
        }
        return super.onKeyDown(code, e);
    }
}

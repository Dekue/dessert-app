package de.fuberlin.dessert.dialog;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import de.fuberlin.dessert.R;
import de.fuberlin.dessert.activity.SetupActivity;

/**
 * Fake drawer menu cannot be drawn but saves resources. Still looks pretty nice.
 */
public class DrawerMenu extends AlertDialog {

	// value in (0; 1]: percentage of screen usage in portrait mode, determines landscape mode width
	private static final float DRAWER_WIDTH = 0.75f;
	private AboutDialog aboutDialog;

	public DrawerMenu(Context context) {
		super(context, R.style.alert_dialog_theme);

		// set dialog options
		View drawerView = getLayoutInflater().inflate(R.layout.drawer_menu, null);
		setView(drawerView);
		setCancelable(true);
		setCanceledOnTouchOutside(true);

		WindowManager.LayoutParams params = getWindow().getAttributes();

		// get display displaySize to scale dialog window (height)
		Point displaySize = new Point();
		Display display = getWindow().getWindowManager().getDefaultDisplay();
		display.getSize(displaySize);

		// get displaySize of toolbar and phone status bar
		TypedArray ta =  getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });

		int titleBarHeight = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
			titleBarHeight = context.getResources().getDimensionPixelSize(resourceId);

		int yMargin = (int)ta.getDimension(0, 0) + titleBarHeight;

		// set x/y-margin and width/height
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = yMargin;
		params.width = getDrawerWidth(displaySize);
		params.height = displaySize.y - yMargin;

		//darken the background
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		params.dimAmount = 0.6f;
	}

	/**
	 * Get the drawer width: 75% of the display size if the screen is in portrait mode or
	 * if it isn't (landscape mode) the same amount of dp as if it were in portrait mode.
	 *
	 * @param displaySize the size of the display
	 * @return drawer width casted to int
	 */
	private int getDrawerWidth(Point displaySize){
		if(getContext().getResources().getConfiguration().orientation ==
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			return (int) ((double) displaySize.x * DRAWER_WIDTH);
		}
		else {
			return (int) ((double) displaySize.y * DRAWER_WIDTH);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drawer_menu);

		final Button prefButton = (Button) findViewById(R.id.DrawerPrefButton);
		if (prefButton != null) {
			prefButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getContext().startActivity(new Intent(getContext(), SetupActivity.class));
				}
			});
			prefButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						prefButton.setBackgroundColor(Color.argb(128,0x4f,0x4f,0x4f));
					}
					if (event.getAction() == MotionEvent.ACTION_UP){
						prefButton.setBackgroundColor(Color.TRANSPARENT);
					}
					return false;
				}
			});
		}

		final Button aboutButton = (Button) findViewById(R.id.DrawerAboutButton);
		if (aboutButton != null) {
			aboutButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					aboutDialog = new AboutDialog(getContext());
					aboutDialog.show();
				}
			});
			aboutButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						aboutButton.setBackgroundColor(Color.argb(128,0x4f,0x4f,0x4f));
					}
					if (event.getAction() == MotionEvent.ACTION_UP){
						aboutButton.setBackgroundColor(Color.TRANSPARENT);
					}
					return false;
				}
			});
		}

		final Button exitButton = (Button) findViewById(R.id.DrawerExitButton);
		if (exitButton != null) {
			exitButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					/*
					Intent homeIntent = new Intent(Intent.ACTION_MAIN);
					homeIntent.addCategory( Intent.CATEGORY_HOME );
					homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					getContext().startActivity(homeIntent);
					*/
					System.exit(0);
				}
			});
			exitButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						exitButton.setBackgroundColor(Color.argb(128,0x4f,0x4f,0x4f));
					}
					if (event.getAction() == MotionEvent.ACTION_UP){
						exitButton.setBackgroundColor(Color.TRANSPARENT);
					}
					return false;
				}
			});
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(aboutDialog != null) {
			if (aboutDialog.isShowing()) {
				aboutDialog.dismiss();
			}
		}
	}
}

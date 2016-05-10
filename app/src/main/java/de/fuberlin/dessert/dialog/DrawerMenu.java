package de.fuberlin.dessert.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import de.fuberlin.dessert.R;

/**
 * Fake drawer menu saves resources and looks pretty nice.
 */
public class DrawerMenu extends AlertDialog {

	public DrawerMenu(Context context){
		super(context, R.style.dialog_theme);

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
		params.width = (int)((double) displaySize.x * 0.6f);
		params.height = displaySize.y - yMargin;

		//darken the background
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		params.dimAmount = 0.6f;
	}
}

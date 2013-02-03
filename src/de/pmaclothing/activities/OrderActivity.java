package de.pmaclothing.activities;

import android.os.Bundle;
import android.view.MenuItem;
import de.pmaclothing.actionbar.ActionBarActivity;

public class OrderActivity extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
	        setResult(RESULT_CANCELED);    
			finish();
			
//			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_background_1);
//				postBitmapToServer(bitmap);
		}
		return super.onOptionsItemSelected(item);
	}
}

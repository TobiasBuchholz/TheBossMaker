package de.pmaclothing.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import de.pmaclothing.actionbar.ActionBarActivity;
import de.pmaclothing.actionbar.CustomActionBarActivity;
import de.pmaclothing.facedetect.R;
import de.pmaclothing.utils.Constants;

public class ChooseBackgroundActivity extends CustomActionBarActivity {
	public static final String EXTRA_CHOSEN_BACKGROUND_POS = "de.pmaclothing.facedetector.chosenBackgroundPos";
	
	private GridView mGridView;
	private BackgroundImageAdapter mImageAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_background);
//		setActionBarHomeIcon(R.drawable.ic_home_back);
		
		mImageAdapter = new BackgroundImageAdapter();
		mGridView = (GridView) findViewById(R.id.gridview_choose_background);
		mGridView.setAdapter(mImageAdapter);
		
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				chooseBackground(position);
			}
		});
	}
	
	public void chooseBackground(final int backgroundId) {
		Intent intent = new Intent();
		intent.putExtra(EXTRA_CHOSEN_BACKGROUND_POS, backgroundId);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
	        setResult(RESULT_CANCELED);    
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class BackgroundImageAdapter extends BaseAdapter {
	    private LayoutInflater mInflater;
	    private ViewHolder mHolder;
	    
	    public BackgroundImageAdapter() {
	        mInflater = ChooseBackgroundActivity.this.getLayoutInflater();
	    }

	    public int getCount() {
	        return FaceDetectorActivity.mBackgroundIds.length;
	    }

	    public Object getItem(int position) {
	        return null;
	    }

	    public long getItemId(int position) {
	        return 0;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        if (convertView == null) { 
	        	mHolder = new ViewHolder();
	        	convertView = mInflater.inflate(R.layout.grid_item_choose_background, null);
	        	
	        	mHolder.mImageViewBackground = (ImageView) convertView.findViewById(R.id.grid_item_background_image);
	        	
	        	convertView.measure(View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
	            convertView.setTag(mHolder);
	        } else {
	        	mHolder = (ViewHolder) convertView.getTag();
	        }
	        
	        String path = Constants.PMA_BOSSES_FILE_PATH + Constants.FILE_PATH_THUMBS + FaceDetectorActivity.mBackgroundIds[position] + Constants.SUFFIX_JPEG;
			Bitmap bitmap = BitmapFactory.decodeFile(path);
			mHolder.mImageViewBackground.setImageBitmap(bitmap);
	        return convertView;
	    }

	    private class ViewHolder {
	        ImageView   mImageViewBackground;
	    }
	}
}

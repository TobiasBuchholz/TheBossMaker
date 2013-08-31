package de.pmaclothing.view;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.app.*;
import android.view.GestureDetector;
import android.view.ViewGroup;
import de.pmaclothing.facedetect.R;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 14.07.13 | Time: 13:46
 */
public class BossFragmentPagerAdapter extends FixedFragmentStatePagerAdapter {
    public static final int[]       mBackgroundIds          = { R.drawable.sample_background_4, R.drawable.sample_background_2, R.drawable.sample_background_3, R.drawable.screen_test_background};
    public static final Point[]     mFacePositions          = { new Point(-110, -120), new Point(-230, -190), new Point(100, -190), new Point(-110, -120)};
    public static final Point[]     mFaceSavingsPositions   = { new Point(-110, -120), new Point(-110, -120), new Point(-110, -120), new Point(-110, -120)};

    private final Context           mContext;

    private GestureDetector         mGestureDetector;
    private BossFragment            mCurrentFragment;

    public BossFragmentPagerAdapter(final FragmentManager fragmentManager, final Context context) {
        super(fragmentManager);
        mContext = context;
    }

    @Override
    public Fragment getItem(final int i) {
        final BossFragment bossFragment = BossFragment.instantiate(mContext, BossFragment.class.getName(), i);
        bossFragment.setGestureDetector(mGestureDetector);
        return bossFragment;
    }

    @Override
    public int getCount() {
        return mBackgroundIds.length;
    }

    @Override
    public void setPrimaryItem(final ViewGroup container, final int position, final Object object) {
        mCurrentFragment = (BossFragment) object;
        super.setPrimaryItem(container, position, object);
    }

    public BossFragment getCurrentFragment() {
        return mCurrentFragment;
    }

    public void setGestureDetector(final GestureDetector listener) {
        mGestureDetector = listener;
    }
}

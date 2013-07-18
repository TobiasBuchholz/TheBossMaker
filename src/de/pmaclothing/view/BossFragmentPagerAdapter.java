package de.pmaclothing.view;

import android.content.Context;
import android.support.v4.app.*;
import android.view.GestureDetector;
import android.view.ViewGroup;
import de.pmaclothing.facedetect.R;

/**
 * User: tobiasbuchholz @ PressMatrix GmbH
 * Date: 14.07.13 | Time: 13:46
 */
public class BossFragmentPagerAdapter extends FixedFragmentStatePagerAdapter {
    public static final int[]       mBackgroundIds = { R.drawable.sample_background_4, R.drawable.sample_background_2, R.drawable.sample_background_3, R.drawable.screen_test_background};
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

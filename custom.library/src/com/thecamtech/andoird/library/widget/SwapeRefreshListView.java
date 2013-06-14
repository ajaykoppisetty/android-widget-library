package com.thecamtech.andoird.library.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Scroller;

import com.thecamtech.andoird.library.R;

public class SwapeRefreshListView extends ListView {

	private float mDownPositionY = 0;
	private float mDragPositionY;
	private float mLastPositionY;

	private boolean mIsRefreshAble;
	private boolean mBeginDrag;
	private boolean mItemIsOnTop;
	private boolean mShouldUpdateProgress;
	private boolean mProgressing;

	private ProgressBar mHosizontalProgressBar;
	private View mRefreshView;
	private View mRefreshContainer;
	private PullRefreshViewGroup mPullRefreshView;

	private AnimationListener mPushDownListener;

	private Animation mFadeIn;
	private Animation mFadeOut;
	private Animation mPushDown;
	private Animation mPushUp;

	private OnRefreshListener mOnRefreshListener;

	private int mTouchSlop;
	private int mProgressHeight;

	private Handler mHander = new Handler();
	private Scroller mScroller;

	private Runnable mProgressDecrease = new Runnable() {
		@Override
		public void run() {
			if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
				mPullRefreshView.setWidth(Math.abs(mScroller.getCurrX()));
				mPullRefreshView.requestLayout();
				mHander.postDelayed(this, 34);
			}
		}
	};

	public SwapeRefreshListView(Context context) {
		this(context, null);
	}

	public SwapeRefreshListView(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.swapeRefreshListViewStyle);
	}

	public SwapeRefreshListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.SwapeRefreshListView, defStyle, 0);

		int contentLayout = a.getResourceId(
				R.styleable.SwapeRefreshListView_refreshLayout, 0);
		int refreshContainerId = a.getResourceId(
				R.styleable.SwapeRefreshListView_refreshContainerId, 0);
		int progressBar = a.getResourceId(
				R.styleable.SwapeRefreshListView_progressBar, 0);

		if (contentLayout == 0 || refreshContainerId == 0) {
			throw new RuntimeException("Refresh container id must be set.");
		}

		Drawable drawable = a
				.getDrawable(R.styleable.SwapeRefreshListView_progressBackground);

		mProgressHeight = a.getDimensionPixelSize(
				R.styleable.SwapeRefreshListView_progressHeight, 0);

		mIsRefreshAble = a.getBoolean(R.styleable.SwapeRefreshListView_enabled,
				true);

		a.recycle();

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = ViewConfigurationCompat
				.getScaledPagingTouchSlop(configuration);

		mScroller = new Scroller(context);

		mPullRefreshView = new PullRefreshViewGroup(context);
		mRefreshView = LayoutInflater.from(context)
				.inflate(contentLayout, null);
		mRefreshContainer = mRefreshView.findViewById(refreshContainerId);
		View progress = new View(context);
		progress.setBackgroundDrawable(drawable);

		mPullRefreshView.addView(mRefreshView);
		mPullRefreshView.addView(progress);
		mPullRefreshView.mRefreshContent = mRefreshView;
		mPullRefreshView.mProgress = progress;

		if (progressBar != 0) {
			mHosizontalProgressBar = (ProgressBar) LayoutInflater.from(context)
					.inflate(progressBar, null);
			mPullRefreshView.addView(mHosizontalProgressBar);
		}

		initView();
	}

	public void initView() {

		WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
		windowParams.gravity = Gravity.TOP | Gravity.LEFT;
		windowParams.x = 0;
		windowParams.y = (int) (25 * getResources().getDisplayMetrics().density);

		// older version under ICS
		int actionBarHeight = ((Activity) getContext()).getWindow()
				.findViewById(Window.ID_ANDROID_CONTENT).getTop()
				- windowParams.y;

		TypedValue tv = new TypedValue();
		if (((Activity) getContext()).getTheme().resolveAttribute(
				android.R.attr.actionBarSize, tv, true)) {
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
					getResources().getDisplayMetrics());
		}

		windowParams.height = actionBarHeight;
		windowParams.width = getResources().getDisplayMetrics().widthPixels;
		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		windowParams.format = PixelFormat.TRANSLUCENT;

		WindowManager windowManager = (WindowManager) getContext()
				.getSystemService("window");
		windowManager.addView(mPullRefreshView, windowParams);

		mRefreshView.setVisibility(View.GONE);
		mRefreshContainer.setVisibility(View.GONE);
		if (mHosizontalProgressBar != null)
			mHosizontalProgressBar.setVisibility(View.GONE);

		mPushDownListener = new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				mHander.postDelayed(new Runnable() {
					@Override
					public void run() {
						mShouldUpdateProgress = true;
					}
				}, 10);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
			}
		};

		mPushDown = AnimationUtils
				.loadAnimation(getContext(), R.anim.push_down);
		mPushUp = AnimationUtils.loadAnimation(getContext(), R.anim.push_up);
		mFadeIn = AnimationUtils.loadAnimation(getContext(),
				android.R.anim.fade_in);
		mFadeOut = AnimationUtils.loadAnimation(getContext(),
				android.R.anim.fade_out);

		mPushDown.setFillAfter(true);
		mPushUp.setFillAfter(true);
		mFadeIn.setFillAfter(true);
		mFadeOut.setFillAfter(true);
		mPushDown.setAnimationListener(mPushDownListener);
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}

	public void setEnable(boolean bool) {
		mIsRefreshAble = bool;
		if (!bool) {
			// immediately disable refresh feature on demand
			resetRefresh();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		WindowManager windowManager = (WindowManager) getContext()
				.getSystemService("window");
		windowManager.removeView(mPullRefreshView);
		super.onDetachedFromWindow();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		touch(ev);
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			return super.onTouchEvent(ev);
		}

		if (touch(ev)) {
			return true;
		}

		return super.onTouchEvent(ev);
	}

	private boolean touch(MotionEvent event) {
		if (mIsRefreshAble && !mProgressing) {
			int action = event.getAction();

			switch (action & MotionEventCompat.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mDownPositionY = event.getY();
				mItemIsOnTop = this.getFirstVisiblePosition() == 0
						&& this.getChildAt(0).getTop() >= 0;
				break;

			case MotionEvent.ACTION_MOVE:
				if (mDownPositionY == 0) {
					mDownPositionY = event.getY();
				}

				if (!mItemIsOnTop) {
					mItemIsOnTop = this.getFirstVisiblePosition() == 0
							&& this.getChildAt(0).getTop() >= 0;
				}

				final float distance = event.getY() - mLastPositionY;
				if (distance < 0
						&& Math.abs(event.getY() - mLastPositionY) > mTouchSlop) {
					resetRefresh();
					return false;
				}

				if (distance > 0 && event.getY() - mDownPositionY > mTouchSlop
						&& mItemIsOnTop) {
					if (!mBeginDrag) {
						if (!mScroller.isFinished()) {
							mScroller.abortAnimation();
						}
						mDragPositionY = event.getY();
						mRefreshView.setVisibility(View.VISIBLE);
						mRefreshContainer.setVisibility(View.VISIBLE);
						mPullRefreshView.mProgress.setVisibility(View.VISIBLE);

						mRefreshView.clearAnimation();
						mRefreshContainer.clearAnimation();

						mRefreshView.startAnimation(mFadeIn);
						mRefreshContainer.startAnimation(mPushDown);
						mBeginDrag = true;

						if (mOnRefreshListener != null) {
							mOnRefreshListener.onBeginDrag(mRefreshView);
						}
					}

					if (mShouldUpdateProgress) {
						mPullRefreshView
								.setWidth(Math.abs((int) (event.getY() - mDragPositionY)));
						mPullRefreshView.requestLayout();
					}
					mLastPositionY = event.getY();

					return true;
				}
				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				resetRefresh();
			}
		}
		return false;
	}

	public void doneLoading() {
		mProgressing = false;
		resetRefresh();
	}

	private void resetRefresh() {
		if (mBeginDrag) {
			int progressWidth = mPullRefreshView.getWidht();
			mScroller.startScroll(progressWidth, 0, -progressWidth, 0, 600);
			mHander.post(mProgressDecrease);

			mRefreshView.clearAnimation();
			mRefreshContainer.clearAnimation();

			mRefreshView.startAnimation(mFadeOut);
			mRefreshContainer.startAnimation(mPushUp);
			mBeginDrag = false;
		}
		mDownPositionY = 0;
		mLastPositionY = 0;
		mItemIsOnTop = false;
		mShouldUpdateProgress = false;
		if (!mProgressing && mHosizontalProgressBar != null) {
			mHosizontalProgressBar.setVisibility(View.GONE);
		}
	}

	private class PullRefreshViewGroup extends ViewGroup {

		private View mProgress;
		private View mRefreshContent;
		private int mProgressWidth;
		private int mScreenWidth;
		private int mMultiply;

		public PullRefreshViewGroup(Context context) {
			super(context);
			mProgressWidth = 0;
			mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
			int height = getContext().getResources().getDisplayMetrics().heightPixels;
			if (height < mScreenWidth) {
				mMultiply = 10;
			}else{
				mMultiply = 2;
			}
		}

		private void setWidth(int width) {
			mProgressWidth = width * mMultiply;
			if (!mProgressing && mProgressWidth >= mScreenWidth) {
				mProgressing = true;
				mShouldUpdateProgress = false;
				boolean showProgressBar = mHosizontalProgressBar != null;
				if (mOnRefreshListener != null) {
					boolean bool = mOnRefreshListener.onRefresh(mRefreshView);
					showProgressBar = showProgressBar && bool;
				}
				if (showProgressBar) {
					mHosizontalProgressBar.setVisibility(View.VISIBLE);
				}
				mProgressWidth = 0;
				requestLayout();
				resetRefresh();
			}
		}

		private int getWidht() {
			return mProgressWidth / mMultiply;
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			mRefreshContent.measure(widthMeasureSpec, heightMeasureSpec);

			mProgress.measure(
					MeasureSpec.makeMeasureSpec(
							Math.min(mScreenWidth, mProgressWidth),
							MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
							mProgressHeight, MeasureSpec.EXACTLY));

			if (mHosizontalProgressBar != null)
				mHosizontalProgressBar.measure(MeasureSpec.makeMeasureSpec(
						mScreenWidth, MeasureSpec.EXACTLY), MeasureSpec
						.makeMeasureSpec(MeasureSpec.AT_MOST,
								MeasureSpec.EXACTLY));

			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			mRefreshContent.layout(0, 0, mRefreshContent.getMeasuredWidth(),
					mRefreshContent.getMeasuredHeight());

			int left = (mScreenWidth - mProgress.getMeasuredWidth()) / 2;
			int top = getMeasuredHeight() - mProgress.getMeasuredHeight();
			mProgress.layout(left, top, left + mProgress.getMeasuredWidth(),
					top + mProgress.getMeasuredHeight());

			if (mHosizontalProgressBar != null) {
				top = (int) (getMeasuredHeight()
						- mHosizontalProgressBar.getMeasuredHeight() + 6 * getResources()
						.getDisplayMetrics().density);
				mHosizontalProgressBar.layout(0, top,
						mHosizontalProgressBar.getMeasuredWidth(), top
								+ mHosizontalProgressBar.getMeasuredHeight());
			}
		}

	}

	public static interface OnRefreshListener {

		public void onBeginDrag(View view);

		public boolean onRefresh(View view);

	}

}

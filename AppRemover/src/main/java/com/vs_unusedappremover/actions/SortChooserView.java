package com.vs_unusedappremover.actions;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.ListPopupWindow;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.vs_unusedappremover.R;
import com.vs_unusedappremover.actions.SortActionProvider.SortActionAdapter;
import com.vs_unusedappremover.common.GA;

class SortChooserView extends ViewGroup {

    private SortActionAdapter mAdapter;
    private final View mButton;  
    private final TextView mTextView;
    private AdapterView.OnItemClickListener itemListener;
        
    private final int mListPopupMaxWidth;
    private SortActionProvider mProvider;

    private final OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (isShowingPopup()) {
                if (!isShown()) {
                    getListPopupWindow().dismiss();
                } else {
                    getListPopupWindow().show();
                    if (mProvider != null) {
                        mProvider.subUiVisibilityChanged(true);
                    }
                }
            }
        }
    };

    private ListPopupWindow mListPopupWindow;

    private boolean mIsAttachedToWindow;
    private final Context mContext;

    public SortChooserView(Context context) {
        this(context, null);
    }

    public SortChooserView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SortChooserView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(R.layout.chooser_view, this, true);
            
        mButton = findViewById(R.id.expand_activities_button);
        mButton.setOnClickListener(onButtonClick);
        
        mTextView = (TextView)findViewById(R.id.text);
               
        Resources resources = context.getResources();
        int size = 0;
        try {
            size = resources.getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_dialog_fixed_width_major);
        } catch (Resources.NotFoundException e) {
            GA.reportException(e);
        }
        mListPopupMaxWidth = Math.max(resources.getDisplayMetrics().widthPixels / 2, size);
    }
    
    public void setAdapter(SortActionAdapter adapter) {
    	 this.mAdapter = adapter;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
    	this.itemListener = listener;
    }
    
    public void setProvider(SortActionProvider provider) {
        mProvider = provider;
        setTextResource(mProvider.getOrder().shortTextResId);
        this.requestLayout();
    }

    public boolean showPopup() {
        if (isShowingPopup() || !mIsAttachedToWindow) {
            return false;
        }
       
        showPopupUnchecked();
        return true;
    }
    
    private void showPopupUnchecked() {
        
        getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        
        ListPopupWindow popupWindow = getListPopupWindow();
        if (!popupWindow.isShowing()) {            
            final int contentWidth = Math.min(measureContentWidth(mAdapter), mListPopupMaxWidth);
            popupWindow.setContentWidth(contentWidth);
            popupWindow.show();
            if (mProvider != null) {
                mProvider.subUiVisibilityChanged(true);
            }
            popupWindow.getListView().setContentDescription(mContext.getString(R.string.menu_sort));
        }
        
        setTextResource(R.string.order_by);
    }

    public boolean dismissPopup() {    	
        if (isShowingPopup()) {
            getListPopupWindow().dismiss();
            ViewTreeObserver viewTreeObserver = getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.removeGlobalOnLayoutListener(mOnGlobalLayoutListener);
            }            
        }
        return true;
    }

    public boolean isShowingPopup() {
        return getListPopupWindow().isShowing();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();        
        mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();        
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeGlobalOnLayoutListener(mOnGlobalLayoutListener);
        }
        mIsAttachedToWindow = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View child = mButton;
        // If the default action is not visible we want to be as tall as the
        // ActionBar so if this widget is used in the latter it will look as
        // a normal action button.        
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	mButton.layout(0, 0, right - left, bottom - top);
        if (getListPopupWindow().isShowing()) {
            showPopupUnchecked();
        } else {
            dismissPopup();
        }
    }

    private ListPopupWindow getListPopupWindow() {
        if (mListPopupWindow == null) {
            mListPopupWindow = new ListPopupWindow(getContext());
            mListPopupWindow.setAdapter(mAdapter);
            mListPopupWindow.setAnchorView(SortChooserView.this);
            mListPopupWindow.setModal(true);
            mListPopupWindow.setOnItemClickListener(onItemClick);
            mListPopupWindow.setOnDismissListener(onPopupDismissed);
        }
        return mListPopupWindow;
    }
    
    private int measureContentWidth(BaseAdapter adapter) {
        
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);            
        final int count = adapter.getCount();

        int contentWidth = 0;
        View itemView = null;
        for (int i = 0; i < count; i++) {
            itemView = adapter.getView(i, itemView, null);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            contentWidth = Math.max(contentWidth, itemView.getMeasuredWidth());
        }
        return contentWidth;
    } 
    
    private void setTextResource(int resId) {
    	mTextView.setText(resId);
		requestLayout();
    }
    
    private final OnItemClickListener onItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {			
			if (itemListener != null) {
				itemListener.onItemClick(parent, view, position, id);
			}            
			dismissPopup();
		}
	};
	
	private final View.OnClickListener onButtonClick = new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			showPopupUnchecked();
		}
	}; 
	
	private final PopupWindow.OnDismissListener onPopupDismissed = new PopupWindow.OnDismissListener() {
		@Override
		public void onDismiss() {
			setTextResource(mProvider.getOrder().shortTextResId);
			if (mProvider != null) {
                mProvider.subUiVisibilityChanged(false);
            }
		}
	};
}

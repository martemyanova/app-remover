package com.vs_unusedappremover.actions;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vs_unusedappremover.R;
import com.vs_unusedappremover.data.OrderBy;

public class SortActionProvider extends ActionProvider {
	
	public interface OnSortSelectedListener {    
        public void onSortSelected(SortActionProvider source, OrderBy order);
    }

    private final Context context;
    private final SortActionAdapter adapter;
    private OnSortSelectedListener listener;
    private OrderBy order = OrderBy.TIME_UNUSED;
    
    public SortActionProvider(Context context) {
        super(context);
        this.context = context;
        this.adapter = new SortActionAdapter(context);
    }

    @Override
    public View onCreateActionView() {
    	SortChooserView activityChooserView = new SortChooserView(context);
                                        
        activityChooserView.setProvider(this);
        
        activityChooserView.setAdapter(adapter);
        activityChooserView.setOnItemClickListener(onItemClick);
        
        return activityChooserView;
    }
    
    public void setOrder(OrderBy order) {
    	assert order != null;
    	this.order = order;
    }
    
    public OrderBy getOrder() {
    	return order;
    }
    
    public void setOnSortSelectedListener(OnSortSelectedListener l) {
    	this.listener = l;
    }
    
    private final OnItemClickListener onItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			order = adapter.getItem(position);
			if (listener != null) {
				listener.onSortSelected(SortActionProvider.this, order);
			}			           
		}
	};
    
    public static class SortActionAdapter extends BaseAdapter {
        
    	private final Context context;
    	
    	public SortActionAdapter(Context context) {
    		this.context = context;
    	}
    	
        public int getCount() {
        	return OrderBy.values().length;
        }

        public OrderBy getItem(int position) {
        	return OrderBy.values()[position];            
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	if (convertView == null || convertView.getId() != R.id.list_item) {
                convertView = LayoutInflater.from(context).inflate(R.layout.chooser_list_item, parent, false);
            }
        	OrderBy order = getItem(position);
                                   
            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            titleView.setText(order.fullTextResId);
            
            return convertView;
        }              
    }
}




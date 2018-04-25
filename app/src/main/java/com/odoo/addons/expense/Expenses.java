/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 30/12/14 3:28 PM
 */
package com.odoo.addons.expense;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.expense.models.HrEmployee;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.support.list.OListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Expenses extends BaseFragment implements ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener,
        OCursorListAdapter.OnViewBindListener, IOnSearchViewChangeListener, View.OnClickListener,
        AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, AdapterView.OnItemSelectedListener {

    public static final String KEY = Expenses.class.getSimpleName();
    public static final String EXTRA_KEY_TYPE = "extra_key_type";
    public static final String EXTRA_IS_MINE = "extra_is_mine";

    public static final String TAG = Expenses.class.getSimpleName();
    private View mView;
    private String mCurFilter = null;
    private String stateFilter = null;
    ListView listView;
    private OCursorListAdapter mAdapter = null;
    private boolean syncRequested = false;

    HrEmployee hrEmployee;
    HrExpense hrExpense;

    private Spinner spinner;
    private OListAdapter spinnerAdapter;

    boolean is_mine = false;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        setHasSyncStatusObserver(KEY, this, db());

        return inflater.inflate(R.layout.common_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hrEmployee = new HrEmployee(getContext(),user());
        hrExpense = new HrExpense(getContext(),null);

        is_mine = getArguments().getBoolean(EXTRA_IS_MINE);

        parent().setHasActionBarSpinner(true);
        spinner = parent().getActionBarSpinner();

        setHasSwipeRefreshView(view, R.id.swipe_container, this);
        mView = view;
        listView = (ListView) view.findViewById(R.id.listview);
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.expense_row_item);
        mAdapter.setOnViewBindListener(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        setHasFloatingButton(view, R.id.fabButton, listView, this);
        getLoaderManager().initLoader(0, null, this);
        initSpinner();
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {

        int attachment_number = hrExpense.compute_attachment_number(row.toValues());
        if(attachment_number != row.getInt("attachment_number")){
            row.put("attachment_number",attachment_number);
            hrExpense.update(row.getInt(OColumn.ROW_ID),row.toValues());
        }
        OControls.setText(view, R.id.name, row.getString("name"));
        String date_string = "";
        try {
            SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
            Date date = dateParse.parse(row.getString("date"));
            date_string = dateFormat.format(date);
        }catch (Exception e){
            Log.d(TAG, "onViewBind: e = "+e.fillInStackTrace());
        }
        OControls.setText(view, R.id.date, date_string);
        String state = hrExpense.compute_state(row.toValues());

        if(!state.equals(row.getString("state"))){
            row.put("state",state);
            hrExpense.update(row.getInt(OColumn.ROW_ID),row.toValues());
        }
        String state_string = row.getString("state");
        HashMap<String,String> stateMap = hrExpense.getColumn("state").getSelectionMap();
        if(stateMap.containsKey(row.getString("state"))){
            state_string = stateMap.get(state_string);
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.attachment_icon);
        Drawable drawable = ContextCompat.getDrawable(getContext(),
                        row.getInt("attachment_number") > 0 ?
                                R.drawable.ic_attach_file_black_24dp : R.drawable.ic_attach_money_black_24dp);
        imageView.setImageDrawable(drawable);
        OControls.setText(view, R.id.state,state_string);
        OControls.setText(view, R.id.total_price, String.valueOf(row.getString("total_amount")));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        int emp_id = -1;
        try {
            for (ODataRow row : hrEmployee.select()){
                if(row.get("user_id") != null && row.getM2ORecord("user_id").browse().getInt("id").equals(user().getUserId())){
                    emp_id = row.getInt(OColumn.ROW_ID);
                }
            }
        }catch (Exception e){
            Log.d(TAG, "onCreateLoader: e = "+e.fillInStackTrace());
        }
        String where= "";
        List<String> args = new ArrayList<>();

        if(is_mine){
            where = "employee_id = ? ";
            args.add(String.valueOf(emp_id));
        }

        if (mCurFilter != null) {
            if(where.length() > 0){
                where += " and ";
            }
            where += " name like ? ";
            args.add(mCurFilter + "%");
        }
        if (stateFilter != null) {
            if(where.length() > 0){
                where += "and ";
            }
            where += " state = ? ";
            args.add(stateFilter);
        }
        String selection = (args.size() > 0) ? where : null;
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return new CursorLoader(getActivity(), db().uri(),
                null, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        if (data.getCount() > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(mView, R.id.swipe_container);
                    OControls.setGone(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Expenses.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, Expenses.this);
                    OControls.setImage(mView, R.id.icon, R.drawable.ic_monetization_on_black_24dp);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no_expense_found));
                    OControls.setText(mView, R.id.subTitle, "");
                }
            }, 500);
            if (db().isEmptyTable() && !syncRequested) {
                syncRequested = true;
                onRefresh();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public Class<HrExpense> database() {
        return HrExpense.class;
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();
        items.add(new ODrawerItem(KEY).setTitle("Expense")
                .setIcon(R.drawable.ic_monetization_on_black_24dp)
                .setInstance(new Expenses()));
        items.add(new ODrawerItem(KEY).setTitle("Expense (Mine)")
                .setIcon(R.drawable.ic_monetization_on_black_24dp)
                .setExtra(is_mine(true))
                .setInstance(new Expenses()));
        items.add(new ODrawerItem(KEY).setTitle("To Approve")
                .setIcon(R.drawable.ic_monetization_on_black_24dp)
                .setInstance(new Sheets()));
        items.add(new ODrawerItem(KEY).setTitle("To Approve (Mine)")
                .setExtra(is_mine(true))
                .setIcon(R.drawable.ic_monetization_on_black_24dp)
                .setInstance(new Sheets()));
        return items;
    }

    public Bundle extra() {
        return new Bundle();
    }


    @Override
    public void onStatusChange(Boolean refreshing) {
        // Sync Status
        getLoaderManager().restartLoader(0, null, this);
    }


    @Override
    public void onRefresh() {
        if (inNetwork()) {
            parent().sync().requestSync(HrExpense.AUTHORITY);
            setSwipeRefreshing(true);
        } else {
            hideRefreshingProgress();
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {
        // nothing to do
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabButton:
                loadActivity(null);
                break;
        }
    }

    private void loadActivity(ODataRow row) {
        Bundle data = new Bundle();
        if (row != null) {
            data = row.getPrimaryBundleData();
        }
        IntentUtils.startActivity(getActivity(), ExpenseDetails.class, data);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        loadActivity(row);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if(item.getItemId() == R.id.action_submit_to_manager){
            SparseBooleanArray positions = listView.getCheckedItemPositions();
            ArrayList<Integer> checked = new ArrayList<>();
            List<String> error_row_name = new ArrayList<>();
            String error_string = "";
            Log.d(TAG, "onActionItemClicked: positions raw = "+String.valueOf(positions));
            String emp_id = null;
            for(int i = 0 ; i < positions.size(); i++){
                if(positions.valueAt(i)){
                    ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(positions.keyAt(i)));
                    if(!hrExpense.compute_state(row.toValues()).equals("draft")){
                        error_string = "You cannot report twice the same line!";
                        error_row_name.add(row.getString("name"));
                        break;
                    }
                    if(emp_id == null){
                        emp_id = row.getString("employee_id");
                    }else if(!emp_id.equals(row.getString("employee_id"))){
                        error_string = "One sheet only for one employee";
                        break;
                    }
                    checked.add(row.getInt(OColumn.ROW_ID));
                }
            }
            Log.d(TAG, "onActionItemClicked: positions 2 = "+String.valueOf(checked));
            if(error_string.length() > 0){
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(error_string);
                builder.create().show();
            }else {
                Intent expenseSheetIntent = new Intent(getActivity(),ExpenseSheetActivity.class);
                Bundle bundle = new Bundle();
                bundle.putIntegerArrayList(ExpenseDetails.EXPENSE_IDS_TAG,checked);
                expenseSheetIntent.putExtras(bundle);
                startActivity(expenseSheetIntent);
            }
            mode.finish();
        }
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        getActivity().findViewById(R.id.action_mode_bar).setVisibility(View.GONE);
        parent().getSupportActionBar().show();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_expense_selector,menu);
        parent().getSupportActionBar().hide();
        return true;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.setTitle(String.valueOf(listView.getCheckedItemCount()));
    }

    private void initSpinner(){
        List<Object> spinnerItems = new ArrayList<>();
        if(getActivity() == null){
            return;
        }
        ODataRow row = new ODataRow();
        row.put("key","false");
        row.put("name","All Expenses");
        spinnerItems.add(row);
        HashMap<String,String> hashMap = hrExpense.getColumn("state").getSelectionMap();
        for ( String key : hashMap.keySet()){
            row = new ODataRow();
            row.put("key",key);
            row.put("name",hashMap.get(key));
            spinnerItems.add(row);
        }
        if(spinnerItems.size() == 1){
            parent().setHasActionBarSpinner(false);
            return;
        }
        spinnerAdapter = new OListAdapter(getActivity(),R.layout.base_simple_list_item_1,spinnerItems){
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null){
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.base_simple_list_item_1_selected
                            , parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(getResource(), parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }
        };
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(this);
    }

    private View getSpinnerView(Object row, int pos, View view, ViewGroup parent) {
        ODataRow r = (ODataRow) row;
        OControls.setText(view, android.R.id.text1, r.getString("name"));
        return view;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ODataRow row = (ODataRow) spinnerAdapter.getItem(position);
        if(row.getString("key").equals("false")){
            stateFilter = null;
        }else {
            stateFilter = row.getString("key");
        }
        getLoaderManager().restartLoader(0,null,this);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        parent.setSelection(0);
    }

    public Bundle is_mine(boolean isMine) {
        Bundle extra = new Bundle();
        extra.putBoolean(EXTRA_IS_MINE, isMine);
        return extra;
    }
}

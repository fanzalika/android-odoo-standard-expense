package com.odoo.addons.expense;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.customers.CustomerDetails;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.ir.feature.OFileManager;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.support.OUser;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.support.sync.SyncUtils;
import com.odoo.core.utils.OControls;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import odoo.controls.OField;

public class ExpenseAttachmentActivity extends OdooCompatActivity implements View.OnClickListener,SwipeRefreshLayout.OnRefreshListener{

    private String TAG = ExpenseAttachmentActivity.class.getSimpleName();
    private String KEY = ExpenseAttachmentActivity.class.getSimpleName();
//    private ActionBar actionBar;
    IrAttachment irAttachment;
    Bundle extras;
    int res_id = -1;
    String res_model;
    OFileManager oFileManager;
//    List<ODataRow> attachments;
//    LoadAttachments loadAttachments;

    private String mCurFilter = null;
    private boolean syncRequested = false;

    ActionBar actionBar;

    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    ExpenseAttachmentAdapter expenseAttachmentAdapter;

    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        irAttachment = new IrAttachment(ExpenseAttachmentActivity.this,null);
        extras = getIntent().getExtras();
        res_id = extras.getInt(ExpenseDetails.SERVER_ID_TAG);
        res_model = extras.getString(ExpenseDetails.MODEL_NAME_TAG);

        progressDialog = new ProgressDialog(ExpenseAttachmentActivity.this);
        progressDialog.setMessage("Loading attachment(s)..");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        setContentView(R.layout.activity_expense_attachment);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new GridLayoutManager(this,3));

        expenseAttachmentAdapter = new ExpenseAttachmentAdapter(new ArrayList<ODataRow>());
        recyclerView.setAdapter(expenseAttachmentAdapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        if(extras != null){
            actionBar.setTitle(extras.getString(ExpenseDetails.RECORD_NAME_TAG));
        }

        oFileManager = new OFileManager(this);
        expenseAttachmentAdapter.swap(collectLocalData());


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private OModel db() {
        return new OModel(ExpenseAttachmentActivity.this, null, user()).createInstance(IrAttachment.class);
    }

    public OUser user() {
        return OUser.current(ExpenseAttachmentActivity.this);
    }

//    private class LoadAttachments extends AsyncTask<Void, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    for (ODataRow row : attachments) {
//                        addAttachment(row);
//                    }
//                }
//            });
//            return null;
//        }
//    }

    private void addAttachment(ODataRow values,LinearLayout linearLayout) {
        View attachmentView = LayoutInflater.from(this)
                .inflate(R.layout.base_attachment_item, linearLayout, false);
        String fileName = values.getString("name");
        String type = values.getString("file_type");
        String datas_fname = values.getString("datas_fname");
        ImageView imgPreview = (ImageView) attachmentView.findViewById(R.id.attachmentPreview);
        String[] image_ext = new  String[]{".png",".jpg",".jpeg"};
        for (String s : image_ext){
            if(datas_fname.contains(s)){
                type = "image";
                break;
            }
        }
        if (type.contains("image")) {
            if (!values.getString("file_uri").equals("false")) {
                Uri uri = Uri.parse(new File(values.getString("file_uri")).toString());
                imgPreview.setImageBitmap(oFileManager.getBitmapFromURI(uri));
            } else{
                imgPreview.setImageResource(R.drawable.image);
            }
        } else if (type.contains("audio")) {
            imgPreview.setImageResource(R.drawable.audio);
        } else if (type.contains("video")) {
            imgPreview.setImageResource(R.drawable.video);
        } else {
            imgPreview.setImageResource(R.drawable.file);
        }
        OControls.setText(attachmentView, R.id.attachmentFileName, fileName);
        attachmentView.setTag(values);
        attachmentView.findViewById(R.id.btnRemoveAttachment).setVisibility(View.GONE);
        attachmentView.setOnClickListener(this);
        linearLayout.addView(attachmentView);
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            ODataRow attachment = (ODataRow) v.getTag();
            oFileManager.downloadAttachment(attachment.getInt(OColumn.ROW_ID));
        }
    }


    public boolean inNetwork() {
        App app = (App) getApplicationContext();
        return app.inNetwork();
    }

    private class ReadAttachmentsByRecord extends AsyncTask<Void,Void,List<ODataRow>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected List<ODataRow> doInBackground(Void... params) {
            OdooFields odooFields = new OdooFields(irAttachment.getColumns());
            ODomain oDomain = new ODomain()
                    .add("res_model","=",res_model)
                    .add("res_id","=",res_id);
            return irAttachment.getServerDataHelper().searchRecords(odooFields,oDomain,80);
        }

        protected void onPostExecute(List<ODataRow> oDataRows) {
            for(ODataRow row : oDataRows){
                int id = row.getFloat("id").intValue();
                row.put("id",id);
                row.put("company_id","false");
                Log.d(TAG, "onPostExecute: "+row.toString());
                irAttachment.insertOrUpdate(id,row.toValues());
            }
            if(oDataRows.size() > 0){
                expenseAttachmentAdapter.swap(collectLocalData());
                expenseAttachmentAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
            progressDialog.dismiss();
        }
    }
    private class ExpenseAttachmentAdapter extends RecyclerView.Adapter<ExpenseAttachmentAdapter.ExpenseAttachmentHolder>{
        List<ODataRow> oDataRows;
        ExpenseAttachmentAdapter(List<ODataRow> oDataRows){
            super();
            this.oDataRows = oDataRows;
        }
        class ExpenseAttachmentHolder extends RecyclerView.ViewHolder{
            View view;
            ImageView imgPreview;
            TextView attachmentFileName;
            ImageView btnRemoveAttachment;
            ExpenseAttachmentHolder(View view){
                super(view);
                this.view = view;
                view.setOnClickListener(ExpenseAttachmentActivity.this);
                imgPreview = (ImageView) view.findViewById(R.id.attachmentPreview);
                attachmentFileName = (TextView) view.findViewById(R.id.attachmentFileName);
                btnRemoveAttachment = (ImageView) view.findViewById(R.id.btnRemoveAttachment);
            }
        }

        @Override
        public void onBindViewHolder(ExpenseAttachmentHolder holder, int position) {
            ODataRow values = oDataRows.get(position);
            String fileName = values.getString("name");
            String type = values.getString("file_type");
            String datas_fname = values.getString("datas_fname");
            String[] image_ext = new  String[]{".png",".jpg",".jpeg"};
            for (String s : image_ext){
                if(datas_fname.contains(s)){
                    type = "image";
                    break;
                }
            }
            if (type.contains("image")) {
                if (!values.getString("file_uri").equals("false")) {
                    Uri uri = Uri.parse(new File(values.getString("file_uri")).toString());
                    holder.imgPreview.setImageBitmap(oFileManager.getBitmapFromURI(uri));
                } else{
                    holder.imgPreview.setImageResource(R.drawable.image);
                }
            } else if (type.contains("audio")) {
                holder.imgPreview.setImageResource(R.drawable.audio);
            } else if (type.contains("video")) {
                holder.imgPreview.setImageResource(R.drawable.video);
            } else {
                holder.imgPreview.setImageResource(R.drawable.file);
            }
            holder.attachmentFileName.setText(fileName);
            holder.view.setTag(values);
            holder.btnRemoveAttachment.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return oDataRows.size();
        }

        @Override
        public ExpenseAttachmentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.base_attachment_item,parent,false);
            return new ExpenseAttachmentHolder(view);
        }
        public void swap(List<ODataRow> oDataRows){
            this.oDataRows.clear();
            this.oDataRows.addAll(oDataRows);
            notifyDataSetChanged();
        }
    }

    List<ODataRow> collectLocalData(){
        String where = "res_model = ? and res_id = ?";
        List<String> args = new ArrayList<>();
        args.add(res_model);
        args.add(String.valueOf(res_id));
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return irAttachment.select(null,where,selectionArgs);
    }

    @Override
    public void onRefresh() {
        if(inNetwork()){
            ReadAttachmentsByRecord readAttachmentsByRecord = new ReadAttachmentsByRecord();
            readAttachmentsByRecord.execute();
        }else {
            expenseAttachmentAdapter.swap(collectLocalData());
            expenseAttachmentAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}

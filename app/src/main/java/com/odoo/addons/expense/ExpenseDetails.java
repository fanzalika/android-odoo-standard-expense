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
 * Created on 8/1/15 5:47 PM
 */
package com.odoo.addons.expense;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.customers.utils.ShareUtil;
import com.odoo.addons.expense.models.AccountTax;
import com.odoo.addons.expense.models.HrEmployee;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.addons.expense.models.LocalAttachment;
import com.odoo.addons.expense.models.ProductProduct;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.ir.feature.OFileManager;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OM2MRecord;
import com.odoo.core.orm.OM2ORecord;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.RelValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.OStringColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import odoo.controls.OField;
import odoo.controls.OForm;
import odoo.controls.SearchableItemActivity;


public class ExpenseDetails extends OdooCompatActivity
        implements View.OnClickListener, OField.IOnFieldValueChangeListener {
    public static final String TAG = ExpenseDetails.class.getSimpleName();
    public static String KEY_PARTNER_TYPE = "partner_type";

    public static String MODEL_NAME_TAG = "model_name";
    public static String SERVER_ID_TAG = "server_id";
    public static String RECORD_NAME_TAG = "record_name";
    public static String MODEL_NAME = "hr.expense";
    public static String EXPENSE_IDS_TAG = "ids";
    public static String SHEET_ID_TAG = "sheet_id";
    public static String EXPENSE_SHEET_OPEN_TAG = "expense_sheet_open";

    public static final int EXPENSE_SHEET_REQUEST = 99;
    public static final int ADD_TAX_REQUEST = 77;

    private final String KEY_MODE = "key_edit_mode";
    private final String KEY_NEW_IMAGE = "key_new_image";
    private Bundle extras;
    private HrExpense hrExpense;
    IrAttachment irAttachment;
    LocalAttachment localAttachment;

    private ODataRow record = null;
//    private ImageView userImage = null;
    private OForm mForm;
    EditText quantityEditText;
    EditText unitAmountEditText;

    private App app;
    private Boolean mEditMode = false;
    private Menu mMenu;
    private OFileManager fileManager;
    private ActionBar actionBar;


    TextView attachmentTextView;
    TextView stateTextView;
    Button stateActionBtn;
    ImageView btnAddAttachment;
    TextView pendingTextViewAttachment;

    private LinearLayout attachmentLayout;
    private LinearLayout taxesViewLayout;
    private LinearLayout taxesEditLayout;
    private LinearLayout taxesLinearLayout;
    private RecyclerView taxesEditRecycler;
    ImageButton addImageButton;
    TaxRecyclerAdapter editTaxAdapter;
    List<ODataRow> taxes_ids = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expense_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        attachmentLayout = (LinearLayout) findViewById(R.id.expense_attachments_layout);
        attachmentLayout.setOnClickListener(this);

        fileManager = new OFileManager(this);
        if (actionBar != null)
            actionBar.setTitle("");
        if (savedInstanceState != null) {
            mEditMode = savedInstanceState.getBoolean(KEY_MODE);
        }
        app = (App) getApplicationContext();
        hrExpense = new HrExpense(this, null);
        irAttachment = new IrAttachment(this,null);
        localAttachment = new LocalAttachment(this,null);
        extras = getIntent().getExtras();


        attachmentTextView  = (TextView) findViewById(R.id.expense_attachments);
        int rowId = extras.getInt(OColumn.ROW_ID);
        record = hrExpense.browse(rowId);
        writeAttachmentNumber();
        stateTextView = (TextView) findViewById(R.id.expense_state_text);
        stateActionBtn = (Button) findViewById(R.id.expense_action_btn);
        stateActionBtn.setOnClickListener(this);

        btnAddAttachment = (ImageView) findViewById(R.id.btnAddAttachment);
        btnAddAttachment.setOnClickListener(this);

        pendingTextViewAttachment = (TextView) findViewById(R.id.btnSendAttachment);
        pendingTextViewAttachment.setOnClickListener(this);

        taxesViewLayout = (LinearLayout) findViewById(R.id.taxes_view_layout);
        taxesEditLayout = (LinearLayout) findViewById(R.id.taxes_edit_layout);
        taxesLinearLayout = (LinearLayout) findViewById(R.id.taxes_linear_layout);
        taxesEditRecycler = (RecyclerView) findViewById(R.id.taxes_edit_recycler);
        addImageButton = (ImageButton) findViewById(R.id.add_img_button);
        addImageButton.setOnClickListener(this);

        taxes_ids.add(new ODataRow());
        editTaxAdapter = new TaxRecyclerAdapter();
        taxesEditRecycler.setAdapter(editTaxAdapter);

        if (!hasRecordInExtra())
            mEditMode = true;
        setupToolbar();
    }

    private boolean hasRecordInExtra() {
        return extras != null && extras.containsKey(OColumn.ROW_ID);
    }

    private void setMode(Boolean edit) {

        int setVisible = View.VISIBLE;
        String read_only_state = "done,post";
        if(!edit || (record != null && record.get("state")!= null && read_only_state.contains(record.getString("state")))){
            setVisible = View.GONE;
        }
        addImageButton.setVisibility(setVisible);

        if (mMenu != null) {
            mMenu.findItem(R.id.menu_customer_detail_more).setVisible(!edit);
            mMenu.findItem(R.id.menu_customer_edit).setVisible(!edit);
            mMenu.findItem(R.id.menu_customer_save).setVisible(edit);
            mMenu.findItem(R.id.menu_customer_cancel).setVisible(edit);
        }
        int color = Color.DKGRAY;
        if (record != null) {
            color = OStringColorUtil.getStringColor(this, record.getString("name"));
        }
        taxesViewLayout.removeAllViews();
        taxesEditLayout.removeAllViews();
        if (edit) {
            if (!hasRecordInExtra()) {
                actionBar.setTitle("New");
            }
            mForm = (OForm) findViewById(R.id.expenseFormEdit);

            ((OField) findViewById(R.id.quantity)).setOnValueChangeListener(this);
            ((OField) findViewById(R.id.unit_amount)).setOnValueChangeListener(this);
            ((OField) findViewById(R.id.currency_id)).setOnValueChangeListener(this);
            ((OField) findViewById(R.id.product_id)).setOnValueChangeListener(this);
            findViewById(R.id.expense_view_layout).setVisibility(View.GONE);
            findViewById(R.id.expense_edit_layout).setVisibility(View.VISIBLE);
            taxesEditLayout.addView(taxesLinearLayout);
        } else {
            mForm = (OForm) findViewById(R.id.expenseForm);
            findViewById(R.id.expense_edit_layout).setVisibility(View.GONE);
            findViewById(R.id.expense_view_layout).setVisibility(View.VISIBLE);
            taxesViewLayout.addView(taxesLinearLayout);
        }
        editTaxAdapter.notifyDataSetChanged();
        setColor(color);
    }

    private void setupToolbar() {
        if (!hasRecordInExtra()) {
            pendingTextViewAttachment.setVisibility(View.GONE);
            setMode(mEditMode);
            mForm.setEditable(mEditMode);
            mForm.initForm(null);
            HrEmployee hrEmployee = new HrEmployee(ExpenseDetails.this,null);
            Integer currentEmployeeId = hrEmployee.getCurrentEmployeeId();
            if(currentEmployeeId != null){
                ((OField)mForm.findViewById(R.id.employee_id)).setValue(currentEmployeeId);
            }
            ODataRow oDataRow = new ODataRow();
            oDataRow.put("quantity", hrExpense.getColumn("quantity").getDefaultValue());
            oDataRow.put("date", ODateUtils.getDate());
            try {
                oDataRow.put("currency_id", ResCompany.myCurrency(ExpenseDetails.this));
            }catch (Exception e){
                Log.d(TAG, "setupToolbar: e = "+e.fillInStackTrace());
            }
            mForm.updateFieldData(oDataRow);
//            quantityEditText = null;

            View quantity_view = findViewById(R.id.quantity);
            quantityEditText = (EditText) quantity_view.findViewWithTag("EditText");
//            if(quantityEditText != null)
//                quantityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
//            TYPE_CLASS_NUMBER VS TYPE_NUMBER_FLAG_DECIMAL
//            number : keyboard display as number, not allow input fractional value
//            decimal : keyboard display as text, allow input fractional value
//            unitAmountEditText = null;
            View unit_amount_view = findViewById(R.id.unit_amount);
            unitAmountEditText = (EditText) unit_amount_view.findViewWithTag("EditText");
//            if (unitAmountEditText != null)
//                unitAmountEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else {
            record = hrExpense.browse(record.getInt(OColumn.ROW_ID));
            int pendingAttachmentCount = localAttachment.getPendingAttachmentCount(hrExpense.getModelName(),record.getInt(OColumn.ROW_ID));
            pendingTextViewAttachment.setText(String.valueOf(pendingAttachmentCount));
            pendingTextViewAttachment.setVisibility(pendingAttachmentCount > 0 ? View.VISIBLE : View.GONE);

            String state_string = record.getString("state");
            switch (state_string){
                case "draft":
                    stateActionBtn.setText("Submit to Manager");
                    break;
                default:
                    stateActionBtn.setText("View Report");
                    break;
            }
            HashMap<String,String> hashMap = hrExpense.getColumn("state").getSelectionMap();
            if(hashMap.containsKey(state_string)){
                state_string = hashMap.get(state_string);
            }
            stateTextView.setText(state_string);
//            checkControls();
            setMode(mEditMode);
            mForm.setEditable(mEditMode);
            mForm.initForm(record);
            actionBar.setTitle(record.getString("name"));

            taxes_ids.clear();
            taxes_ids.addAll(record.getM2MRecord("tax_ids").browseEach());
            if(taxes_ids.size() == 0)
                taxes_ids.add(new ODataRow());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_img_button:
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
                Intent searchable_intent = new Intent(this, SearchableItemActivity.class);
                searchable_intent.putExtra("model", "hr.expense");
                searchable_intent.putExtra("search_hint", "Tax");
                searchable_intent.putExtra("column_name", "tax_ids");
                searchable_intent.putExtra("live_search", true);
                searchable_intent.putExtra("resource_id", -1);
                searchable_intent.putExtra(OColumn.ROW_ID, -1);
                broadcastManager.registerReceiver(valueReceiver, new IntentFilter("searchable_value_select"));
                startActivity(searchable_intent);
                break;
            case R.id.full_address:
                IntentUtils.redirectToMap(this, record.getString("full_address"));
                break;
            case R.id.website:
                IntentUtils.openURLInBrowser(this, record.getString("website"));
                break;
            case R.id.email:
                IntentUtils.requestMessage(this, record.getString("email"));
                break;
            case R.id.phone_number:
                IntentUtils.requestCall(this, record.getString("phone"));
                break;
            case R.id.mobile_number:
                IntentUtils.requestCall(this, record.getString("mobile"));
                break;
//            case R.id.captureImage:
//                fileManager.requestForFile(OFileManager.RequestType.IMAGE_OR_CAPTURE_IMAGE);
//                break;
            case R.id.expense_attachments_layout:
                Intent intent = new Intent(ExpenseDetails.this,ExpenseAttachmentActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(MODEL_NAME_TAG,MODEL_NAME);
                bundle.putInt(SERVER_ID_TAG,hrExpense.selectServerId(record.getInt(OColumn.ROW_ID)));
                bundle.putString(RECORD_NAME_TAG,record.getString("name"));

                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.expense_action_btn:
                if(record != null && record.get("state") != null){
//                    switch (record.getString("state")){
//                        case "draft":
//                            OValues oValues = record.toValues();
//                            oValues.put("state","reported");
//                            hrExpense.update(record.getInt(OColumn.ROW_ID),oValues);
//                            setupToolbar();
//                            break;
//                    }
                    Intent expenseSheetIntent = new Intent(ExpenseDetails.this,ExpenseSheetActivity.class);
                    Bundle extra = new Bundle();
                    ArrayList<Integer> ids = new ArrayList<>();
                    ids.add(record.getInt(OColumn.ROW_ID));
                    extra.putIntegerArrayList(EXPENSE_IDS_TAG,ids);
                    extra.putBoolean(EXPENSE_SHEET_OPEN_TAG,record.getString("state").equals("draft"));
                    expenseSheetIntent.putExtras(extra);
                    startActivityForResult(expenseSheetIntent,EXPENSE_SHEET_REQUEST);
                }
                break;
            case R.id.btnAddAttachment:
                fileManager.requestForFile(OFileManager.RequestType.IMAGE_OR_CAPTURE_IMAGE);
                break;
            case R.id.btnSendAttachment:
                if(inNetwork()){
                    new CreateAttachments().execute();
                }else {
                    Toast.makeText(this, "This action need internet connection", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

//    private void checkControls() {
//        findViewById(R.id.full_address).setOnClickListener(this);
//        findViewById(R.id.website).setOnClickListener(this);
//        findViewById(R.id.email).setOnClickListener(this);
//        findViewById(R.id.phone_number).setOnClickListener(this);
//        findViewById(R.id.mobile_number).setOnClickListener(this);
//    }


    private void setColor(int color) {
        mForm.setIconTintColor(color);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_customer_save:
                ODataRow oDataRow = new ODataRow();
                if(quantityEditText != null){
                    oDataRow.put("quantity",quantityEditText.getText());
                }
                if(unitAmountEditText != null){
                    oDataRow.put("unit_amount",unitAmountEditText.getText());
                }
                if(oDataRow.size() > 0){
                    mForm.updateFieldData(oDataRow);
                }
                OValues values = mForm.getValues();
                if (values != null) {
                    if (record != null) {
                        hrExpense.update(record.getInt(OColumn.ROW_ID), values);
                        Toast.makeText(this, R.string.toast_information_saved, Toast.LENGTH_LONG).show();
                        mEditMode = !mEditMode;
                        setupToolbar();
                    } else {
                        int row_id = hrExpense.insert(values);
                        if (row_id != OModel.INVALID_ROW_ID) {
                            finish();
                        }
                    }
                }
                break;
            case R.id.menu_customer_cancel:
            case R.id.menu_customer_edit:

                quantityEditText = null;
                unitAmountEditText = null;

                if (hasRecordInExtra()) {
                    mEditMode = !mEditMode;
                    setMode(mEditMode);
                    ((OField) findViewById(R.id.quantity)).setOnValueChangeListener(null);
                    ((OField) findViewById(R.id.unit_amount)).setOnValueChangeListener(null);
                    ((OField) findViewById(R.id.currency_id)).setOnValueChangeListener(null);
                    ((OField) findViewById(R.id.product_id)).setOnValueChangeListener(null);
                    mForm.setEditable(mEditMode);

                    mForm.initForm(record);

                    ((OField) findViewById(R.id.quantity)).setOnValueChangeListener(this);
                    ((OField) findViewById(R.id.unit_amount)).setOnValueChangeListener(this);
                    ((OField) findViewById(R.id.currency_id)).setOnValueChangeListener(this);
                    ((OField) findViewById(R.id.product_id)).setOnValueChangeListener(this);

                    View quantity_view = findViewById(R.id.quantity);
                    quantityEditText = (EditText) quantity_view.findViewWithTag("EditText");
                    if(quantityEditText != null)
                        quantityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                    View unit_amount_view = findViewById(R.id.unit_amount);
                    unitAmountEditText = (EditText) unit_amount_view.findViewWithTag("EditText");
                    if(unitAmountEditText != null)
                        unitAmountEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                } else {
                    finish();
                }
                break;
            case R.id.menu_customer_share:
                ShareUtil.shareContact(this, record, true);
                break;
            case R.id.menu_customer_import:
                ShareUtil.shareContact(this, record, false);
                break;
            case R.id.menu_customer_delete:
                OAlert.showConfirm(this, OResource.string(this,
                        R.string.confirm_are_you_sure_want_to_delete),
                        new OAlert.OnAlertConfirmListener() {
                            @Override
                            public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                                if (type == OAlert.ConfirmType.POSITIVE) {
                                    // Deleting record and finishing activity if success.
                                    if (hrExpense.delete(record.getInt(OColumn.ROW_ID))) {
                                        Toast.makeText(ExpenseDetails.this, R.string.toast_record_deleted,
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            }
                        });

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_detail, menu);
        mMenu = menu;
        setMode(mEditMode);
        return true;
    }

    @Override
    public void onFieldValueChange(OField field, Object value) {
        switch (field.getFieldName()){
            case "product_id":
                ODataRow newValues = new ODataRow();
                ODataRow product_row = (ODataRow) value;
                if (mForm.getValue("name") == null || String.valueOf(mForm.getValue("name")).length() == 0)
                    newValues.put("name",product_row.getString("display_name"));
                newValues.put("unit_amount",HrExpense.price_compute(product_row,"standard_price",null,null));
                newValues.put("product_uom_id",product_row.getInt("uom_id"));

                int account_id = ProductProduct._get_product_accounts(product_row);
                if(account_id != OModel.INVALID_ROW_ID){
                    newValues.put("account_id",account_id);
                }

                mForm.updateFieldData(newValues);
                editTaxAdapter.swap(product_row.getM2MRecord("supplier_taxes_id").browseEach());
                break;
            case "quantity":

                if(value != null && !value.equals("") && !value.equals("false"))
                    _compute_amount(Float.valueOf(String.valueOf(value)), null, null);
                break;
            case "unit_amount":
                if(value != null && !value.equals("") && !value.equals("false"))
                    _compute_amount(null, Float.valueOf(String.valueOf(value)), null);
                break;
            case "currency_id":
//                if(value != formValues.get("currency_id"))
//                    _compute_amount();
                break;
            case "is_company":
                Boolean checked = Boolean.parseBoolean(value.toString());
                int view = (checked) ? View.GONE : View.VISIBLE;
                findViewById(R.id.parent_id).setVisibility(view);
                break;
        }
    }

    private class BigImageLoader extends AsyncTask<Integer, Void, String> {

        @Override
        protected String doInBackground(Integer... params) {
            String image = null;
            try {
                Thread.sleep(300);
                OdooFields fields = new OdooFields();
                fields.addAll(new String[]{"image_medium"});
                OdooResult record = hrExpense.getServerDataHelper().read(null, params[0]);
                if (record != null && !record.getString("image_medium").equals("false")) {
                    image = record.getString("image_medium");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return image;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                if (!result.equals("false")) {
                    OValues values = new OValues();
                    values.put("large_image", result);
                    hrExpense.update(record.getInt(OColumn.ROW_ID), values);
                    record.put("large_image", result);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_MODE, mEditMode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case EXPENSE_SHEET_REQUEST:
                if(data != null && data.getExtras() != null){
                    UpdateHrExpense updateHrExpense = new UpdateHrExpense();
                    switch (data.getExtras().getString("action")){
                        case "create":
                            OM2ORecord om2ORecord = new OM2ORecord(hrExpense,hrExpense.getColumn("sheet_id"),data.getExtras().getInt("id"));
                            OValues oValues = new OValues();
                            oValues.put("sheet_id", om2ORecord );
                            hrExpense.update(record.getInt(OColumn.ROW_ID),oValues);
                            record = hrExpense.browse(record.getInt(OColumn.ROW_ID));
                            updateHrExpense.execute(record);
                            break;
                        case "update":
                            record = hrExpense.browse(record.getInt(OColumn.ROW_ID));
                            updateHrExpense.execute(record);
                            break;

                    }
                }else {
                    Log.d(TAG, "onActivityResult: data == null");
                }
                break;
            default:
                OValues values = fileManager.handleResult(requestCode, resultCode, data);
                if (values != null && !values.contains("size_limit_exceed")) {

                    boolean isImage = (values.getString("file_type").contains("image"));
                    values.put("datas", BitmapUtils.uriToBase64(
                            Uri.parse(values.getString("file_uri"))
                            , getContentResolver(), isImage
                    ));
                    values.put("res_model",hrExpense.getModelName());
                    values.put("res_id_local",record.getInt(OColumn.ROW_ID));
                    localAttachment.insert(values);
                    setupToolbar();
                } else if (values != null) {
                    Toast.makeText(this, R.string.toast_image_size_too_large, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    public boolean inNetwork() {
        App app = (App) getApplicationContext();
        return app.inNetwork();
    }
    private class UpdateHrExpense extends AsyncTask<ODataRow, Void, Void> {
        protected Void doInBackground(ODataRow... params) {
            ODomain oDomain = new ODomain();
            oDomain.add("id","=",params[0].getFloat("id").intValue());
            hrExpense.quickSyncRecords(oDomain);
            return null;
        }

        protected void onPostExecute() {
            Log.d(TAG, "onPostExecute: ");
        }
    }
    public OUser user(){
        return OUser.current(ExpenseDetails.this);
    }


    private class CreateAttachments extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setupToolbar();
            progressDialog = new ProgressDialog(ExpenseDetails.this);
            progressDialog.setTitle(R.string.title_working);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage("Uploading attachments...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            String where = "res_model = ? and res_id_local = ?";
            List<String> args = new ArrayList<>();
            args.add("hr.expense");
            args.add(String.valueOf(record.getInt(OColumn.ROW_ID)));
            String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
            List<ODataRow> oDataRows = localAttachment.select(null,where,selectionArgs);
            progressDialog.setMax(oDataRows.size());
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                int id = 0;
                String where = "res_model = ? and res_id_local = ?";
                List<String> args = new ArrayList<>();
                args.add("hr.expense");
                args.add(String.valueOf(record.getInt(OColumn.ROW_ID)));
                String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
                List<ODataRow> oDataRows = localAttachment.select(null,where,selectionArgs);
                for (ODataRow row : oDataRows){
                    OValues value = row.toValues();
                    ORecordValues data = IrAttachment.valuesToData(irAttachment, value, hrExpense.getModelName(),
                            record.getInt("id"));
                    if (data != null) {
                        id = irAttachment.getServerDataHelper().createOnServer(data);
                        value.put("id", id);
                        value.put("file_uri","false");
                        irAttachment.createAttachment(value, hrExpense.getModelName(),
                                record.getInt("id"));
                        if(id > 0 && record != null && record.get("attachment_number") != null){
                            int number_of_attachment = record.getInt("attachment_number");
                            OValues oValues = record.toValues();
                            oValues.put("attachment_number",number_of_attachment + 1);
                            hrExpense.update(record.getInt(OColumn.ROW_ID),oValues);
                            record = hrExpense.browse(record.getInt(OColumn.ROW_ID));
                            localAttachment.delete(row.getInt(OColumn.ROW_ID),true);
                            publishProgress();
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "doInBackground: e = "+e.fillInStackTrace());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            String where = "res_model = ? and res_id_local = ?";
            List<String> args = new ArrayList<>();
            args.add("hr.expense");
            args.add(String.valueOf(record.getInt(OColumn.ROW_ID)));
            String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
            List<ODataRow> oDataRows = localAttachment.select(null,where,selectionArgs);
            progressDialog.setProgress(progressDialog.getMax()-oDataRows.size());
            writeAttachmentNumber();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setupToolbar();
            progressDialog.dismiss();
        }
    }
    void writeAttachmentNumber(){
        int number_of_attachment = 0;
        if(record != null && record.get("attachment_number") != null){
            number_of_attachment = record.getInt("attachment_number");
        }
        attachmentTextView.setText(getString(R.string.attachments_label,number_of_attachment));
    }
    private class UpdateProduct extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: ");
            progressDialog = new ProgressDialog(ExpenseDetails.this);
            progressDialog.setTitle(R.string.title_working);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
//                String subject = params[0];
//                String body = params[1];
//                OArguments args = new OArguments();
//                HashMap<String, Object> data = new HashMap<>();
//                data.put("body", body);

                OArguments args = new OArguments();
                Object o = hrExpense.getServerDataHelper().callMethod("_onchange_product_id", args, null, null);
                Log.d(TAG, "doInBackground: 0 = "+o.toString());
                return true;
            } catch (Exception e) {
                Log.d(TAG, "doInBackground: e = "+e.fillInStackTrace());
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();

        }
    }

    class TaxRecyclerAdapter extends RecyclerView.Adapter{
        class EmptyViewHolder extends RecyclerView.ViewHolder{
            EmptyViewHolder(View view){
                super(view);
            }
        }
        class TaxItemViewHolder extends RecyclerView.ViewHolder{
            TextView textView;
            ImageView imageView;
            TaxItemViewHolder(View view){
                super(view);
                textView = (TextView) view.findViewById(R.id.text_view);
                imageView = (ImageView) view.findViewById(R.id.image_view);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        taxes_ids.remove(getAdapterPosition());
                        if(taxes_ids.size() == 0){
                            taxes_ids.add(new ODataRow());
                        }

                        RelValues relValues = new RelValues();
                        ODataRow newValues = new ODataRow();
                        for(ODataRow oDataRow : taxes_ids){
                            if(oDataRow.get(OColumn.ROW_ID) != null)
                                relValues.append(oDataRow.getInt(OColumn.ROW_ID));
                        }
                        newValues.put("tax_ids",relValues);
                        mForm.updateFieldData(newValues);
                        notifyDataSetChanged();
                        _compute_amount(null,null,null);
                    }
                });
            }
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if(viewType == 0){
                view = LayoutInflater.from(ExpenseDetails.this).inflate(R.layout.no_tax_viewholder,parent,false);
                return new EmptyViewHolder(view);
            }else {
                view = LayoutInflater.from(ExpenseDetails.this).inflate(R.layout.tax_item_viewholder,parent,false);
                return new TaxItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (viewHolder instanceof TaxItemViewHolder){
                TaxItemViewHolder holder = (TaxItemViewHolder) viewHolder;
                holder.textView.setText(taxes_ids.get(position).getString("name"));
                int setVisible = View.VISIBLE;
                String read_only_state = "done,post";
                if(!mEditMode || (record != null && record.get("state")!= null && read_only_state.contains(record.getString("state")))){
                    setVisible = View.GONE;
                }
                holder.imageView.setVisibility(setVisible);
            }
        }

        @Override
        public int getItemCount() {
            return taxes_ids.size();
        }

        @Override
        public int getItemViewType(int position) {
            if(taxes_ids.get(position).get(OColumn.ROW_ID) == null){
                return 0;
            }else {
                return 1;
            }
        }
        public void swap(List<ODataRow> oDataRows){
            taxes_ids.clear();
            taxes_ids.addAll(oDataRows);
            if(taxes_ids.size() == 0){
                taxes_ids.add(new ODataRow());
            }
            RelValues relValues = new RelValues();
            ODataRow newValues = new ODataRow();
            for(ODataRow oDataRow : taxes_ids){
                if(oDataRow.get(OColumn.ROW_ID) != null)
                    relValues.append(oDataRow.getInt(OColumn.ROW_ID));
            }
            newValues.put("tax_ids",relValues);
            mForm.updateFieldData(newValues);
            notifyDataSetChanged();
            _compute_amount(null,null,null);
        }
        public void add(ODataRow oDataRow){
            if(taxes_ids.size() == 1 && taxes_ids.get(0).get(OColumn.ROW_ID) == null){
                taxes_ids.clear();
            }
            taxes_ids.add(oDataRow);
            RelValues relValues = new RelValues();
            ODataRow newValues = new ODataRow();
            for(ODataRow row : taxes_ids){
                if(row.get(OColumn.ROW_ID) != null)
                    relValues.append(row.getInt(OColumn.ROW_ID));
            }
            newValues.put("tax_ids",relValues);
            mForm.updateFieldData(newValues);
            notifyDataSetChanged();
            _compute_amount(null,null,null);
        }
    }
    public void compute_amount(List<ODataRow> tax_ids,ODataRow product_row,Float quantity,Float unit_amount, Integer currency_id){
        Log.d(TAG, "compute_amount: =============================");
        OValues values = mForm.getValues(false);
        ODataRow self = new ODataRow();
        String[] prev_key = {"quantity","unit_amount","currency_id","tax_ids","product_id"};
        for (String s : values.keys()){
            if(!Arrays.asList(prev_key).contains(s)){
                self.put(s,values.get(s));
            }
        }
        if(quantity == null){
            if(values.getString("quantity").equals("false")){
                quantity = Float.parseFloat("0");
            }else {
                quantity = Float.parseFloat(values.getString("quantity"));
            }
        }
        if(unit_amount == null){
            if(values.getString("unit_amount").equals("false")){
                unit_amount = Float.parseFloat("0");
            }else {
                unit_amount = Float.parseFloat(values.getString("unit_amount"));
            }
        }

        ODataRow oDataRow = hrExpense._compute_amount(self,tax_ids,product_row,quantity,unit_amount,currency_id);
        mForm.updateFieldData(oDataRow);
    }

    public void _compute_amount(Float quantity,Float unit_amount, Integer currency_id){
        OValues values = mForm.getValues(false);
        ODataRow oDataRow = values.toDataRow();
        if(oDataRow.size() > 0 && oDataRow.get("product_id") != null){
            ODataRow product_row;
            if(oDataRow.get("product_id") instanceof OM2ORecord){
                product_row = oDataRow.getM2ORecord("product_id").browse();
            }else {
                ProductProduct productProduct = new ProductProduct(this,null);
                product_row = productProduct.browse(oDataRow.getInt("product_id"));
            }
            List<ODataRow> tax_ids = new ArrayList<>();
            AccountTax accountTax = new AccountTax(this,null);
            for(ODataRow tax_row : taxes_ids){
                if(tax_row.get(OColumn.ROW_ID) != null){
                    int tax_id = tax_row.getInt(OColumn.ROW_ID);
                    tax_ids.add(accountTax.browse(tax_id));
                }
            }
            compute_amount(tax_ids,product_row,quantity,unit_amount,currency_id);
        }
    }

    BroadcastReceiver valueReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("tax_ids".equals(intent.getStringExtra("column_name"))) {
                int row_id = intent.getIntExtra("selected_position", -1);
                if(row_id != -1){
                    AccountTax accountTax = new AccountTax(ExpenseDetails.this,null);
                    ODataRow row = accountTax.browse(row_id);
                    editTaxAdapter.add(row);
                }else {
                    Log.d(TAG, "onReceive: row_id == -1");
                }
                LocalBroadcastManager.getInstance(ExpenseDetails.this).unregisterReceiver(valueReceiver);
            }
        }
    };
}
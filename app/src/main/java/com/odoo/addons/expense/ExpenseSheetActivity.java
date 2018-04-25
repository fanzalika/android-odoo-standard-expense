package com.odoo.addons.expense;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.expense.models.HrEmployee;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.addons.expense.models.HrExpenseSheet;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.RelValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.support.OUser;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.OAlert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.controls.OField;
import odoo.controls.OForm;

public class ExpenseSheetActivity extends OdooCompatActivity implements View.OnClickListener{
    public static final String TAG = ExpenseSheetActivity.class.getSimpleName();
    HrExpense hrExpense;
    List<ODataRow> expense_records = new ArrayList<>();
    ODataRow expense_sheet_record = new ODataRow();
    HrExpenseSheet hrExpenseSheet;
    Bundle extras;
    ArrayList<Integer> ids = new ArrayList<>();
    ActionBar actionBar;
    OForm oForm;
    OField paymentModeField;

    Button sheetActionButton;
    Button sheetActionButton2;
    TextView expenseStateTextView;

    Menu mMenu;
    private boolean mEditMode = false;

    RecyclerView recyclerView;
    ExpenseAdapter adapter;
    boolean enablePaymentField = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extras = getIntent().getExtras();
        hrExpenseSheet = new HrExpenseSheet(this,user());
        hrExpense = new HrExpense(this,null);

        if(extras.get(OColumn.ROW_ID) != null && extras.getInt(OColumn.ROW_ID) > 0){
            int rowId = extras.getInt(OColumn.ROW_ID);
            expense_sheet_record = hrExpenseSheet.browse(rowId);
            Log.d(TAG, "onCreate: expense_sheet_record = "+expense_sheet_record);
        }else {
            ids = extras.getIntegerArrayList(ExpenseDetails.EXPENSE_IDS_TAG);
            for(int id : ids){
                expense_records.add(hrExpense.browse(id));
            }
            if(expense_records.size() == 1 ){
                if(expense_records.get(0).get("sheet_id") != null && !expense_records.get(0).getString("sheet_id").equals("false")){
                    expense_sheet_record = expense_records.get(0).getM2ORecord("sheet_id").browse();
                }else {
                    expense_sheet_record.put("state",hrExpenseSheet.getColumn("state").getDefaultValue());
                    expense_sheet_record.put("name",expense_records.get(0).getString("name"));
                    expense_sheet_record.put("date",expense_records.get(0).getString("date"));
                    expense_sheet_record.put("employee_id",expense_records.get(0).getInt("employee_id"));
                    expense_sheet_record.put("payment_mode",expense_records.get(0).getString("payment_mode"));
                }
            }
        }
        setContentView(R.layout.activity_expense_sheet);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        oForm = (OForm)findViewById(R.id.expense_sheet_form);
        paymentModeField = (OField) findViewById(R.id.payment_mode);
        paymentModeField.setOnValueChangeListener(new OField.IOnFieldValueChangeListener() {
            @Override
            public void onFieldValueChange(OField field, Object value) {
                if(!enablePaymentField){
                    enablePaymentField = true;
                    if(adapter.getExpenses().size() > 0){
                        paymentModeField.setValue(adapter.getExpenses().get(0).getString("payment_mode"));
                    }else {
                        paymentModeField.setValue(hrExpenseSheet.getColumn("payment_mode").getDefaultValue());
                    }
                    enablePaymentField = false;
                }
            }
        });
        sheetActionButton = (Button) findViewById(R.id.sheet_action_btn);
        sheetActionButton.setOnClickListener(this);
        sheetActionButton2 = (Button) findViewById(R.id.sheet_action_btn2);
        sheetActionButton2.setOnClickListener(this);
        expenseStateTextView = (TextView) findViewById(R.id.expense_state_text);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.expense_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ExpenseAdapter();
        recyclerView.setAdapter(adapter);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mEditMode = expense_sheet_record.get(OColumn.ROW_ID) == null;
        setUpForm(mEditMode);
    }
    void setUpForm(boolean isEdit){
        String actionBarTitle = "New";
        if(expense_sheet_record != null && expense_sheet_record.get("name") != null){
            actionBarTitle = expense_sheet_record.getString("name");
        }else {
            if(expense_records.size() == 1 ){
                if(expense_records.get(0).get("sheet_id") != null && !expense_records.get(0).getString("sheet_id").equals("false")){
                    actionBarTitle = expense_sheet_record.getString("name");
                }else {
                    actionBarTitle = expense_records.get(0).getString("name")+" (Report)";
                }
            }
        }
        actionBar.setTitle(actionBarTitle);

        List<ODataRow> expenses = new ArrayList<>();
        if(expense_sheet_record != null && expense_sheet_record.get("expense_line_ids") != null){
            expenses = expense_sheet_record.getO2MRecord("expense_line_ids").browseEach();
        }else{
            for (int id : ids){
                expenses.add(hrExpense.browse(id));
            }
        }
        adapter.swap(expenses);
        if(adapter.getExpenses().size() > 0){
            paymentModeField.setValue(adapter.getExpenses().get(0).getString("payment_mode"));
        }else {
            paymentModeField.setValue(hrExpenseSheet.getColumn("payment_mode").getDefaultValue());
        }
        if (mMenu != null) {
            mMenu.findItem(R.id.menu_expense_sheet_edit).setVisible(!isEdit);
            mMenu.findItem(R.id.menu_expense_sheet_save).setVisible(isEdit);
            mMenu.findItem(R.id.menu_expense_sheet_cancel).setVisible(isEdit);
        }
        oForm.setEditable(isEdit);
        String exp_sheet_state = expense_sheet_record.size() == 0 ? hrExpenseSheet.getColumn("state").getDefaultValue().toString() :
                                expense_sheet_record.getString("state");
        switch (exp_sheet_state){
            case "submit":
                sheetActionButton.setVisibility(View.VISIBLE);
                sheetActionButton.setText("Approve");
                sheetActionButton2.setVisibility(View.VISIBLE);
                sheetActionButton2.setText("Refuse");
                break;
            case "cancel":
                sheetActionButton.setVisibility(View.VISIBLE);
                sheetActionButton.setText("Resubmit");
                sheetActionButton2.setVisibility(View.GONE);
                break;
            default:
                sheetActionButton.setVisibility(View.GONE);
                sheetActionButton2.setVisibility(View.GONE);
                break;
        }
        HashMap<String,String> hashMap = hrExpenseSheet.getColumn("state").getSelectionMap();
        if(hashMap.containsKey(exp_sheet_state)){
            exp_sheet_state = hashMap.get(exp_sheet_state);
        }
        expenseStateTextView.setText(exp_sheet_state);

        if(expense_sheet_record.size() == 0){
            oForm.initForm(null);
            HrEmployee hrEmployee = new HrEmployee(ExpenseSheetActivity.this,null);
            Integer current_emp_id = hrEmployee.getCurrentEmployeeId();
            if(current_emp_id != null){
                ((OField)oForm.findViewById(R.id.employee_id)).setValue(current_emp_id);
            }
        }else {
            oForm.initForm(expense_sheet_record);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_expense_sheet,menu);
        mMenu = menu;
        setUpForm(mEditMode);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_expense_sheet_save:
                Log.d(TAG, "onOptionsItemSelected: case R.id.menu_expense_sheet_save");
                OValues oValues = oForm.getValues();
                if(oValues != null){
                    RelValues relValues = new RelValues();
                    for (ODataRow row : adapter.getExpenses()){
                        relValues.append(row.getInt(OColumn.ROW_ID));
                    }
                    oValues.put("expense_line_ids",relValues);
                    if(expense_sheet_record.get(OColumn.ROW_ID) == null){
//                    create
                        Log.d(TAG, "onOptionsItemSelected: create");
                        oValues.put("state","submit");
                        int row_id = hrExpenseSheet.insert(oValues);
                        if(row_id != OModel.INVALID_ROW_ID){
                            expense_sheet_record = hrExpenseSheet.browse(row_id);
                            Log.d(TAG, "onOptionsItemSelected: expense_sheet_record = "+expense_sheet_record);
                        }
                    }else {
//                    update
                        Log.d(TAG, "onOptionsItemSelected: update");
                        hrExpenseSheet.update(expense_sheet_record.getInt(OColumn.ROW_ID),oValues);
                        expense_sheet_record = hrExpenseSheet.browse(expense_sheet_record.getInt(OColumn.ROW_ID));
                        Log.d(TAG, "onOptionsItemSelected: expense_sheet_record = "+expense_sheet_record);
                    }
                    mEditMode = !mEditMode;
                    setUpForm(mEditMode);
                }
                break;
            case R.id.menu_expense_sheet_cancel:
            case R.id.menu_expense_sheet_edit:
                if (extras != null) {
                    mEditMode = !mEditMode;
                    setUpForm(mEditMode);
                } else {
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean inNetwork() {
        App app = (App) getApplicationContext();
        return app.inNetwork();
    }

    public OUser user(){
        return OUser.current(ExpenseSheetActivity.this);
    }

    private class CreateHrExpenseSheet extends AsyncTask<ODataRow, Void, Integer> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ExpenseSheetActivity.this);
            progressDialog.setMessage("Connecting server..");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(ODataRow... params) {
            OValues expense_sheet_values = params[0].toValues();
            expense_sheet_values.put("state","submit");
            RelValues relValues = new RelValues();
            for (int id : ids){
                relValues.append(id);
            }
            expense_sheet_values.put("expense_line_ids",relValues);

            return hrExpenseSheet.insert(expense_sheet_values);
        }

        @Override
        protected void onPostExecute(Integer id) {
            super.onPostExecute(id);
            progressDialog.dismiss();
            Intent output = new Intent();
            output.putExtra("id", id);
            output.putExtra("action","create");
            setResult(RESULT_OK, output);
            finish();
        }
    }

    private class UpdateHrExpenseSheet extends AsyncTask<Object, Void, Integer> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ExpenseSheetActivity.this);
            progressDialog.setMessage("Connecting server..");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Object... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Integer ids) {
            super.onPostExecute(ids);
            Toast.makeText(ExpenseSheetActivity.this, "id = "+ids, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        OValues oValues = oForm.getValues();
        switch (v.getId()){
            case R.id.sheet_action_btn:
                if(oValues != null){
                    if(expense_sheet_record.get(OColumn.ROW_ID) == null){
//                    insert data
                        int row_id = hrExpenseSheet.insert(oValues);
                        if(row_id != OModel.INVALID_ROW_ID){
                            expense_sheet_record = hrExpenseSheet.browse(row_id);
                        }else {
                            break;
                        }
                    }
                    if(expense_sheet_record.getString("state").equals("cancel")){
                        oValues.put("state","submit");
                    }else {
                        oValues.put("state","approve");
                        ResUsers resUsers = new ResUsers(ExpenseSheetActivity.this,null);
                        oValues.put("responsible_id",resUsers.selectRowId(user().getUserId()));
                    }
                    RelValues relValues = new RelValues();
                    for (ODataRow row : adapter.getExpenses()){
                        relValues.append(row.getInt(OColumn.ROW_ID));
                    }
                    oValues.put("expense_line_ids",relValues);
//                    oValues.put("total_amount",hrExpenseSheet._compute_amount(oValues));
                    hrExpenseSheet.update(expense_sheet_record.getInt(OColumn.ROW_ID),oValues);
                    expense_sheet_record = hrExpenseSheet.browse(expense_sheet_record.getInt(OColumn.ROW_ID));
                    mEditMode = false;
                    setUpForm(mEditMode);
                }
                break;
            case R.id.sheet_action_btn2:
                if(oValues != null){
                    if(expense_sheet_record.get(OColumn.ROW_ID) == null){
//                    if record not found
                        int id = hrExpenseSheet.insert(oValues);
                        expense_sheet_record = hrExpenseSheet.browse(id);
                    }
                    openCancelWizard();
                }
                break;
        }
    }

    void openCancelWizard(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ExpenseSheetActivity.this);
        builder.setTitle("Reason to refuse expense.");
        final EditText editText = new EditText(ExpenseSheetActivity.this);
        builder.setView(editText);
        builder.setCancelable(false);
        builder.setNegativeButton("Refuse", null);
        builder.setPositiveButton("Cancel",null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(editText.getText().length() > 0){
                            OValues oValues = new OValues();
                            oValues.put("state","cancel");
                            oValues.put("refuse_reason",editText.getText().toString());
                            hrExpenseSheet.update(expense_sheet_record.getInt(OColumn.ROW_ID),oValues);
                            expense_sheet_record = hrExpenseSheet.browse(expense_sheet_record.getInt(OColumn.ROW_ID));
                            alertDialog.dismiss();
                            mEditMode = false;
                            setUpForm(mEditMode);
                        }else {
                            editText.setError("Refuse reason required");
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private class ExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        List<ODataRow> expenses = new ArrayList<>();

        class ExpenseViewHolder extends RecyclerView.ViewHolder{
            View view;
            TextView date;
            TextView tax_ids;
            TextView attach_num;
            TextView total_amount;
            TextView expenseName;
            ImageView deleteBtn;
            ExpenseViewHolder(View view){
                super(view);
                this.view = view;
                date = (TextView) view.findViewById(R.id.date);
                tax_ids = (TextView) view.findViewById(R.id.tax_ids);
                attach_num = (TextView) view.findViewById(R.id.attachment_number);
                total_amount = (TextView) view.findViewById(R.id.total_amount);
                expenseName = (TextView) view.findViewById(R.id.expense_name);
                deleteBtn = (ImageView) view.findViewById(R.id.delete_btn);
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OAlert.showConfirm(ExpenseSheetActivity.this, "Do you really want to remove these records ?", new OAlert.OnAlertConfirmListener() {
                            @Override
                            public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                                expenses.remove(getAdapterPosition());
                                notifyDataSetChanged();
                            }
                        });
                    }
                });
            }
        }
        class ExpenseHeaderViewHolder extends RecyclerView.ViewHolder{
            ExpenseHeaderViewHolder(View view){
                super(view);
            }
        }
        class ExpenseFooterViewHolder extends RecyclerView.ViewHolder{
            View view;
            TextView totalAmount;
            View deleteBtnSpace;
            ExpenseFooterViewHolder(View view){
                super(view);
                this.view = view;
                this.totalAmount = (TextView) view.findViewById(R.id.total_amount);
                this.deleteBtnSpace = view.findViewById(R.id.delete_btn_space);
            }
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType){
                case 0:
                    view = LayoutInflater.from(ExpenseSheetActivity.this).inflate(R.layout.expense_header_item_in_sheet,parent,false);
                    return new ExpenseHeaderViewHolder(view);
                case 1:
                    view = LayoutInflater.from(ExpenseSheetActivity.this).inflate(R.layout.expense_footer_item_in_sheet,parent,false);
                    return new ExpenseFooterViewHolder(view);
                default:
                    view = LayoutInflater.from(ExpenseSheetActivity.this).inflate(R.layout.expense_row_item_in_sheet,parent,false);
                    return new ExpenseViewHolder(view);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0){
                return 0;
            }else if(position == getItemCount()-1){
                return 1;
            }else return 2;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if(viewHolder instanceof ExpenseViewHolder){
                ExpenseViewHolder holder = (ExpenseViewHolder) viewHolder;
                if(position % 2 == 1){
                    holder.view.setActivated(true);
                }
                holder.date.setText(expenses.get(position).getString("date"));
                holder.expenseName.setText(expenses.get(position).getString("name"));
                holder.attach_num.setText(
                        String.valueOf(hrExpense.compute_attachment_number(expenses.get(position).toValues())));
                List<ODataRow> taxIds = expenses.get(position).getM2MRecord("tax_ids").browseEach();
                String tax_text = "";
                for (ODataRow oDataRow : taxIds){
                    if(tax_text.length() > 0){
                        tax_text+=", ";
                    }
                    tax_text+= oDataRow.getString("name");
                }
                holder.tax_ids.setText(tax_text);
                holder.total_amount.setText(expenses.get(position).getString("total_amount"));

                int setVisible = View.VISIBLE;
                String read_only_state = "approve,done,post";
                if(!mEditMode || (expense_sheet_record != null && expense_sheet_record.get("state")!= null && read_only_state.contains(expense_sheet_record.getString("state")))){
                    setVisible = View.GONE;
                }
                holder.deleteBtn.setVisibility(setVisible);
            }else if(viewHolder instanceof ExpenseFooterViewHolder){
                ExpenseFooterViewHolder holder = (ExpenseFooterViewHolder) viewHolder;
                if(position % 2 == 1){
                    holder.view.setActivated(true);
                }
                String totalAmount = "";
                if(expense_sheet_record.size() == 0 || expense_sheet_record.getString("total_amount").equals("false")){
                    totalAmount = String.valueOf(hrExpenseSheet._compute_amount(getExpenses()));
                }else {
                    totalAmount = expense_sheet_record.getString("total_amount");
                }
                holder.totalAmount.setText(totalAmount);

                holder.deleteBtnSpace.setVisibility(
                        mEditMode ? View.VISIBLE : View.GONE
                );
            }
        }

        void swap(List<ODataRow> oDataRows){
            expenses.clear();
            expenses.add(new ODataRow());
            expenses.addAll(oDataRows);
            expenses.add(new ODataRow());
            notifyDataSetChanged();
        }

        public List<ODataRow> getExpenses() {
            List<ODataRow> oDataRows = new ArrayList<>();
            if(expenses.size() > 2){
                oDataRows.addAll(expenses.subList(1,expenses.size()-1));
            }
            return oDataRows;
        }
    }
}

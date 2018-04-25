package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OO2MRecord;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rahma on 11/09/2017.
 */

public class HrExpenseSheet extends OModel {
    public static final String TAG = HrExpenseSheet.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.hr_expense_sheet";
    OColumn name = new OColumn("Expense Report Summary", OVarchar.class)
            .setRequired();
    OColumn expense_line_ids = new OColumn("Expense Lines",HrExpense.class, OColumn.RelationType.OneToMany)
            .addReadOnlyRule("state","approve,done,post")
            .setRelatedColumn("sheet_id");
    OColumn state = new OColumn("Status", OSelection.class)
            .addSelection("submit", "Submitted")
            .addSelection("approve","Approved")
            .addSelection("post","Posted")
            .addSelection("done","Paid")
            .addSelection("cancel","Refused")
            .setRequired()
            .setDefaultValue("submit")
            .addReadOnlyRule("state","all");
    OColumn employee_id = new OColumn("Employee",HrEmployee.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","approve,done,post,cancel")
            .setRequired();
    OColumn address_id = new OColumn("Employee Home Address", ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn payment_mode = new OColumn("Payment By",OSelection.class)
            .addSelection("own_account","Employee (to reimburse)")
            .addSelection("company_account","Company")
            .setRelTableName("hr.expense")
            .setRelBaseColumn("expense_line_ids")
            .setRelRelationColumn("payment_mode")
            .setDefaultValue("own_account")
            .addReadOnlyRule("state","all");
    OColumn responsible_id = new OColumn("Validation By", ResUsers.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","approve,done,post,cancel");
    OColumn total_amount = new OColumn("Total Amount",OFloat.class).setRequired();
    OColumn company_id = new OColumn("Company", ResCompany.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","approve,done,post,cancel");
    OColumn currency_id = new OColumn("Currency", ResCurrency.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","approve,done,post,cancel");
    OColumn attachment_number = new OColumn("Number of Attachments", OInteger.class);
    OColumn accounting_date = new OColumn("Accounting Date",ODate.class);
    OColumn account_move_id = new OColumn("Journal Entry", AccountMove.class, OColumn.RelationType.ManyToOne);
//    local column
    OColumn refuse_reason = new OColumn("Reason to refuse expense.",OVarchar.class).setLocalColumn();

    public HrExpenseSheet(Context context, OUser user){
        super(context,"hr.expense.sheet",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public static ORecordValues valuesToData(OValues value) {
        ORecordValues data = new ORecordValues();
        data.put("name", value.get("name"));
        data.put("db_datas", value.getString("datas"));
        data.put("datas_fname", value.get("name"));
        data.put("file_size", value.get("file_size"));
        return data;
    }

    public float _compute_amount(List<ODataRow> oDataRows){
        Log.d(TAG, "_compute_amount: oDataRows.size()= "+oDataRows.size());
        Log.d(TAG, "_compute_amount: oDataRows = "+oDataRows);
        float amount = 0;
        for (ODataRow row : oDataRows){
            amount += row.getFloat("total_amount");
        }
        return amount;
    }
    public float _compute_amount(OValues oValues){
        float amount = 0;
        try {
            HrExpense hrExpense = new HrExpense(getContext(),null);
            if(oValues.get("expense_line_ids") instanceof ArrayList){
                for (Double id : (ArrayList<Double> )oValues.get("expense_line_ids")){
                    ODataRow oDataRow = hrExpense.browse(hrExpense.selectRowId(id.intValue()));
                    amount += Float.parseFloat(oDataRow.getString("total_amount"));
                }
            }else if(oValues.get("expense_line_ids") instanceof OO2MRecord){
                ODataRow oDataRow = oValues.toDataRow();
                amount = _compute_amount(oDataRow.getO2MRecord("expense_line_ids").browseEach());
            }
        }catch (Exception e){
            Log.d(TAG, "_compute_amount: e = "+e.fillInStackTrace());
        }
        return amount;
    }
    public float _compute_amount(int id){
        return _compute_amount(browse(id).toValues());
    }
}

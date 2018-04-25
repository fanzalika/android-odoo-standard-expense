package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.odoo.BuildConfig;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OM2ORecord;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rahma on 11/09/2017.
 */

public class HrExpense extends OModel {
    public static final String TAG = HrExpense.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.hr_expense";
    OColumn name = new OColumn("Expense Description", OVarchar.class)
            .addReadOnlyRule("state","reported,done")
            .setRequired();
    OColumn date = new OColumn("Date", ODate.class)
            .addReadOnlyRule("state","reported,done");
    OColumn employee_id = new OColumn("Employee",HrEmployee.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","reported,done")
            .setRequired();
    OColumn product_id = new OColumn("Product",ProductProduct.class, OColumn.RelationType.ManyToOne)
            .addDomain("can_be_expensed","=",true)
            .addReadOnlyRule("state","reported,done")
            .setRequired();
    OColumn product_uom_id = new OColumn("Unit of Measure",ProductUom.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","reported,done")
            .setRequired();
    OColumn unit_amount = new OColumn("Unit Price", OFloat.class)
            .setDefaultValue(0)
            .addReadOnlyRule("state","reported,done")
            .setRequired();
    OColumn quantity = new OColumn("Quantity",OFloat.class)
            .setRequired()
            .addReadOnlyRule("state","reported,done")
            .setDefaultValue(1);
    OColumn tax_ids = new OColumn("Taxes",AccountTax.class, OColumn.RelationType.ManyToMany)
            .addReadOnlyRule("state","done,post");
    OColumn untaxed_amount = new OColumn("Subtotal",OFloat.class);
    OColumn total_amount = new OColumn("Total Amount",OFloat.class).setRequired();
    OColumn currency_id = new OColumn("Currency", ResCurrency.class, OColumn.RelationType.ManyToOne);
//    OColumn account_id = new OColumn("Account")
    OColumn payment_mode = new OColumn("Payment By",OSelection.class)
            .addSelection("own_account","Employee (to reimburse)")
            .addSelection("company_account","Company")
            .addReadOnlyRule("state","done,post")
            .setDefaultValue("own_account");
    @Odoo.Functional (method = "compute_attachment_number", depends = {}, store = true)
    OColumn attachment_number = new OColumn("Number of Attachments", OInteger.class).setLocalColumn().setDefaultValue(0);
    @Odoo.Functional (method = "compute_state", depends = {"sheet_id"}, store = true)
    OColumn state = new OColumn("Status", OSelection.class).setLocalColumn()
            .addSelection("draft", "To Submit")
            .addSelection("reported","Reported")
            .addSelection("done","Posted")
            .addSelection("refused","Refused")
            .addReadOnlyRule("state","all");
    OColumn sheet_id = new OColumn("Expense Report",HrExpenseSheet.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","all");
    OColumn reference = new OColumn("Bill Reference",OVarchar.class);
    OColumn description = new OColumn("Notes..",OText.class);
    OColumn account_id = new OColumn("Account",AccountAccount.class, OColumn.RelationType.ManyToOne)
            .addReadOnlyRule("state","done,post");

    public HrExpense(Context context, OUser user){
        super(context,"hr.expense",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public int compute_attachment_number(OValues value) {
        ODataRow row = value.toDataRow();
        IrAttachment irAttachment = new IrAttachment(getContext(),null);
        int attachment_number = irAttachment.count("res_model = ? and res_id = ? ", new String[]{this.getModelName(),String.valueOf(row.getInt("id").intValue())});
        return attachment_number;
    }
    public String compute_state(OValues value) {
        try {
            ODataRow row = value.toDataRow();
            if(row.get("sheet_id") == null || row.getString("sheet_id").equals("false") || row.getFloat("sheet_id").intValue() == 0){
                return "draft";
            }else {
                int sheet_id = row.getFloat("sheet_id").intValue();
                HrExpenseSheet hrExpenseSheet = new HrExpenseSheet(getContext(),null);
                ODataRow sheet_id_row = hrExpenseSheet.browse(sheet_id);
                if (sheet_id_row.getString("state").equals("cancel")){
                    return "refused";
                }else if(sheet_id_row.get("account_move_id") == null || sheet_id_row.getString("account_move_id").equals("false")){
                    return "reported";
                }else {
                    return "done";
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "compute_state: e = "+e.fillInStackTrace());
        }
        return "draft";
    }

    public ODataRow _compute_amount(ODataRow self,List<ODataRow> tax_ids,ODataRow product_row,Float quantity,Float unit_amount, Integer currency_id){
        self.put("untaxed_amount",unit_amount * quantity);
        AccountTax accountTax = new AccountTax(getContext(),null);
        ODataRow currency_row;
        ODataRow partner_id_row = new ODataRow();


        int partner_id = OModel.INVALID_ROW_ID;
        if(self.get("employee_id") != null){
            ODataRow employee_id;
            if(self.get("employee_id") instanceof OM2ORecord){
                employee_id = self.getM2ORecord("employee_id").browse();
            }else {
                int emp_id = self.getInt("employee_id");
                HrEmployee hrEmployee = new HrEmployee(getContext(),null);
                employee_id = hrEmployee.browse(emp_id);
            }
            if(employee_id!=null && employee_id.get("user_id") != null){
                ODataRow user_id = employee_id.getM2ORecord("user_id").browse();
                if(user_id != null && user_id.get("partner_id") != null)
                    if(user_id.get("partner_id") instanceof OM2ORecord){
                        partner_id_row = user_id.getM2ORecord("partner_id").browse();
                    }else {
                        partner_id = user_id.getInt("partner_id");
                    }
            }
        }
        if(partner_id_row == null || partner_id_row.size() == 0){
            ResPartner resPartner = new ResPartner(getContext(),null);
            if(partner_id == OModel.INVALID_ROW_ID){
                partner_id = resPartner.selectRowId(getUser().getPartnerId());
            }
            partner_id_row = resPartner.browse(partner_id);
        }
        if(self.get("currency_id") != null && self.get("currency_id") instanceof OM2ORecord){
            currency_row = self.getM2ORecord("currency_id").browse();
        }else {
            ResCurrency resCurrency = new ResCurrency(getContext(),null);

            if(currency_id == null){
                if(self.get("currency_id") == null){
                    self.put("currency_id", ResCompany.myCurrency(getContext()));
                }
                currency_id = self.getInt("currency_id");
            }
            currency_row =  resCurrency.browse(currency_id);
        }
        HashMap<String,Object> taxes = accountTax.compute_all(tax_ids,
                unit_amount, currency_row,
               quantity, product_row,
                partner_id_row,null);
        self.put("total_amount", taxes.get("total_included"));
        return self;
    }

    public static float price_compute(ODataRow row, String price_type, @Nullable ODataRow uom_row, @Nullable ODataRow currency_row){
        float price = 0;
        if(!row.getString(price_type).equals("false")){
            price = row.getFloat(price_type);
            if (price_type.equals("list_price"))
                price += row.getFloat("price_extra");
            if(uom_row != null)
                price = ProductUom._compute_price(price,uom_row);
            if(currency_row != null)
                price = ResCurrency.compute(price,currency_row);
        }
        return price;
    }

    @Override
    public void onSyncFinished() {
        super.onSyncFinished();
    }
}

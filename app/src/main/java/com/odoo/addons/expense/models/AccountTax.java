package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rahma on 11/09/2017.
 */

public class AccountTax extends OModel {
    public static final String TAG = AccountTax.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.account_tax";
    OColumn name = new OColumn("Tax Name", OVarchar.class)
            .setRequired();
    OColumn amount_type = new OColumn("Tax Computation", OSelection.class)
            .setRequired()
            .setDefaultValue("percent")
            .addSelection("group","Group of Taxes")
            .addSelection("fixed","Fixed")
            .addSelection("percent","Percentage of Price")
            .addSelection("division","Percentage of Price Tax Included");
    OColumn company_id = new OColumn("Company", ResCompany.class, OColumn.RelationType.ManyToOne);
    OColumn children_tax_ids = new OColumn("Children Taxes",AccountTax.class, OColumn.RelationType.ManyToMany)
            .setRelTableName("account_tax_filiation_rel")
            .setRelBaseColumn("parent_tax")
            .setRelRelationColumn("child_tax");
    OColumn sequence = new OColumn("Sequence", OInteger.class)
            .setRequired()
            .setDefaultValue(1);
    OColumn amount = new OColumn("Amount", OFloat.class).setRequired();
    OColumn price_include = new OColumn("Included in Price", OBoolean.class).setDefaultValue(false);
    OColumn include_base_amount = new OColumn("Affect Base of Subsequent Taxes",OBoolean.class).setDefaultValue(false);

    public AccountTax(Context context, OUser user){
        super(context,"account.tax",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
    public HashMap<String, Object> compute_all(List<ODataRow> self, float price_unit, ODataRow currency,
                              float quantity, @Nullable ODataRow product, @Nullable ODataRow partner, @Nullable HashMap<String,Object> ctx){

        int prec = ResCurrency._compute_decimal_places(currency);
        double total_excluded, total_included, base;
        ODataRow company_id;
        if(self.size() == 0){
            ResCompany resCompany = new ResCompany(getContext(),null);
            company_id = resCompany.browse(resCompany.selectRowId(getUser().getCompanyId()));
        }else {
            company_id = self.get(0).getM2ORecord("company_id").browse();
        }

        List<ODataRow> taxes = new ArrayList<>();
        boolean round_tax = !company_id.getString("tax_calculation_rounding_method").equals("round_globally");
        boolean round_total = true;
        if(ctx != null && ctx.keySet().contains("round")){
            round_tax = (Boolean) ctx.get("round");
            round_total = (Boolean) ctx.get("round");
        }
        if (!round_tax)
            prec += 5;
        if(ctx == null)
            total_excluded = total_included = base = round(price_unit * quantity,prec);
        else{
            total_excluded = (Double)ctx.get("total_excluded");
            total_included = (Double)ctx.get("total_included");
            base = (Double)ctx.get("base");
        }
//        sort tax (self)
        Collections.sort(self,new Comparator<ODataRow>(){
            @Override
            public int compare(ODataRow o1, ODataRow o2) {
                return o1.getInt("sequence").compareTo(o2.getInt("sequence"));
            }
        });
        double tax_amount;
        for (ODataRow oDataRow : self){
            if(oDataRow.getString("amount_type").equals("group")){
                List<ODataRow> children = oDataRow.get("children_tax_ids") == null ? null : oDataRow.getM2MRecord("children_tax_ids").browseEach();
                HashMap<String,Object> childrenCtx = new HashMap<>();
                childrenCtx.put("total_excluded",total_excluded);
                childrenCtx.put("total_included",total_included);
                childrenCtx.put("base",base);
                HashMap<String,Object> ret = compute_all(children,price_unit,currency,quantity,product,partner,childrenCtx);
                total_excluded = (Double)ret.get("total_excluded");
                if (oDataRow.getBoolean("include_base_amount"))
                    base = (Double)ret.get("base");
                total_included = (Double)ret.get("total_included");
                tax_amount = total_included - total_excluded;
                taxes.addAll((List<ODataRow>) ret.get("taxes"));
                continue;
            }
            tax_amount = _compute_amount(oDataRow,base,price_unit,quantity,product,partner);
            if(!round_tax)
                tax_amount = round(tax_amount,prec);
            else
                tax_amount = ResCurrency.round(currency,tax_amount);
            if (oDataRow.getBoolean("price_include")){
                total_excluded -= tax_amount;
                base -= tax_amount;
            }else
                total_included += tax_amount;
            double tax_base = base;
            if(oDataRow.getBoolean("include_base_amount"))
                base += tax_amount;
            ODataRow tax_row = new ODataRow();
            tax_row.put("id",oDataRow.get("id"));
            tax_row.put("name",oDataRow.get("name"));
            tax_row.put("amount",tax_amount);
            tax_row.put("base",base);
            tax_row.put("sequence",oDataRow.get("sequence"));
            tax_row.put("account_id",oDataRow.getM2ORecord("account_id"));
            tax_row.put("refund_account_id",oDataRow.getM2ORecord("refund_account_id"));
            tax_row.put("analytic",oDataRow.get("analytic"));
            taxes.add(tax_row);
        }
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("taxes",taxes);
        hashMap.put("total_excluded",round_total ? ResCurrency.round(currency,total_excluded) : total_excluded);
        hashMap.put("total_included",total_included);
        hashMap.put("base",base);
        return hashMap;
    }
    public double _compute_amount(ODataRow self,double base_amount, double price_unit, double quantity, @Nullable ODataRow product, @Nullable ODataRow partner){
        if(self.getString("amount_type").equals("fixed")){
            if (base_amount > 0)
                return Math.copySign(quantity, base_amount) * self.getFloat("amount");
            else
                return quantity * self.getFloat("amount");
        }
        if((self.getString("amount_type").equals("percent") && !self.getBoolean("price_include"))
                || self.getString("amount_type").equals("division") && self.getBoolean("price_include")){
            return base_amount * self.getFloat("amount") / 100;
        }
        if(self.getString("amount_type").equals("percent") && self.getBoolean("price_include")) {
            return base_amount - (base_amount / (1 + self.getFloat("amount")/100));
        }
        if(self.getString("amount_type").equals("division") && !self.getBoolean("price_include"))
            return base_amount / (1 - self.getFloat("amount")/100) - base_amount;
//        this value (below) is not expected to be returned
        return (double) OModel.INVALID_ROW_ID;
    }
    public static double round(double number,int prec){
        double scale = Math.pow(10d, prec);
        return Math.round(number * scale)/scale;
    }
}

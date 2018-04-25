package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 11/09/2017.
 */

public class ProductProduct extends OModel {
    public static final String TAG = ProductProduct.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.product_product";
    OColumn name = new OColumn("Name", OVarchar.class)
            .setRequired();
    OColumn display_name = new OColumn("Display Name",OVarchar.class);
    OColumn uom_id = new OColumn("Unit of Measure",ProductUom.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn price = new OColumn("Price", OFloat.class);
    @Odoo.Functional(method = "_compute_product_price_extra")
    OColumn price_extra = new OColumn("Variant Price Extra",OFloat.class);
    OColumn standard_price = new OColumn("Cost",OFloat.class);
//    inherit from product.template
    OColumn supplier_taxes_id = new OColumn("Vendor Taxes",AccountTax.class, OColumn.RelationType.ManyToMany);
    OColumn property_account_expense_id = new OColumn("Expense Account",AccountAccount.class, OColumn.RelationType.ManyToOne)
            .addDomain("deprecated","=",false);
    OColumn can_be_expensed = new OColumn("Can be Expensed", OBoolean.class);


    public ProductProduct(Context context, OUser user){
        super(context,"product.product",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
//  return default expense account
    public static int _get_product_accounts(ODataRow row){
        if(row.get("property_account_expense_id") != null){
            return row.getInt("property_account_expense_id");
        }
        if(row.get("categ_id") != null){
//            self.categ_id.property_account_expense_categ_id
            // TODO: 10/10/2017
        }
        return INVALID_ROW_ID;
    }
    public static float _compute_product_price_extra(ODataRow row){
        float price_extra = 0;
        // TODO: 11/10/2017
        return price_extra;
    }
}

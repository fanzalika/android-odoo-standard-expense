package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 11/09/2017.
 */

public class ProductUom extends OModel {
    public static final String TAG = ProductUom.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.product_uom";
    OColumn name = new OColumn("Name", OVarchar.class)
            .setRequired();

    public ProductUom(Context context, OUser user){
        super(context,"product.uom",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public static float _compute_price(float price, ODataRow row){
        // TODO: 11/10/2017
        return price;
    }
}

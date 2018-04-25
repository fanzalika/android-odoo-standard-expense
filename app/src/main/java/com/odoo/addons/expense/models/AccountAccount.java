package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 11/09/2017.
 */

public class AccountAccount extends OModel {
    public static final String TAG = AccountAccount.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.account_account";
    OColumn name = new OColumn("Name", OVarchar.class)
            .setRequired();

    public AccountAccount(Context context, OUser user){
        super(context,"account.account",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}

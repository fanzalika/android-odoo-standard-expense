package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 11/09/2017.
 */

public class ResourceResource extends OModel {
    public static final String TAG = ResourceResource.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.resource_resource";
    OColumn name = new OColumn("Name", OVarchar.class)
            .setRequired();
    OColumn user_id = new OColumn("User", ResUsers.class, OColumn.RelationType.ManyToOne);
    OColumn resource_type = new OColumn("Resource Type",OVarchar.class)
            .setRequired()
            .setDefaultValue("user");

    public ResourceResource(Context context, OUser user){
        super(context,"resource.resource",user);
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}

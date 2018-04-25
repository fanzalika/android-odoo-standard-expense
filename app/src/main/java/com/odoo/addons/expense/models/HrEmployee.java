package com.odoo.addons.expense.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 11/09/2017.
 */

public class HrEmployee extends OModel {
    public static final String TAG = HrEmployee.class.getSimpleName();

    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.hr_employee";
    OColumn name = new OColumn("Name", OVarchar.class)
            .setRequired();
//    OColumn resource_id = new OColumn("Resource",ResourceResource.class, OColumn.RelationType.ManyToOne)
//            .setRequired();
//    OColumn resource_type = new OColumn("Resource Type",OVarchar.class)
//            .setDefaultValue("user");
    OColumn user_id = new OColumn("User", ResUsers.class, OColumn.RelationType.ManyToOne);

    public HrEmployee(Context context, OUser user){
        super(context,"hr.employee",user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public ODataRow getUserId(Context context,int row_id) {
        HrEmployee hrEmployee = new HrEmployee(context, null);
        return hrEmployee.browse(row_id).getM2ORecord("user_id").browse();
    }
    public Integer getCurrentEmployeeId(){
        Integer emp_id = null;
        for (ODataRow row : select()){
            if(row.get("user_id") != null && row.getM2ORecord("user_id").browse().getInt("id").equals(getUser().getUserId())){
                emp_id = row.getInt(OColumn.ROW_ID);
                break;
            }
        }
        return emp_id;
    }
}

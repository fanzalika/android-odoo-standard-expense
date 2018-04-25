package com.odoo.addons.expense.models;

import android.content.Context;
import android.util.Log;

import com.odoo.base.addons.res.ResCompany;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;

/**
 * Created by rahma on 28/09/2017.
 */

public class LocalAttachment extends OModel {
    OColumn name = new OColumn("Name", OVarchar.class).setLocalColumn();;
    OColumn datas_fname = new OColumn("Data file name", OText.class).setLocalColumn();;
    OColumn file_size = new OColumn("File Size", OInteger.class).setLocalColumn();;
    OColumn res_model = new OColumn("Model", OVarchar.class).setSize(100).setLocalColumn();;
    OColumn file_type = new OColumn("Content Type", OVarchar.class).setSize(100).setLocalColumn();
    OColumn company_id = new OColumn("Company", ResCompany.class,
            OColumn.RelationType.ManyToOne).setLocalColumn();;
    OColumn res_id_local = new OColumn("Resource id", OInteger.class).setDefaultValue(0).setLocalColumn();;
    OColumn scheme = new OColumn("File Scheme", OVarchar.class).setSize(100)
            .setLocalColumn();
    // Local Column
    OColumn file_uri = new OColumn("File URI", OVarchar.class).setSize(150)
            .setLocalColumn().setDefaultValue(false);
    OColumn type = new OColumn("Type", OText.class).setLocalColumn();
    OColumn datas = new OColumn("File BLOB", OBlob.class).setLocalColumn().setRequired();

    public LocalAttachment(Context context, OUser user){
        super(context,"local.attachment",user);
    }


    @Override
    public boolean allowCreateRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowUpdateRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowDeleteRecordOnServer() {
        return false;
    }

    public int getPendingAttachmentCount(String res_model_string, int res_id_int){
        try {
            String selection =  "res_model = ? and res_id_local = ?";
            String[] selectionArgs = {res_model_string,String.valueOf(res_id_int)};
            return count(selection,selectionArgs);
        }catch (Exception e){
            Log.d(TAG, "getPendingAttachmentCount: e = "+e.fillInStackTrace());
        }
        return 0;
    }

    public static ORecordValues valuesToData(OModel model, OValues value, String rel_model, int res_id_local) {
        ORecordValues data = new ORecordValues();
        data.put("name", value.get("name"));
        data.put("datas_fname", value.get("name"));
        data.put("file_size", value.get("file_size"));
        data.put("db_datas", value.getString("datas"));
        data.put("res_model", rel_model);
        data.put("res_id_local", res_id_local);
        data.put("file_type", value.get("file_type"));
        data.put("company_id", model.getUser().getCompanyId());
        return data;
    }
}

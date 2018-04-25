/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 31/12/14 12:41 PM
 */
package com.odoo.base.addons.ir;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.addons.expense.models.LocalAttachment;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.rpc.helper.utils.gson.OdooRecord;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;

import java.util.ArrayList;
import java.util.List;


public class IrAttachment extends OModel {
    public static final String TAG = IrAttachment.class.getSimpleName();
    public static final String AUTHORITY  = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.ir_attachment";

    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn datas_fname = new OColumn("Data file name", OText.class);
    OColumn file_size = new OColumn("File Size", OInteger.class);
    OColumn res_model = new OColumn("Model", OVarchar.class).setSize(100);
    OColumn file_type = new OColumn("Content Type", OVarchar.class).setSize(100).setLocalColumn();
    OColumn company_id = new OColumn("Company", ResCompany.class,
            OColumn.RelationType.ManyToOne);
    OColumn res_id = new OColumn("Resource id", OInteger.class).setDefaultValue(0);
    OColumn scheme = new OColumn("File Scheme", OVarchar.class).setSize(100)
            .setLocalColumn();
    // Local Column
    OColumn file_uri = new OColumn("File URI", OVarchar.class).setSize(150)
            .setLocalColumn().setDefaultValue(false);
    OColumn type = new OColumn("Type", OText.class).setLocalColumn();

    public IrAttachment(Context context, OUser user) {
        super(context, "ir.attachment", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
    public boolean createAttachment(OValues value, String rel_model, int res_id) {
        OValues values = new OValues();
        values.put("name", value.get("name"));
        values.put("datas_fname", value.getString("name"));
        values.put("file_size", value.get("file_size"));
        values.put("file_type", value.get("file_type"));
        values.put("company_id", getUser().getCompanyId());
        values.put("res_id", res_id);
        values.put("res_model", rel_model);
        values.put("file_uri", value.getString("file_uri"));
        values.put("type", value.getString("file_type"));
        values.put("id", value.get("id"));
        insert(values);
        return true;
    }

    public static ORecordValues valuesToData(OModel model, OValues value, String rel_model, int res_id) {
        ORecordValues data = new ORecordValues();
        data.put("name", value.get("name"));
        data.put("db_datas", value.getString("datas"));
        data.put("datas_fname", value.get("name"));
        data.put("file_size", value.get("file_size"));
        data.put("res_model", rel_model);
        data.put("res_id", res_id);
        data.put("file_type", value.get("file_type"));
        data.put("company_id", model.getUser().getCompanyId());
        return data;
    }

    public static ORecordValues valuesToData(OModel model, OValues value) {
        ORecordValues data = new ORecordValues();
        data.put("name", value.get("name"));
        data.put("db_datas", value.getString("datas"));
        data.put("datas_fname", value.get("name"));
        data.put("file_size", value.get("file_size"));
        data.put("res_model", false);
        data.put("res_id", false);
        data.put("file_type", value.get("file_type"));
        data.put("company_id", model.getUser().getCompanyId());
        return data;
    }

    public String getDatasFromServer(Integer row_id) {
        ODomain domain = new ODomain();
        domain.add("id", "=", selectServerId(row_id));
        OdooFields fields = new OdooFields();
        fields.addAll(new String[]{"datas"});
        OdooResult result = getServerDataHelper().read(fields, selectServerId(row_id));
        if (result != null && result.has("result") && result.get("result") instanceof ArrayList) {
            OdooRecord res = (OdooRecord) result.getArray("result").get(0);
            return res.getString("datas");
        }
        if (result != null && result.has("datas")) {
            return result.getString("datas");
        }
        return "false";
    }

    @Override
    public void onSyncFinished() {
        super.onSyncFinished();
        Log.d(TAG, "onSyncFinished: ");
        new SyncLocalAttachment().execute();

    }
    private class SyncLocalAttachment extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "doInBackground: Try");
                LocalAttachment localAttachment = new LocalAttachment(getContext(),null);
                HrExpense hrExpense = new HrExpense(getContext(),null);
                String where = "res_model = ?";
                List<String> args = new ArrayList<>();
                args.add("hr.expense");
                String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
                List<ODataRow> oDataRows = localAttachment.select(null,where,selectionArgs);
                int iterate = 0;
                for (ODataRow row : oDataRows){
                    Log.d(TAG, "doInBackground: iterasi ~"+String.valueOf(iterate+1));
                    OValues value = row.toValues();
                    int res_id = hrExpense.selectServerId(row.getInt("res_id_local"));
                    ORecordValues data = IrAttachment.valuesToData(IrAttachment.this, value, "hr.expense",
                            res_id);
                    if (data != null) {
                        int id = getServerDataHelper().createOnServer(data);

                        ODataRow oDataRow = new ODataRow();
                        oDataRow.put("id",id);
                        value.put("id", id);
                        value.put("file_uri","false");
                        createAttachment(value, hrExpense.getModelName(),
                                res_id);
                        if(id > 0){
                            Log.d(TAG, "doInBackground: id > 0");
                            ODataRow record = quickCreateRecord(oDataRow);
                            localAttachment.delete(row.getInt(OColumn.ROW_ID),true);
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "doInBackground: e = "+e.fillInStackTrace());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}

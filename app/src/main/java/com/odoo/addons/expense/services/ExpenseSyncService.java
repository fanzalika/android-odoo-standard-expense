/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 2/1/15 11:07 AM
 */
package com.odoo.addons.expense.services;

import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.odoo.addons.expense.models.AccountTax;
import com.odoo.addons.expense.models.HrExpense;
import com.odoo.addons.expense.models.HrExpenseSheet;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.service.ISyncFinishListener;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

import java.util.HashMap;

public class ExpenseSyncService extends OSyncService implements ISyncFinishListener{
    public static final String TAG = ExpenseSyncService.class.getSimpleName();
    Context context;



    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        this.context = context;
        return new OSyncAdapter(context, HrExpense.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {

        switch (adapter.getModel().getModelName()){
            case "hr.expense":
                adapter.syncDataLimit(80);
                adapter.onSyncFinish(this);
                break;
            case "ir.attachment":
                ODomain oDomain = new ODomain();
                oDomain.add("res_model","=","hr.expense");
                adapter.syncDataLimit(80).setDomain(oDomain);
                break;
        }
    }

    @Override
    public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
        return new OSyncAdapter(context, IrAttachment.class,this,true);
    }
}

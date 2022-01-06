/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changehandlers;

import static java.util.Arrays.asList;

import io.harness.changestreamsframework.ChangeEvent;

import software.wings.beans.Account.AccountKeys;

import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountChangeDataHandler extends AbstractChangeDataHandler {
  @Override
  public Map<String, String> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    if (changeEvent == null) {
      return null;
    }
    Map<String, String> columnValueMapping = new HashMap<>();
    DBObject dbObject = changeEvent.getFullDocument();

    columnValueMapping.put("id", changeEvent.getUuid());

    if (dbObject == null) {
      return columnValueMapping;
    }

    if (dbObject.get(AccountKeys.accountName) != null) {
      columnValueMapping.put("name", dbObject.get(AccountKeys.accountName).toString());
    }

    if (dbObject.get(AccountKeys.createdAt) != null) {
      columnValueMapping.put("created_at", dbObject.get(AccountKeys.createdAt).toString());
    }

    return columnValueMapping;
  }

  @Override
  public List<String> getPrimaryKeys() {
    return asList("id");
  }
}

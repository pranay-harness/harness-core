/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changehandlers;

import io.harness.changestreamsframework.ChangeEvent;

import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;

@Slf4j
public class CECloudAccountBigQueryChangeHandler extends AbstractBigQueryChangeDataHandler {
  @Override
  public Map<String, String> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    Map<String, String> columnValueMapping = new HashMap<>();
    DBObject dbObject = changeEvent.getFullDocument();
    columnValueMapping.put(Strings.toUpperCase("id"), changeEvent.getUuid());
    for (String field : fields) {
      columnValueMapping.put(field, dbObject.get(field).toString());
    }
    return columnValueMapping;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.tags;

import static java.util.Arrays.asList;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.HarnessTagLink;
import software.wings.search.SQLOperationHelper;
import software.wings.search.framework.ChangeHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class TagLinkTimescaleChangeHandler implements ChangeHandler {
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    String tableName = "cg_tags";

    switch (changeEvent.getChangeType()) {
      case INSERT:
        dbOperation(SQLOperationHelper.insertSQL(tableName, getColumnValueMapping(changeEvent)));
        break;
      case UPDATE:
        dbOperation(SQLOperationHelper.updateSQL(tableName, getColumnValueMapping(changeEvent),
            Collections.singletonMap("id", changeEvent.getUuid()), getPrimaryKeys()));
        break;
      case DELETE:
        dbOperation(SQLOperationHelper.deleteSQL(tableName, Collections.singletonMap("id", changeEvent.getUuid())));
        break;
      default:
        return false;
    }
    return true;
  }

  public List<String> getPrimaryKeys() {
    return asList("id");
  }

  public boolean dbOperation(String query) {
    boolean successfulOperation = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulOperation && retryCount < 5) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(query)) {
          statement.execute();
          successfulOperation = true;
        } catch (SQLException e) {
          log.error("Failed to save/update/delete data Query = {},retryCount=[{}], Exception: ", query, retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("TimeScale Down");
    }
    return successfulOperation;
  }

  public Map<String, Object> getColumnValueMapping(ChangeEvent<?> changeEvent) {
    Map<String, Object> columnValueMapping = new HashMap<>();
    HarnessTagLink tagLink = (HarnessTagLink) changeEvent.getFullDocument();

    if (changeEvent == null) {
      return null;
    }

    if (tagLink == null) {
      return columnValueMapping;
    }

    // name
    if (tagLink.getUuid() != null) {
      columnValueMapping.put("id", tagLink.getUuid());
    }

    // account_id
    if (tagLink.getAccountId() != null) {
      columnValueMapping.put("account_id", tagLink.getAccountId());
    }

    // app_id
    if (tagLink.getAppId() != null) {
      columnValueMapping.put("app_id", tagLink.getAppId());
    }

    // created_at
    columnValueMapping.put("created_at", tagLink.getCreatedAt());

    // created_by
    if (tagLink.getCreatedBy() != null) {
      columnValueMapping.put("created_by", tagLink.getCreatedBy().getName());
    }

    // last_updated_at
    columnValueMapping.put("last_updated_at", tagLink.getLastUpdatedAt());

    // created_by
    if (tagLink.getKey() != null) {
      columnValueMapping.put("tag_key", tagLink.getKey());
    }

    // created_by
    if (tagLink.getEntityType() != null) {
      columnValueMapping.put("entity_type", tagLink.getEntityType());
    }

    // created_by
    if (tagLink.getEntityId() != null) {
      columnValueMapping.put("entity_id", tagLink.getEntityId());
    }

    columnValueMapping.put("tag_value", tagLink.getValue());

    // last_updated_by
    if (tagLink.getLastUpdatedBy() != null) {
      columnValueMapping.put("last_updated_by", tagLink.getLastUpdatedBy().getName());
    }

    return columnValueMapping;
  }
}

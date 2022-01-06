/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Arrays.asList;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.Service;
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
public class ServiceTimescaleChangeHandler implements ChangeHandler {
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    String tableName = "cg_services";

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
    Service service = (Service) changeEvent.getFullDocument();

    if (changeEvent == null) {
      return null;
    }

    if (service == null) {
      return columnValueMapping;
    }

    // name
    if (service.getUuid() != null) {
      columnValueMapping.put("id", service.getUuid());
    }

    // account_id
    if (service.getAccountId() != null) {
      columnValueMapping.put("account_id", service.getAccountId());
    }

    // app_id
    if (service.getAppId() != null) {
      columnValueMapping.put("app_id", service.getAppId());
    }

    // created_at
    columnValueMapping.put("created_at", service.getCreatedAt());

    // created_by
    if (service.getCreatedBy() != null) {
      columnValueMapping.put("created_by", service.getCreatedBy().getName());
    }

    // name
    columnValueMapping.put("name", service.getName());

    // artifact_stream_ids
    if (!isEmpty(service.getArtifactStreamIds())) {
      columnValueMapping.put("artifact_stream_ids", service.getArtifactStreamIds());
    }

    // version
    columnValueMapping.put("version", service.getVersion());

    // artifact_type
    columnValueMapping.put("artifact_type", service.getArtifactType());

    // last_updated_at
    columnValueMapping.put("last_updated_at", service.getLastUpdatedAt());

    // last_updated_by
    if (service.getLastUpdatedBy() != null) {
      columnValueMapping.put("last_updated_by", service.getLastUpdatedBy().getName());
    }

    if (service.getDeploymentType() != null) {
      columnValueMapping.put("deployment_type", service.getDeploymentType().getDisplayName());
    }

    return columnValueMapping;
  }
}

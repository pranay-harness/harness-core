/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Arrays.asList;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.search.SQLOperationHelper;
import software.wings.search.framework.ChangeHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class WorkflowTimescaleChangeHandler implements ChangeHandler {
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    String tableName = "cg_workflows";

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
    Workflow workflow = (Workflow) changeEvent.getFullDocument();

    if (changeEvent == null) {
      return null;
    }

    if (workflow == null) {
      return columnValueMapping;
    }

    // name
    if (workflow.getUuid() != null) {
      columnValueMapping.put("id", workflow.getUuid());
    }

    if (workflow.getName() != null) {
      columnValueMapping.put("name", workflow.getName());
    }

    // account_id
    if (workflow.getAccountId() != null) {
      columnValueMapping.put("account_id", workflow.getAccountId());
    }

    // orchestration_workflow_type
    if (workflow.getOrchestration() != null && workflow.getOrchestration().getOrchestrationWorkflowType() != null) {
      columnValueMapping.put(
          "orchestration_workflow_type", workflow.getOrchestration().getOrchestrationWorkflowType().toString());
    }

    // env_id
    if (workflow.getAppId() != null) {
      columnValueMapping.put("env_id", workflow.getEnvId());
    }

    // app_id
    if (workflow.getAppId() != null) {
      columnValueMapping.put("app_id", workflow.getAppId());
    }

    // created_at
    columnValueMapping.put("created_at", workflow.getCreatedAt());

    // last_updated_at
    columnValueMapping.put("last_updated_at", workflow.getLastUpdatedAt());

    // created_by
    if (workflow.getCreatedBy() != null) {
      columnValueMapping.put("created_by", workflow.getCreatedBy().getName());
    }

    // last_updated_by
    if (workflow.getLastUpdatedBy() != null) {
      columnValueMapping.put("last_updated_by", workflow.getLastUpdatedBy().getName());
    }

    List<String> deploymentTypes = new LinkedList<>();
    CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestration();
    if (coWorkflow != null && coWorkflow.getWorkflowPhaseIdMap() != null) {
      coWorkflow.getWorkflowPhaseIdMap().values().forEach(workflowPhase -> {
        if (workflowPhase.getDeploymentType() != null) {
          deploymentTypes.add(workflowPhase.getDeploymentType().getDisplayName());
        }
      });
    }

    // deployment_type
    if (!isEmpty(deploymentTypes)) {
      columnValueMapping.put("deployment_type", deploymentTypes);
    }

    // service_ids
    if (workflow.getOrchestration() != null && !isEmpty(workflow.getOrchestration().getServiceIds())) {
      columnValueMapping.put("service_ids", workflow.getOrchestration().getServiceIds());
    }
    return columnValueMapping;
  }
}

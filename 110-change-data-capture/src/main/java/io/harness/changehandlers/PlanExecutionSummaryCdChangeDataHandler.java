/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changehandlers;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class PlanExecutionSummaryCdChangeDataHandler extends AbstractChangeDataHandler {
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
    if (dbObject.get(PlanExecutionSummaryKeys.accountId) != null) {
      columnValueMapping.put("accountId", dbObject.get(PlanExecutionSummaryKeys.accountId).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.orgIdentifier) != null) {
      columnValueMapping.put("orgIdentifier", dbObject.get(PlanExecutionSummaryKeys.orgIdentifier).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.projectIdentifier) != null) {
      columnValueMapping.put("projectIdentifier", dbObject.get(PlanExecutionSummaryKeys.projectIdentifier).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.pipelineIdentifier) != null) {
      columnValueMapping.put(
          "pipelineIdentifier", dbObject.get(PlanExecutionSummaryKeys.pipelineIdentifier).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.planExecutionId) != null) {
      columnValueMapping.put("planExecutionId", dbObject.get(PlanExecutionSummaryKeys.planExecutionId).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.name) != null) {
      columnValueMapping.put("name", dbObject.get(PlanExecutionSummaryKeys.name).toString());
    }
    if (dbObject.get(PlanExecutionSummaryKeys.status) != null) {
      columnValueMapping.put("status", dbObject.get(PlanExecutionSummaryKeys.status).toString());
    }

    if (dbObject.get("moduleInfo") == null) {
      return null;
    }

    // if moduleInfo is not null
    if (((BasicDBObject) dbObject.get("moduleInfo")).get("cd") != null) {
      columnValueMapping.put("moduleInfo_type", "CD");
      // this is a cd deployment pipeline

      // TriggerTypeInfo
      PlanExecutionSummaryChangeDataHandler.commonHandlerTriggerInfo(columnValueMapping, dbObject);

      // CI-relatedInfo
      if (((BasicDBObject) dbObject.get("moduleInfo")).get("ci") != null) {
        PlanExecutionSummaryChangeDataHandler.commonHandlerRepoInfo(columnValueMapping, dbObject);
      }
    } else {
      return null;
    }

    columnValueMapping.put(
        "startTs", String.valueOf(Long.parseLong(dbObject.get(PlanExecutionSummaryKeys.startTs).toString())));
    if (dbObject.get(PlanExecutionSummaryKeys.endTs) != null) {
      columnValueMapping.put("endTs", String.valueOf(dbObject.get(PlanExecutionSummaryKeys.endTs).toString()));
    }

    return columnValueMapping;
  }

  @Override
  public List<String> getPrimaryKeys() {
    return asList("id", "startts");
  }
}

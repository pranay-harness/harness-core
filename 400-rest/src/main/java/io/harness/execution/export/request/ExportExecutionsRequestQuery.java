/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.WorkflowExecution;

import com.mongodb.BasicDBObject;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;

@OwnedBy(CDC)
@Value
@Builder
public class ExportExecutionsRequestQuery {
  String dbObjectJson;

  @SuppressWarnings("deprecation")
  public static ExportExecutionsRequestQuery fromQuery(Query<WorkflowExecution> query) {
    if (query == null) {
      return null;
    }

    BasicDBObject dbObject = (BasicDBObject) query.getQueryObject();
    return ExportExecutionsRequestQuery.builder().dbObjectJson(dbObject == null ? null : dbObject.toJson()).build();
  }

  public static void updateQuery(Query<WorkflowExecution> query, ExportExecutionsRequestQuery requestQuery) {
    if (requestQuery == null || requestQuery.getDbObjectJson() == null || query == null) {
      return;
    }

    if (query instanceof QueryImpl) {
      BasicDBObject dbObject = BasicDBObject.parse(requestQuery.getDbObjectJson());
      ((QueryImpl<WorkflowExecution>) query).setQueryObject(dbObject);
    }
  }
}

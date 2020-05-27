package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.mongodb.BasicDBObject;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import software.wings.beans.WorkflowExecution;

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

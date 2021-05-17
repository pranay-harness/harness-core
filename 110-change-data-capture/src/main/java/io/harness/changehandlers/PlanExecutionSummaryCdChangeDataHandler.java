package io.harness.changehandlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
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
    if (dbObject.get("accountId") != null) {
      columnValueMapping.put("accountId", dbObject.get("accountId").toString());
    }
    if (dbObject.get("orgIdentifier") != null) {
      columnValueMapping.put("orgIdentifier", dbObject.get("orgIdentifier").toString());
    }
    if (dbObject.get("projectIdentifier") != null) {
      columnValueMapping.put("projectIdentifier", dbObject.get("projectIdentifier").toString());
    }
    if (dbObject.get("pipelineIdentifier") != null) {
      columnValueMapping.put("pipelineIdentifier", dbObject.get("pipelineIdentifier").toString());
    }
    if (dbObject.get("name") != null) {
      columnValueMapping.put("name", dbObject.get("name").toString());
    }
    if (dbObject.get("status") != null) {
      columnValueMapping.put("status", dbObject.get("status").toString());
    }

    if (dbObject.get("moduleInfo") == null) {
      return null;
    }

    // if moduleInfo is not null
    if (((BasicDBObject) dbObject.get("moduleInfo")).get("cd") != null) {
      columnValueMapping.put("moduleInfo_type", "CD");
      // this is a cd deployment pipeline
    } else {
      return null;
    }

    columnValueMapping.put("startTs", String.valueOf(Long.parseLong(dbObject.get("startTs").toString())));
    if (dbObject.get("endTs") != null) {
      columnValueMapping.put("endTs", String.valueOf(dbObject.get("endTs").toString()));
    }

    return columnValueMapping;
  }
}

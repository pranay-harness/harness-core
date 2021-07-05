package io.harness.changehandlers;

import static java.util.Arrays.asList;

import io.harness.changestreamsframework.ChangeEvent;
import io.harness.ng.core.entities.Project.ProjectKeys;

import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectsChangeDataHandler extends AbstractChangeDataHandler {
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
    if (dbObject.get(ProjectKeys.identifier) != null) {
      columnValueMapping.put("identifier", dbObject.get(ProjectKeys.identifier).toString());
    }

    if (dbObject.get(ProjectKeys.name) != null) {
      columnValueMapping.put("name", dbObject.get(ProjectKeys.name).toString());
    }

    if (dbObject.get(ProjectKeys.deleted) != null) {
      columnValueMapping.put("deleted", dbObject.get(ProjectKeys.deleted).toString());
    }

    if (dbObject.get(ProjectKeys.orgIdentifier) != null) {
      columnValueMapping.put("org_identifier", dbObject.get(ProjectKeys.orgIdentifier).toString());
    }

    if (dbObject.get(ProjectKeys.lastModifiedAt) != null) {
      columnValueMapping.put("last_modified_at", dbObject.get(ProjectKeys.lastModifiedAt).toString());
    }

    if (dbObject.get(ProjectKeys.accountIdentifier) != null) {
      columnValueMapping.put("account_identifier", dbObject.get(ProjectKeys.accountIdentifier).toString());
    }

    if (dbObject.get(ProjectKeys.createdAt) != null) {
      columnValueMapping.put("created_at", dbObject.get(ProjectKeys.createdAt).toString());
    }

    return columnValueMapping;
  }

  @Override
  public List<String> getPrimaryKeys() {
    return asList("id");
  }
}

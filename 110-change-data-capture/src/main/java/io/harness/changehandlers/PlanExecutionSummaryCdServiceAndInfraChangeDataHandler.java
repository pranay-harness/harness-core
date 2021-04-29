package io.harness.changehandlers;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class PlanExecutionSummaryCdServiceAndInfraChangeDataHandler implements ChangeHandler {
  private static final int MAX_RETRY_COUNT = 5;
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent, String tableName, String[] fields) {
    log.info("In TimeScale Change Handler: {}, {}, {}", changeEvent, tableName, fields);
    Map<String, List<String>> columnValueMapping = getColumnValueMapping(changeEvent, fields);
    switch (changeEvent.getChangeType()) {
      case INSERT:
        if (columnValueMapping != null) {
          List<String> idList = columnValueMapping.get("id");
          Set<String> keyObject = columnValueMapping.keySet();
          Map<String, String> newColumnValueMapping = new HashMap<>();
          for (int i = 0; i < idList.size(); i++) {
            for (String entry : keyObject) {
              if (columnValueMapping.get(entry).size() > i) {
                newColumnValueMapping.put(entry, columnValueMapping.get(entry).get(i));
              }
            }
            dbOperation(insertSQL(tableName, newColumnValueMapping));
          }
        }
        break;
      case UPDATE:
        if (columnValueMapping != null) {
          List<String> idList = columnValueMapping.get("id");
          Set<String> keyObject = columnValueMapping.keySet();
          Map<String, String> newColumnValueMapping = new HashMap<>();
          for (int i = 0; i < idList.size(); i++) {
            for (String entry : keyObject) {
              if (columnValueMapping.get(entry).size() > i) {
                newColumnValueMapping.put(entry, columnValueMapping.get(entry).get(i));
              }
            }
            dbOperation(
                updateSQL(tableName, newColumnValueMapping, Collections.singletonMap("id", changeEvent.getUuid())));
            newColumnValueMapping.clear();
          }
        }
        break;
      default:
        log.info("Change Event Type not Handled: {}", changeEvent.getChangeType());
    }
    return true;
  }

  public Map<String, List<String>> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    if (changeEvent == null) {
      return null;
    }
    Map<String, List<String>> columnValueMapping = new HashMap<>();
    DBObject dbObject = changeEvent.getFullDocument();

    if (dbObject == null) {
      return columnValueMapping;
    }

    // if moduleInfo is null, not sure whether needs to be pushed to this table
    if (dbObject.get("moduleInfo") == null) {
      return null;
    }

    if (dbObject.get("layoutNodeMap") == null) {
      return null;
    }

    Set<Map.Entry<String, Object>> layoutNodeMap = ((BasicDBObject) dbObject.get("layoutNodeMap")).entrySet();

    Iterator<Map.Entry<String, Object>> iterator = layoutNodeMap.iterator();
    while (iterator.hasNext()) {
      if (columnValueMapping.containsKey("pipeline_execution_summary_cd_id")) {
        columnValueMapping.get("pipeline_execution_summary_cd_id").add(changeEvent.getUuid());
      } else {
        List<String> executionIdList = new ArrayList<>();
        executionIdList.add(changeEvent.getUuid());
        columnValueMapping.put("pipeline_execution_summary_cd_id", executionIdList);
      }

      Map.Entry<String, Object> iteratorObject = iterator.next();
      String id = iteratorObject.getKey();
      if (columnValueMapping.containsKey("id")) {
        columnValueMapping.get("id").add(id);
      } else {
        List<String> idList = new ArrayList<>();
        idList.add(id);
        columnValueMapping.put("id", idList);
      }
      DBObject moduleInfoObject = (DBObject) ((DBObject) iteratorObject.getValue()).get("moduleInfo");
      if (moduleInfoObject != null) {
        DBObject cdObject = (DBObject) moduleInfoObject.get("cd");
        if (cdObject != null) {
          // serviceInfo
          if (cdObject.get("serviceInfo") != null) {
            DBObject serviceInfoObject = (DBObject) cdObject.get("serviceInfo");
            // service_name
            String serviceName = serviceInfoObject.get("displayName").toString();
            if (columnValueMapping.containsKey("service_name")) {
              columnValueMapping.get("service_name").add(serviceName);
            } else {
              List<String> serviceList = new ArrayList<>();
              serviceList.add(serviceName);
              columnValueMapping.put("service_name", serviceList);
            }

            // service_id
            String serviceId = serviceInfoObject.get("identifier").toString();
            if (columnValueMapping.containsKey("service_id")) {
              columnValueMapping.get("service_id").add(serviceId);
            } else {
              List<String> serviceIdList = new ArrayList<>();
              serviceIdList.add(serviceId);
              columnValueMapping.put("service_id", serviceIdList);
            }

            // deploymentType

            String deploymentType = serviceInfoObject.get("deploymentType").toString();
            if (columnValueMapping.containsKey("deployment_type")) {
              columnValueMapping.get("deployment_type").add(deploymentType);
            } else {
              List<String> deploymentTypeList = new ArrayList<>();
              deploymentTypeList.add(deploymentType);
              columnValueMapping.put("deployment_type", deploymentTypeList);
            }

            // artifact - tag
            if (serviceInfoObject.get("artifacts") != null) {
              DBObject artifacts = (DBObject) serviceInfoObject.get("artifacts");
              if (artifacts.get("primary") != null) {
                DBObject primary = (DBObject) artifacts.get("primary");
                if (primary.get("tag") != null) {
                  String tag = primary.get("tag").toString();
                  if (columnValueMapping.containsKey("tag")) {
                    columnValueMapping.get("tag").add(tag);
                  } else {
                    List<String> tagList = new ArrayList<>();
                    tagList.add(tag);
                    columnValueMapping.put("tag", tagList);
                  }
                }
              }
            }
          }

          // env info
          if (cdObject.get("infraExecutionSummary") != null) {
            DBObject infraExecutionSummaryObject = (DBObject) cdObject.get("infraExecutionSummary");
            String envName = "";
            String envIdentifier = "";
            String envType = "";
            if (infraExecutionSummaryObject.get("name") != null) {
              envName = infraExecutionSummaryObject.get("name").toString();
            }

            if (infraExecutionSummaryObject.get("identifier") != null) {
              envIdentifier = infraExecutionSummaryObject.get("identifier").toString();
            }

            if (infraExecutionSummaryObject.get("type") != null) {
              envType = infraExecutionSummaryObject.get("type").toString();
            }

            if (columnValueMapping.containsKey("env_name")) {
              columnValueMapping.get("env_name").add(envName);
            } else {
              List<String> envNameList = new ArrayList<>();
              envNameList.add(envName);
              columnValueMapping.put("env_name", envNameList);
            }

            if (columnValueMapping.containsKey("env_id")) {
              columnValueMapping.get("env_id").add(envIdentifier);
            } else {
              List<String> envIdList = new ArrayList<>();
              envIdList.add(envIdentifier);
              columnValueMapping.put("env_id", envIdList);
            }

            if (columnValueMapping.containsKey("env_type")) {
              columnValueMapping.get("env_type").add(envType);
            } else {
              List<String> envIdList = new ArrayList<>();
              envIdList.add(envType);
              columnValueMapping.put("env_type", envIdList);
            }
          }

        } else {
          return null;
        }
      } else {
        return null;
      }
    }

    return columnValueMapping;
  }

  public boolean dbOperation(String query) {
    boolean successfulOperation = false;
    log.info("In dbOperation, Query: {}", query);
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulOperation && retryCount < MAX_RETRY_COUNT) {
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

  public static String insertSQL(String tableName, Map<String, String> columnValueMappingForInsert) {
    StringBuilder insertSQLBuilder = new StringBuilder();

    /**
     * Removing column that holds NULL value or Blank value...
     */
    if (!columnValueMappingForInsert.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForInsert.entrySet()) {
        if (entry.getValue() == null || entry.getValue().equals("")) {
          columnValueMappingForInsert.remove(entry.getKey());
        }
      }
    }

    /* Making the INSERT Query... */
    insertSQLBuilder.append(String.format("INSERT INTO %s (", tableName));

    if (!columnValueMappingForInsert.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForInsert.entrySet()) {
        insertSQLBuilder.append(String.format("%s,", entry.getKey()));
      }
    }

    insertSQLBuilder = new StringBuilder(insertSQLBuilder.subSequence(0, insertSQLBuilder.length() - 1));
    insertSQLBuilder.append(") VALUES(");

    if (!columnValueMappingForInsert.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForInsert.entrySet()) {
        insertSQLBuilder.append(String.format("'%s',", entry.getValue()));
      }
    }

    insertSQLBuilder = new StringBuilder(insertSQLBuilder.subSequence(0, insertSQLBuilder.length() - 1));
    insertSQLBuilder.append(')');

    // Returning the generated INSERT SQL Query as a String...
    return insertSQLBuilder.toString();
  }

  public static String updateSQL(String tableName, Map<String, String> columnValueMappingForSet,
      Map<String, String> columnValueMappingForCondition) {
    StringBuilder updateQueryBuilder = new StringBuilder(2048);

    /**
     * Adding insert statement
     */
    if (insertSQL(tableName, columnValueMappingForSet) != null) {
      updateQueryBuilder.append(insertSQL(tableName, columnValueMappingForSet));
    }
    // On conflict condition
    updateQueryBuilder.append(" ON CONFLICT (id) Do ");

    if (!columnValueMappingForSet.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForSet.entrySet()) {
        if (entry.getValue() == null || entry.getValue().equals("")) {
          columnValueMappingForSet.remove(entry.getKey());
        }
      }
    }

    /**
     * Removing column that holds NULL value or Blank value...
     */
    if (!columnValueMappingForCondition.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForCondition.entrySet()) {
        if (entry.getValue() == null || entry.getValue().equals("")) {
          columnValueMappingForCondition.remove(entry.getKey());
        }
      }
    }

    /* Making the UPDATE Query */
    updateQueryBuilder.append(String.format("UPDATE  SET "));

    if (!columnValueMappingForSet.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForSet.entrySet()) {
        updateQueryBuilder.append(String.format("%s=%s,", entry.getKey(), String.format("'%s'", entry.getValue())));
      }
    }

    updateQueryBuilder = new StringBuilder(updateQueryBuilder.subSequence(0, updateQueryBuilder.length() - 1));

    // Returning the generated UPDATE SQL Query as a String...
    return updateQueryBuilder.toString();
  }
}

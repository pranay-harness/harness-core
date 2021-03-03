package io.harness;

import io.harness.changestreamsframework.ChangeEvent;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.Application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;

@Slf4j
@Singleton
public class TimeScaleDBChangeHandler implements ChangeHandler {
  private static final int MAX_RETRY_COUNT = 5;
  @Inject private TimeScaleDBService timeScaleDBService;

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent, String tableName, String[] fields) {
    log.info("In TimeScale Change Handler: {}, {}, {}", changeEvent, tableName, fields);
    switch (changeEvent.getChangeType()) {
      case INSERT:
        create(tableName, getColumnValueMapping(changeEvent, fields));
        break;
      default:
        log.info("Change Event Type not Handled: {}", changeEvent.getChangeType());
    }
    return false;
  }

  public boolean create(String tableName, Map<String, String> columnValueMappingForInsert) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement =
                 dbConnection.prepareStatement(insertSQL(tableName, columnValueMappingForInsert))) {
          statement.execute();
          successfulInsert = true;
        } catch (SQLException e) {
          log.error("Failed to save ceCloudAccount data,retryCount=[{}], Exception: ", retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("TimeScale Down");
    }
    return successfulInsert;
  }

  private Map<String, String> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    Map<String, String> columnValueMapping = new HashMap<>();
    // TODO: make this Handling generic
    Application fullDocument = (Application) changeEvent.getFullDocument();
    columnValueMapping.put(Strings.toUpperCase("uuid"), changeEvent.getUuid());
    columnValueMapping.put(Strings.toUpperCase("appid"), fullDocument.getAppId());
    columnValueMapping.put(Strings.toUpperCase("name"), fullDocument.getName());
    return columnValueMapping;
  }

  // https://www.codeproject.com/articles/779373/generic-functions-to-generate-insert-update-delete Generic Function
  // Adapted from here
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
    insertSQLBuilder.append("INSERT INTO");
    insertSQLBuilder.append(" ").append(tableName);
    insertSQLBuilder.append("(");

    if (!columnValueMappingForInsert.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForInsert.entrySet()) {
        insertSQLBuilder.append(entry.getKey());
        insertSQLBuilder.append(",");
      }
    }

    insertSQLBuilder = new StringBuilder(insertSQLBuilder.subSequence(0, insertSQLBuilder.length() - 1));
    insertSQLBuilder.append(")");
    insertSQLBuilder.append(" VALUES");
    insertSQLBuilder.append("(");

    if (!columnValueMappingForInsert.isEmpty()) {
      for (Map.Entry<String, String> entry : columnValueMappingForInsert.entrySet()) {
        insertSQLBuilder.append(String.format("'%s'", entry.getValue()));
        insertSQLBuilder.append(",");
      }
    }

    insertSQLBuilder = new StringBuilder(insertSQLBuilder.subSequence(0, insertSQLBuilder.length() - 1));
    insertSQLBuilder.append(")");

    // Returning the generated INSERT SQL Query as a String...
    return insertSQLBuilder.toString();
  }
}

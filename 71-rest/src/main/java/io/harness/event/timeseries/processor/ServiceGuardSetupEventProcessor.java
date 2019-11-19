package io.harness.event.timeseries.processor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

@Singleton
@Slf4j
public class ServiceGuardSetupEventProcessor {
  private static final int MAX_RETRY_COUNT = 5;

  /**
   ACCOUNT_ID TEXT NOT NULL,
   VERIFICATION_PROVIDER_TYPE TEXT NOT NULL,
   NUM_OF_CONFIGS INTEGER NOT NULL,
   NUM_OF_ALERTS INTEGER NOT NULL,
   CREATION_TIME TIMESTAMP NOT NULL,
   */
  private String insertSQLStatement = "INSERT INTO CV_CONFIGURATIONS (ACCOUNT_ID, VERIFICATION_PROVIDER_TYPE, "
      + "NUM_OF_CONFIGS, NUM_OF_ALERTS, CREATION_TIME) VALUES (?,?,?,?,?)";

  @Inject private TimeScaleDBService timeScaleDBService;

  public void processEvent(Map<String, String> properties) {
    if (timeScaleDBService.isValid()) {
      String accountId = properties.get("accountId");
      String verificationProviderType = properties.get("verificationProviderType");
      String numOfConfigsString = properties.get("configs");
      String numOfAlertsString = properties.get("alerts");
      Preconditions.checkNotNull(accountId);
      Preconditions.checkNotNull(verificationProviderType);
      Preconditions.checkNotNull(numOfConfigsString);
      Preconditions.checkNotNull(numOfAlertsString);

      int retryCount = 0;
      long startTime = System.currentTimeMillis();
      boolean successfulInsert = false;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection connection = timeScaleDBService.getDBConnection();
             PreparedStatement insertPreparedStatement = connection.prepareStatement(insertSQLStatement)) {
          insertPreparedStatement.setString(1, accountId);
          insertPreparedStatement.setString(2, verificationProviderType);
          insertPreparedStatement.setInt(3, Integer.valueOf(numOfConfigsString));
          insertPreparedStatement.setInt(4, Integer.valueOf(numOfAlertsString));
          insertPreparedStatement.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
          insertPreparedStatement.execute();
          successfulInsert = true;
        } catch (SQLException e) {
          retryCount++;
          if (retryCount >= MAX_RETRY_COUNT) {
            logger.error("Failed to save deployment data,[{}]", properties, e);
          } else {
            logger.info("Failed to save deployment data,[{}],retryCount=[{}]", properties, retryCount);
          }
        } finally {
          logger.info("Total time=[{}],retryCount=[{}]", System.currentTimeMillis() - startTime, retryCount);
        }
      }
    } else {
      logger.trace("Not processing data:[{}]", properties);
    }
  }
}
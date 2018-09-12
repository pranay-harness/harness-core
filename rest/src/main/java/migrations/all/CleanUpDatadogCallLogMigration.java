package migrations.all;

import static software.wings.dl.MongoHelper.setUnset;

import com.google.inject.Inject;

import io.harness.time.Timestamp;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleanUpDatadogCallLogMigration implements Migration {
  private static final String API_KEY = "api_key";
  private static final Logger logger = LoggerFactory.getLogger(CleanUpDatadogCallLogMigration.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      List<StateExecutionInstance> stateExecutionInstances =
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .filter("stateType", "DATA_DOG")
              .field("createdAt")
              .greaterThanOrEq(Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(30))
              .asList();

      logger.info("Fixing the 3P calllogs for {} datadog executions", stateExecutionInstances.size());
      for (StateExecutionInstance instance : stateExecutionInstances) {
        List<ThirdPartyApiCallLog> callLogList = wingsPersistence.createQuery(ThirdPartyApiCallLog.class)
                                                     .field("stateExecutionId")
                                                     .equal(instance.getUuid())
                                                     .asList();

        for (ThirdPartyApiCallLog log : callLogList) {
          if (log.getTitle().contains(API_KEY)) {
            log.setTitle(replaceApiAndAppKey(log.getTitle()));
          }
          for (ThirdPartyApiCallField callLogFields : log.getRequest()) {
            if (callLogFields.getValue().contains(API_KEY)) {
              callLogFields.setValue(replaceApiAndAppKey(callLogFields.getValue()));
            }
          }
          UpdateOperations<ThirdPartyApiCallLog> op =
              wingsPersistence.createUpdateOperations(ThirdPartyApiCallLog.class);
          setUnset(op, "request", log.getRequest());
          setUnset(op, "title", log.getTitle());
          wingsPersistence.update(wingsPersistence.createQuery(ThirdPartyApiCallLog.class)
                                      .filter("stateExecutionId", log.getStateExecutionId()),
              op);
        }
      }

    } catch (RuntimeException ex) {
      logger.error("Exception while migrating thirdpartycallLogs for Datadog", ex);
    }
  }

  private String replaceApiAndAppKey(String badUrl) {
    String returnValue = badUrl;
    final String datadog_api_mask = "api_key=([^&]*)&application_key=([^&]*)&";
    Pattern batchPattern = Pattern.compile(datadog_api_mask);
    Matcher matcher = batchPattern.matcher(badUrl);
    while (matcher.find()) {
      final String apiKey = matcher.group(1);
      final String appKey = matcher.group(2);
      returnValue = badUrl.replace(apiKey, "<apiKeyPlaceholder>");
      returnValue = returnValue.replace(appKey, "<appKeyPlaceholder>");
    }
    return returnValue;
  }
}

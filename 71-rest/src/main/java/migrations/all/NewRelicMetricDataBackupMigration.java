package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.persistence.HQuery.excludeCount;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.DataStorageMode;
import software.wings.app.MainConfiguration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.DataStoreService;

public class NewRelicMetricDataBackupMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(NewRelicMetricDataBackupMigration.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void migrate() {
    if (mainConfiguration.getExecutionLogsStorageMode() != DataStorageMode.GOOGLE_CLOUD_DATA_STORE) {
      logger.info("google data store not enabled, returning....");
      return;
    }
    PageResponse<NewRelicMetricDataRecord> response = dataStoreService.list(
        NewRelicMetricDataRecord.class, aPageRequest().withLimit("1").addOrder("timeStamp", OrderType.DESC).build());

    long maxTime = 0;
    if (!response.isEmpty()) {
      maxTime = response.get(0).getTimeStamp();
    }

    logger.info("Move NewRelicMetricDataRecord to google data store from time {}", maxTime);
    PageRequest<NewRelicMetricDataRecord> pageRequest = aPageRequest()
                                                            .addFilter("timeStamp", Operator.GE, maxTime)
                                                            .withLimit("300")
                                                            .withOffset("0")
                                                            .addOrder("createdAt", OrderType.ASC)
                                                            .build();

    int previousOffSet = 0;
    response = wingsPersistence.query(NewRelicMetricDataRecord.class, pageRequest, excludeCount);
    while (!response.isEmpty()) {
      dataStoreService.save(NewRelicMetricDataRecord.class, response.getResponse());
      previousOffSet += response.size();
      logger.info("moved records:  " + previousOffSet);
      pageRequest.setOffset(String.valueOf(previousOffSet));
      response = wingsPersistence.query(NewRelicMetricDataRecord.class, pageRequest, excludeCount);
      sleep(ofMillis(2000));
    }
  }
}
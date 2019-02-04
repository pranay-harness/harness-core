package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.persistence.CreatedAtAccess.CREATED_AT_KEY;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import migrations.Migration;
import org.mongodb.morphia.annotations.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.DataStorageMode;
import software.wings.app.MainConfiguration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.DataStoreService;

import java.util.concurrent.TimeUnit;

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
    long minTime = Long.MAX_VALUE;
    EntityQuery query = Query.newEntityQueryBuilder()
                            .setKind(NewRelicMetricDataRecord.class.getAnnotation(Entity.class).value())
                            .setLimit(1)
                            .setOrderBy(OrderBy.asc("timeStamp"))
                            .build();
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    QueryResults<com.google.cloud.datastore.Entity> results = datastore.run(query);

    if (results.hasNext()) {
      try {
        NewRelicMetricDataRecord record =
            (NewRelicMetricDataRecord) NewRelicMetricDataRecord.class.newInstance().readFromCloudStorageEntity(
                results.next());
        minTime = record.getTimeStamp();
      } catch (Exception e) {
        logger.info("error reading record ", e);
        return;
      }
    }

    logger.info("Move NewRelicMetricDataRecord to google data store from time {}", minTime);
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .addFilter(CREATED_AT_KEY, Operator.LT_EQ, minTime + TimeUnit.DAYS.toMillis(1))
            .withLimit("1000")
            .withOffset("0")
            .addOrder(CREATED_AT_KEY, OrderType.DESC)
            .build();

    int previousOffSet = 0;
    PageResponse<NewRelicMetricDataRecord> response =
        wingsPersistence.query(NewRelicMetricDataRecord.class, pageRequest, excludeAuthority);
    while (!response.isEmpty()) {
      logger.info("going to save {} records to google", response.size());
      dataStoreService.save(NewRelicMetricDataRecord.class, response.getResponse());
      previousOffSet += response.size();
      logger.info("moved records:  {}", previousOffSet);
      pageRequest.setOffset(String.valueOf(previousOffSet));
      logger.info("get next set of records to move");
      response = wingsPersistence.query(NewRelicMetricDataRecord.class, pageRequest, excludeAuthority);
      logger.info("got {} records to move", response.size());
      sleep(ofMillis(2000));
    }

    logger.info("migration done...");
  }
}
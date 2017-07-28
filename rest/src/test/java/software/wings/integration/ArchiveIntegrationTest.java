package software.wings.integration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/13/17.
 */
public class ArchiveIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(AppdynamicsMetricDataRecord.class));
  }

  @Test
  public void testArchival() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
                                    InvocationTargetException, InterruptedException {
    final Random r = new Random();
    final int numOfRecords = r.nextInt(50);

    final String appID = UUID.randomUUID().toString();
    final String accountId = UUID.randomUUID().toString();

    for (int i = 0; i < numOfRecords; i++) {
      final AppdynamicsMetricDataRecord dataRecord =
          AppdynamicsMetricDataRecord.Builder.anAppdynamicsMetricsDataRecord()
              .withAccountId(accountId)
              .withApplicationId(appID)
              .withAppdAppId(r.nextInt())
              .withBtId(r.nextLong())
              .withBtName(UUID.randomUUID().toString())
              .withCount(r.nextInt())
              .withCurrent(r.nextDouble())
              .withMax(r.nextLong())
              .withMin(r.nextLong())
              .withMetricId(r.nextInt())
              .withMetricName(UUID.randomUUID().toString())
              .build();
      dataRecord.setLastUpdatedAt(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
      dataRecord.setCreatedAt(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
      wingsPersistence.save(dataRecord);
    }

    Query<AppdynamicsMetricDataRecord> splunkLogDataRecordQuery =
        wingsPersistence.createQuery(AppdynamicsMetricDataRecord.class);

    Assert.assertEquals(numOfRecords, splunkLogDataRecordQuery.asList().size());

    Thread.sleep(TimeUnit.SECONDS.toMillis(90));

    splunkLogDataRecordQuery = wingsPersistence.createQuery(AppdynamicsMetricDataRecord.class);
    Assert.assertEquals(0, splunkLogDataRecordQuery.asList().size());
  }
}

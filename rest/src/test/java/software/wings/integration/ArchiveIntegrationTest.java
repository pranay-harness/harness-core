package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import org.junit.Assert;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.Archive;
import software.wings.service.impl.splunk.SplunkLogDataRecord;
import software.wings.service.impl.splunk.SplunkLogRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 7/13/17.
 */
public class ArchiveIntegrationTest extends BaseIntegrationTest {
  public void setUp() throws Exception {
    loginAdminUser();
  }

  @Test
  public void testArchival() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
                                    InvocationTargetException, InterruptedException {
    final Random r = new Random();
    final int numOfRecords = r.nextInt(50);

    final String stateExecutionId = UUID.randomUUID().toString();
    final String query = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();
    final int logCollectionMinute = 0;
    final boolean processed = false;
    final String host = UUID.randomUUID().toString();

    for (int i = 0; i < numOfRecords; i++) {
      final String clusterLabel = UUID.randomUUID().toString();
      final long timeStamp = System.currentTimeMillis();
      final int count = r.nextInt();
      final String logMessage = UUID.randomUUID().toString();
      final String logMD5Hash = UUID.randomUUID().toString();

      final SplunkLogDataRecord splunkLogDataRecord = new SplunkLogDataRecord(applicationId, stateExecutionId, query,
          clusterLabel, host, timeStamp, count, logMessage, logMD5Hash, processed, logCollectionMinute);
      splunkLogDataRecord.setLastUpdatedAt(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
      splunkLogDataRecord.setCreatedAt(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
      wingsPersistence.save(splunkLogDataRecord);
    }

    WebTarget target = client.target(API_BASE + "/splunk/get-logs?accountId=" + accountId);
    final SplunkLogRequest logRequest = new SplunkLogRequest(
        query, applicationId, stateExecutionId, Collections.singletonList(host), logCollectionMinute);
    RestResponse<List<SplunkLogDataRecord>> restResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<SplunkLogDataRecord>>>() {});

    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertEquals(numOfRecords, restResponse.getResource().size());

    Thread.sleep(TimeUnit.SECONDS.toMillis(90));

    restResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<SplunkLogDataRecord>>>() {});
    Assert.assertEquals(0, restResponse.getResource().size());
  }
}

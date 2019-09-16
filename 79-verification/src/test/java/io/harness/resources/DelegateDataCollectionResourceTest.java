package io.harness.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.category.element.IntegrationTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.LogLevel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class DelegateDataCollectionResourceTest extends VerificationBaseIntegrationTest {
  @Inject CVActivityLogService cvActivityLogService;
  @Before
  public void setUp() {
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testPostCVActivityLog() {
    WebTarget target =
        client.target(VERIFICATION_API_BASE + "/delegate-data-collection/save-cv-activity-logs?accountId=" + accountId);
    List<CVActivityLog> logs = IntStream.range(0, 10).mapToObj(i -> getActivityLog()).collect(Collectors.toList());
    Response response = getDelegateRequestBuilderWithAuthHeader(target).post(entity(logs, APPLICATION_JSON));
    assertEquals(200, response.getStatus());
    logs.forEach(log -> {
      CVActivityLog cvActivityLog = cvActivityLogService.findByStateExecutionId(log.getStateExecutionId()).get(0);
      assertEquals(log.getStateExecutionId(), cvActivityLog.getStateExecutionId());
      assertEquals(log.getLog(), cvActivityLog.getLog());
      assertEquals(log.getLogLevel(), cvActivityLog.getLogLevel());
    });
  }

  private CVActivityLog getActivityLog() {
    return CVActivityLog.builder()
        .stateExecutionId(generateUuid())
        .logLevel(LogLevel.INFO)
        .log("test log: " + generateUuid())
        .build();
  }
}
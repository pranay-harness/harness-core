package software.wings.integration.verification;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVActivityLogService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Slf4j
public class CVActivityLogIntegrationTest extends BaseIntegrationTest {
  @Inject CVActivityLogService cvActivityLogService;
  @Mock FeatureFlagService featureFlagService;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    // TODO: need to remove this when feature flag is removed. Only used for writing logs.
    FieldUtils.writeField(cvActivityLogService, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isGlobalEnabled(any())).thenReturn(true);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(IntegrationTests.class)
  public void testGetActivityLogsByStateExecutionId() {
    long now = System.currentTimeMillis();
    String stateExecutionId = generateUUID();
    String logLine = "timestamp %t test-log: " + generateUUID();
    cvActivityLogService.getLoggerByStateExecutionId(stateExecutionId).info(logLine, now);
    WebTarget getTarget =
        client.target(API_BASE + "/cv-activity-logs?accountId=" + accountId + "&stateExecutionId=" + stateExecutionId);

    RestResponse<List<CVActivityLogApiResponse>> response = getRequestBuilderWithAuthHeader(getTarget).get(
        new GenericType<RestResponse<List<CVActivityLogApiResponse>>>() {});

    assertThat(response.getResource().isEmpty()).isFalse();
    assertThat(1).isEqualTo(response.getResource().size());
    CVActivityLogApiResponse cvActivityLogApiResponse = response.getResource().get(0);
    assertThat((long) cvActivityLogApiResponse.getDataCollectionMinute()).isEqualTo(0);
    assertThat(cvActivityLogApiResponse.getLog()).isEqualTo(logLine);
    assertThat(cvActivityLogApiResponse.getStateExecutionId()).isEqualTo(stateExecutionId);
    assertThat(cvActivityLogApiResponse.getLogLevel()).isEqualTo("INFO");
    assertThat(cvActivityLogApiResponse.getTimestamp() >= now).isTrue();
    assertThat((long) cvActivityLogApiResponse.getTimestampParams().get(0)).isEqualTo(now);
    assertThat(cvActivityLogApiResponse.getAnsiLog()).isEqualTo(logLine);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(IntegrationTests.class)
  public void testGetCVActivityLog() {
    long now = System.currentTimeMillis();

    String cvConfigId = generateUUID();
    String logLine = "test log";
    long before = now - TimeUnit.MINUTES.toMillis(1), after = now + TimeUnit.MINUTES.toMillis(5);
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now)).error(logLine);
    WebTarget getTarget = client.target(API_BASE + "/cv-activity-logs?accountId=" + accountId
        + "&cvConfigId=" + cvConfigId + "&startTime=" + before + "&endTime=" + after);

    RestResponse<List<CVActivityLogApiResponse>> response = getRequestBuilderWithAuthHeader(getTarget).get(
        new GenericType<RestResponse<List<CVActivityLogApiResponse>>>() {});

    assertThat(response.getResource().isEmpty()).isFalse();
    assertThat(1).isEqualTo(response.getResource().size());
    CVActivityLogApiResponse cvActivityLogApiResponse = response.getResource().get(0);
    assertThat((long) cvActivityLogApiResponse.getDataCollectionMinute())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes(now));
    assertThat(logLine).isEqualTo(logLine, cvActivityLogApiResponse.getLog());
    assertThat(cvActivityLogApiResponse.getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(cvActivityLogApiResponse.getLogLevel()).isEqualTo("ERROR");
    assertThat(cvActivityLogApiResponse.getTimestamp() >= now).isTrue();
    assertThat(cvActivityLogApiResponse.getAnsiLog()).isEqualTo("\u001B[31mtest log\u001B[0m");
  }

  @Data
  @Builder
  private static class CVActivityLogApiResponse {
    private String log;
    private String cvConfigId;
    private String stateExecutionId;
    private Long dataCollectionMinute;
    private String logLevel;
    private long timestamp;
    private List<Long> timestampParams;
    private String ansiLog;
  }
}

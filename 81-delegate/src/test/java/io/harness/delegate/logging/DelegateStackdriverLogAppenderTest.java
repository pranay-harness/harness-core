package io.harness.delegate.logging;

import static ch.qos.logback.classic.Level.INFO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.RemoteStackdriverLogAppender.logLevelToSeverity;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.AccessTokenBean;
import io.harness.managerclient.ManagerClient;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DelegateStackdriverLogAppenderTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ManagerClient managerClient;
  @Mock private Call<RestResponse<AccessTokenBean>> callAccessTokenBean;

  private DelegateStackdriverLogAppender appender = new DelegateStackdriverLogAppender();

  private final TimeLimiter timeLimiter = new FakeTimeLimiter();
  private final Logger logger = new LoggerContext().getLogger(DelegateStackdriverLogAppenderTest.class);
  private final RestResponse<AccessTokenBean> accessTokenBeanRestResponse =
      new RestResponse<>(AccessTokenBean.builder()
                             .projectId("project-id")
                             .tokenValue("token-value")
                             .expirationTimeMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1))
                             .build());
  private final okhttp3.Response rawResponse = new Builder()
                                                   .protocol(Protocol.HTTP_2)
                                                   .code(200)
                                                   .request(new Request.Builder().url("http://test.harness.io").build())
                                                   .build();

  @Before
  public void setUp() throws Exception {
    when(managerClient.getLoggingToken(anyString())).thenReturn(callAccessTokenBean);
    when(callAccessTokenBean.execute()).thenReturn(Response.success(accessTokenBeanRestResponse, rawResponse));
    DelegateStackdriverLogAppender.setManagerClient(managerClient);
    DelegateStackdriverLogAppender.setTimeLimiter(timeLimiter);
    appender.start();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldAppend() {
    String message = "my log message";
    log(INFO, message);
    waitForMessage(INFO, message);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldSubmit() {
    String message = "my log message";
    log(INFO, message);
    waitForMessage(INFO, message);
    BlockingQueue<LogEntry> logQueue = appender.getLogQueue();
    await().atMost(15L, TimeUnit.SECONDS).until(logQueue::isEmpty);
  }

  private void log(Level level, String message) {
    appender.doAppend(new LoggingEvent("a.b.class", logger, level, message, null, null));
  }

  private void waitForMessage(Level level, String message) {
    BlockingQueue<LogEntry> logQueue = appender.getLogQueue();
    await().atMost(5L, TimeUnit.SECONDS).until(() -> {
      if (isEmpty(logQueue)) {
        return false;
      }

      for (LogEntry entry : logQueue) {
        if (entry == null) {
          continue;
        }
        if (entry.getSeverity() != logLevelToSeverity(level)) {
          continue;
        }
        Map<String, ?> jsonMap = ((JsonPayload) entry.getPayload()).getDataAsMap();
        if (jsonMap.get("message").equals(message)) {
          assertThat(jsonMap.get("logger")).isEqualTo("io.harness.delegate.logging.DelegateStackdriverLogAppenderTest");
          return true;
        }
      }

      return false;
    });
  }
}

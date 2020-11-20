package io.harness.delegate.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Log.Builder.aLog;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.managerclient.ManagerClient;
import io.harness.managerclient.VerificationServiceClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

public class DelegateLogServiceImplTest extends CategoryTest {
  private static final int ACTIVITY_LOGS_BATCH_SIZE = DelegateLogServiceImpl.ACTIVITY_LOGS_BATCH_SIZE;
  @Mock private ManagerClient managerClient;
  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private VerificationServiceClient verificationServiceClient;
  @Mock private ExecutorService executorService;
  @Mock private KryoSerializer kryoSerializer;
  private DelegateLogServiceImpl delegateLogService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    delegateLogService = new DelegateLogServiceImpl(
        managerClient, delegateAgentManagerClient, executorService, verificationServiceClient, kryoSerializer);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void trimAndIndicate() {
    assertThat(delegateLogService.trimAndIndicate(asList(aLog().logLine(null).build()))).isEqualTo("null");
    assertThat(delegateLogService.trimAndIndicate(asList(aLog().logLine("").build()))).isEqualTo("");
    assertThat(delegateLogService.trimAndIndicate(asList(aLog().logLine("hello").build()))).isEqualTo("hello");
    assertThat(delegateLogService.trimAndIndicate(asList(aLog().logLine(sampleString(5)).build())))
        .isEqualTo(sampleString(5));
    assertThat(delegateLogService.trimAndIndicate(
                   asList(aLog().logLine(sampleString(100)).build(), aLog().logLine(sampleString(5)).build())))
        .isEqualTo(sampleString(100) + "\n" + sampleString(5));
    assertThat(
        delegateLogService.trimAndIndicate(asList(aLog().logLine(sampleString(ACTIVITY_LOGS_BATCH_SIZE)).build())))
        .isEqualTo(sampleString(ACTIVITY_LOGS_BATCH_SIZE));
    assertThat(delegateLogService.trimAndIndicate(
                   asList(aLog().logLine(sampleString(5 * ACTIVITY_LOGS_BATCH_SIZE + 3)).build())))
        .isEqualTo(sampleString(ACTIVITY_LOGS_BATCH_SIZE) + "\nThe above log messages are truncated to 10KB\n");
    assertThat(delegateLogService.trimAndIndicate(asList(aLog().logLine(sampleString(5)).build(),
                   aLog().logLine(sampleString(5 * ACTIVITY_LOGS_BATCH_SIZE + 3)).build())))
        .isEqualTo(sampleString(5) + "\n" + sampleString(ACTIVITY_LOGS_BATCH_SIZE - 6)
            + "\nThe above log messages are truncated to 10KB\n");
    assertThat(delegateLogService.trimAndIndicate(asList(
                   aLog().logLine("").build(), aLog().logLine(sampleString(5 * ACTIVITY_LOGS_BATCH_SIZE + 3)).build())))
        .isEqualTo(
            "\n" + sampleString(ACTIVITY_LOGS_BATCH_SIZE - 1) + "\nThe above log messages are truncated to 10KB\n");
  }

  private String sampleString(int len) {
    StringBuilder builder = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      builder.append("a");
    }
    return builder.toString();
  }
}

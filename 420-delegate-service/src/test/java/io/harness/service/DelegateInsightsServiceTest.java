package io.harness.service;

import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateInsightsServiceImpl;

import software.wings.beans.DelegateTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights.DelegateTaskUsageInsightsKeys;
import software.wings.beans.DelegateTaskUsageInsightsEventType;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateInsightsServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";
  private static final String TEST_TASK_ID = "testTaskId";
  private static final String TEST_DELEGATE_ID = "testDelegateId";
  private static final String TEST_DELEGATE_GROUP_ID = "testDelegateGroupId";

  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks @Inject private DelegateInsightsServiceImpl delegateInsightsService;
  @Inject private HPersistence persistence;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskAssignedFeatureFlagDisabled() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(false);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNull();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskAssignedValidValues() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsCreateEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsCreateEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.STARTED);
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsUnknownEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.UNKNOWN);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedValidValuesSucceeded() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.SUCCEEDED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsSucceededEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.SUCCEEDED);

    assertThat(delegateTaskUsageInsightsSucceededEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsSucceededEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsSucceededEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.SUCCEEDED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedValidValuesFailed() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID)).thenReturn(true);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.FAILED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsFailedEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.FAILED);

    assertThat(delegateTaskUsageInsightsFailedEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsFailedEvent.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getTaskId()).isEqualTo(TEST_TASK_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getDelegateId()).isEqualTo(TEST_DELEGATE_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getDelegateGroupId()).isEqualTo(TEST_DELEGATE_GROUP_ID);
    assertThat(delegateTaskUsageInsightsFailedEvent.getEventType())
        .isEqualTo(DelegateTaskUsageInsightsEventType.FAILED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testOnTaskCompletedFeatureFlagDisabled() {
    when(featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, TEST_ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);

    delegateInsightsService.onTaskAssigned(TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, TEST_DELEGATE_GROUP_ID);

    DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.STARTED);
    DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.UNKNOWN);

    assertThat(delegateTaskUsageInsightsCreateEvent).isNotNull();
    assertThat(delegateTaskUsageInsightsUnknownEvent).isNotNull();

    delegateInsightsService.onTaskCompleted(
        TEST_ACCOUNT_ID, TEST_TASK_ID, TEST_DELEGATE_ID, DelegateTaskUsageInsightsEventType.FAILED);

    DelegateTaskUsageInsights delegateTaskUsageInsightsFailedEvent =
        getDefaultDelegateTaskUsageInsightsFromDB(DelegateTaskUsageInsightsEventType.FAILED);

    assertThat(delegateTaskUsageInsightsFailedEvent).isNull();
  }

  private DelegateTaskUsageInsights getDefaultDelegateTaskUsageInsightsFromDB(
      DelegateTaskUsageInsightsEventType eventType) {
    return persistence.createQuery(DelegateTaskUsageInsights.class)
        .filter(DelegateTaskUsageInsightsKeys.accountId, TEST_ACCOUNT_ID)
        .filter(DelegateTaskUsageInsightsKeys.taskId, TEST_TASK_ID)
        .filter(DelegateTaskUsageInsightsKeys.delegateId, TEST_DELEGATE_ID)
        .filter(DelegateTaskUsageInsightsKeys.delegateGroupId, TEST_DELEGATE_GROUP_ID)
        .filter(DelegateTaskUsageInsightsKeys.eventType, eventType)
        .get();
  }
}

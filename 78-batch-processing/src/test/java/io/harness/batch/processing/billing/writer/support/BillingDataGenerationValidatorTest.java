package io.harness.batch.processing.billing.writer.support;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BillingDataGenerationValidatorTest extends CategoryTest {
  private static final String ACCOUNT_ID = "fcf53242-4a9d-4b8c-8497-5ba7360569d9";
  private static final String CLUSTER_ID = "fe847242-1de4-7ab5-8497-5ba7360569d9";
  private static final Instant START_TIME = Instant.now();
  private BillingDataGenerationValidator billingDataGenerationValidator;
  private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  @Before
  public void setUp() {
    lastReceivedPublishedMessageDao = mock(LastReceivedPublishedMessageDao.class);
    billingDataGenerationValidator = new BillingDataGenerationValidator(lastReceivedPublishedMessageDao);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenClusterEventIsNotAvailable() {
    when(lastReceivedPublishedMessageDao.get(ACCOUNT_ID, CLUSTER_ID)).thenReturn(null);
    boolean generateBillingData =
        billingDataGenerationValidator.shouldGenerateBillingData(ACCOUNT_ID, CLUSTER_ID, START_TIME);
    assertThat(generateBillingData).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenClusterEventIsOld() {
    when(lastReceivedPublishedMessageDao.get(ACCOUNT_ID, CLUSTER_ID))
        .thenReturn(lastReceivedPublishedMessage(START_TIME.minus(1, ChronoUnit.DAYS).toEpochMilli()));
    boolean generateBillingData =
        billingDataGenerationValidator.shouldGenerateBillingData(ACCOUNT_ID, CLUSTER_ID, START_TIME);
    assertThat(generateBillingData).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnTrueWhenClusterEventIsRecent() {
    when(lastReceivedPublishedMessageDao.get(ACCOUNT_ID, CLUSTER_ID))
        .thenReturn(lastReceivedPublishedMessage(START_TIME.plus(1, ChronoUnit.DAYS).toEpochMilli()));
    boolean generateBillingData =
        billingDataGenerationValidator.shouldGenerateBillingData(ACCOUNT_ID, CLUSTER_ID, START_TIME);
    assertThat(generateBillingData).isTrue();

    boolean generateBillingDataCached =
        billingDataGenerationValidator.shouldGenerateBillingData(ACCOUNT_ID, CLUSTER_ID, START_TIME);
    assertThat(generateBillingDataCached).isTrue();
  }

  private LastReceivedPublishedMessage lastReceivedPublishedMessage(long lastReceivedAt) {
    return LastReceivedPublishedMessage.builder()
        .accountId(ACCOUNT_ID)
        .identifier(CLUSTER_ID)
        .lastReceivedAt(lastReceivedAt)
        .build();
  }
}

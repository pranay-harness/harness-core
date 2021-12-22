package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.ARPIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.UpgradeCheckResult;
import io.harness.delegate.service.intfc.DelegateUpgraderService;
import io.harness.delegate.utils.DelegateRingConstants;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.Account;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderServiceTest extends CategoryTest {
  private static final String TEST_ACCOUNT_ID1 = "accountId1";
  private static final String TEST_ACCOUNT_ID2 = "accountId2";
  private static final String LATEST_DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  private static final String LATEST_UPGRADER_IMAGE_TAG = "harness/upgrader:latest";
  private static final String DELEGATE_IMAGE_TAG_1 = "harness/delegate:1";
  private static final String UPGRADER_IMAGE_TAG_1 = "harness/upgrader:1";

  @Inject private DelegateUpgraderService upgraderService;
  @Inject private HPersistence persistence;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ARPIT)
  @Category(UnitTests.class)
  public void shouldGetDelegateImageTag() {
    setupAccount(TEST_ACCOUNT_ID1, DelegateRingConstants.RING_NAME_1);
    setupAccount(TEST_ACCOUNT_ID2, DelegateRingConstants.RING_NAME_2);
    setupDelegateRing();
    UpgradeCheckResult upgradeCheckResult1 =
        upgraderService.getDelegateImageTag(TEST_ACCOUNT_ID1, DELEGATE_IMAGE_TAG_1);
    UpgradeCheckResult upgradeCheckResult2 =
        upgraderService.getDelegateImageTag(TEST_ACCOUNT_ID2, DELEGATE_IMAGE_TAG_1);

    assertThat(upgradeCheckResult1.isShouldUpgrade()).isTrue();
    assertThat(upgradeCheckResult1.getImageTag()).isEqualTo(LATEST_DELEGATE_IMAGE_TAG);
    assertThat(upgradeCheckResult2.isShouldUpgrade()).isFalse();
    assertThat(upgradeCheckResult2.getImageTag()).isEqualTo(DELEGATE_IMAGE_TAG_1);
  }

  @Test
  @Owner(developers = ARPIT)
  @Category(UnitTests.class)
  public void shouldGetUpgraderImageTag() {
    setupAccount(TEST_ACCOUNT_ID1, DelegateRingConstants.RING_NAME_1);
    setupAccount(TEST_ACCOUNT_ID2, DelegateRingConstants.RING_NAME_2);
    setupDelegateRing();

    UpgradeCheckResult upgradeCheckResult1 =
        upgraderService.getUpgraderImageTag(TEST_ACCOUNT_ID1, UPGRADER_IMAGE_TAG_1);
    UpgradeCheckResult upgradeCheckResult2 =
        upgraderService.getUpgraderImageTag(TEST_ACCOUNT_ID2, UPGRADER_IMAGE_TAG_1);

    assertThat(upgradeCheckResult1.isShouldUpgrade()).isFalse();
    assertThat(upgradeCheckResult1.getImageTag()).isEqualTo(UPGRADER_IMAGE_TAG_1);
    assertThat(upgradeCheckResult2.isShouldUpgrade()).isTrue();
    assertThat(upgradeCheckResult2.getImageTag()).isEqualTo(LATEST_UPGRADER_IMAGE_TAG);
  }

  private void setupAccount(String accountId, String ringName) {
    Account account = Account.Builder.anAccount().withAccountKey(accountId).withRingName(ringName).build();
    persistence.save(account);
  }

  private void setupDelegateRing() {
    persistence.save(
        new DelegateRing(DelegateRingConstants.RING_NAME_1, LATEST_DELEGATE_IMAGE_TAG, UPGRADER_IMAGE_TAG_1));
    persistence.save(
        new DelegateRing(DelegateRingConstants.RING_NAME_2, DELEGATE_IMAGE_TAG_1, LATEST_UPGRADER_IMAGE_TAG));
  }
}
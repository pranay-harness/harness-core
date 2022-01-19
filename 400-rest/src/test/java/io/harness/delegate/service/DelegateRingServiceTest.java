/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.ARPIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.service.impl.DelegateRingServiceImpl;
import io.harness.delegate.utils.DelegateRingConstants;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.DEL)
@RunWith(MockitoJUnitRunner.class)
public class DelegateRingServiceTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "accountId";
  private static final String LATEST_DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  private static final String LATEST_UPGRADER_IMAGE_TAG = "harness/upgrader:latest";

  @InjectMocks @Inject private DelegateRingServiceImpl delegateRingService;
  @Inject private HPersistence persistence;

  @Before
  public void setUp() {
    initMocks(this);
    setupAccount(TEST_ACCOUNT_ID);
    setupDelegateRing();
  }

  @Test
  @Owner(developers = ARPIT)
  @Category(UnitTests.class)
  public void shouldGetDelegateImageTag() {
    String delegateImageTag = delegateRingService.getDelegateImageTag(TEST_ACCOUNT_ID);
    assertThat(delegateImageTag).isEqualTo(LATEST_DELEGATE_IMAGE_TAG);
  }

  @Test
  @Owner(developers = ARPIT)
  @Category(UnitTests.class)
  public void shouldGetUpgraderImageTag() {
    String upgraderImageTag = delegateRingService.getUpgraderImageTag(TEST_ACCOUNT_ID);
    assertThat(upgraderImageTag).isEqualTo(LATEST_UPGRADER_IMAGE_TAG);
  }

  private void setupAccount(String accountId) {
    persistence.save(
        Account.Builder.anAccount().withUuid(accountId).withRingName(DelegateRingConstants.RING_NAME_1).build());
  }

  private void setupDelegateRing() {
    persistence.save(DelegateRing.builder()
                         .ringName(DelegateRingConstants.RING_NAME_1)
                         .delegateImageTag(LATEST_DELEGATE_IMAGE_TAG)
                         .upgraderImageTag(LATEST_UPGRADER_IMAGE_TAG)
                         .build());
  }
}

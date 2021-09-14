/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class GcpServiceAccountDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String serviceAccountId = "SERVICE_ACCOUNT_ID";
  private GcpServiceAccount gcpServiceAccount;
  @Inject private GcpServiceAccountDao gcpServiceAccountDao;

  @Before
  public void setUp() {
    gcpServiceAccount = GcpServiceAccount.builder().serviceAccountId(serviceAccountId).accountId(accountId).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldSaveAndGet() {
    gcpServiceAccountDao.save(gcpServiceAccount);
    GcpServiceAccount result = gcpServiceAccountDao.getByServiceAccountId(serviceAccountId);
    assertThat(result).isEqualToIgnoringGivenFields(gcpServiceAccount, "uuid");
  }
}

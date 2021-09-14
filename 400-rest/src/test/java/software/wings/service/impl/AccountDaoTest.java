/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AccountDaoTest extends WingsBaseTest {
  private CeLicenseInfo ceLicenseInfo;
  @Inject private AccountDao accountDao;

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpdateCeLicense() {
    ceLicenseInfo = CeLicenseInfo.builder().build();
    String accountId = accountDao.save(Account.Builder.anAccount().build());
    accountDao.updateCeLicense(accountId, ceLicenseInfo);
    Account updatedAccount = accountDao.get(accountId);
    assertThat(updatedAccount).extracting("ceLicenseInfo").isEqualTo(ceLicenseInfo);
  }
}

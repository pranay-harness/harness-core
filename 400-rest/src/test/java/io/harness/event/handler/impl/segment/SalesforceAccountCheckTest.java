/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handler.impl.segment;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;

import com.google.inject.Inject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SalesforceAccountCheckTest extends WingsBaseTest {
  @Mock private SalesforceApiCheck salesforceApiCheck;
  @Inject @InjectMocks private SalesforceAccountCheck salesforceAccountCheck;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testAccountPresentInSalesforce() {
    Account account = getAccount(AccountType.PAID);
    account.setAccountName("PositiveAccountTesting");
    when(salesforceApiCheck.isPresentInSalesforce(account)).thenReturn(true);
    boolean isPresent = salesforceAccountCheck.isAccountPresentInSalesforce(account);
    assertThat(isPresent).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testAccountPresentInSalesforce() {
    Account account = getAccount(AccountType.PAID);
    account.setAccountName("NegativeAccountTesting");
    when(salesforceApiCheck.isPresentInSalesforce(account)).thenReturn(false);
    boolean isPresent = salesforceAccountCheck.isAccountPresentInSalesforce(account);
    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC2_testAccountPresentInSalesforce() {
    Account account = getAccount(AccountType.PAID);
    when(salesforceApiCheck.isPresentInSalesforce(account)).thenReturn(true);
    boolean isPresent = salesforceAccountCheck.isAccountPresentInSalesforce(account);
    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC3_testAccountPresentInSalesforce() {
    Account account = new Account();
    when(salesforceApiCheck.isPresentInSalesforce(account)).thenReturn(false);
    boolean isPresent = salesforceAccountCheck.isAccountPresentInSalesforce(account);
    assertThat(isPresent).isFalse();
  }
}

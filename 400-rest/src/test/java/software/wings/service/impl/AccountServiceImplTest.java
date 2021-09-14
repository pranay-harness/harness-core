/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.rule.OwnerRule.XIN;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(_955_ACCOUNT_MGMT)
public class AccountServiceImplTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject AccountServiceImpl accountService;

  @Mock LicenseService licenseService;
  private static final String INDIVIDUAL_VERSION = "individualVersion";
  private static final String INDIVIDUAL_ACCOUNT = "individualAccount";
  private static final String GLOBAL_ACCOUNT = "__GLOBAL_ACCOUNT_ID__";
  private static final String GLOBAL_VERSION = "globalVersion";

  @Before
  public void setup() throws Exception {
    initMocks(this);
  }

  @Test
  @Owner(developers = XIN)
  @Category(UnitTests.class)
  public void getDelegateConfigurationFromGlobalAccount() {
    when(licenseService.isAccountDeleted(any())).thenReturn(false);

    Account account = new Account();
    account.setUuid(INDIVIDUAL_ACCOUNT);

    Account globalAccount = new Account();
    globalAccount.setUuid(GLOBAL_ACCOUNT);

    String globalVersion = GLOBAL_VERSION;
    List<String> globalDelegateVersions = new ArrayList<>();
    globalDelegateVersions.add(globalVersion);
    DelegateConfiguration globalDelegateConfiguration =
        DelegateConfiguration.builder().delegateVersions(globalDelegateVersions).build();

    globalAccount.setDelegateConfiguration(globalDelegateConfiguration);

    wingsPersistence.save(account);
    wingsPersistence.save(globalAccount);
    DelegateConfiguration resultConfiguration = accountService.getDelegateConfiguration(INDIVIDUAL_ACCOUNT);

    assertEquals(resultConfiguration, globalDelegateConfiguration);
  }

  @Test
  @Owner(developers = XIN)
  @Category(UnitTests.class)
  public void getDelegateConfigurationFromIndividualAccount() {
    when(licenseService.isAccountDeleted(any())).thenReturn(false);

    Account account = new Account();
    account.setUuid(INDIVIDUAL_ACCOUNT);

    String version = INDIVIDUAL_VERSION;
    List<String> delegateVersions = new ArrayList<>();
    delegateVersions.add(version);
    DelegateConfiguration individualConfiguration =
        DelegateConfiguration.builder().delegateVersions(delegateVersions).build();
    account.setDelegateConfiguration(individualConfiguration);

    Account globalAccount = new Account();
    globalAccount.setUuid(GLOBAL_ACCOUNT);

    String globalVersion = GLOBAL_VERSION;
    List<String> globalDelegateVersions = new ArrayList<>();
    globalDelegateVersions.add(globalVersion);
    DelegateConfiguration globalConfiguration =
        DelegateConfiguration.builder().delegateVersions(globalDelegateVersions).build();
    globalAccount.setDelegateConfiguration(globalConfiguration);

    wingsPersistence.save(account);
    wingsPersistence.save(globalAccount);

    DelegateConfiguration resultConfiguration = accountService.getDelegateConfiguration(INDIVIDUAL_ACCOUNT);

    assertEquals(resultConfiguration, individualConfiguration);
  }
}

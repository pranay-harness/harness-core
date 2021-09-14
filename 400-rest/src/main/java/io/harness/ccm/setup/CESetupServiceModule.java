/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.billing.GcpResourceManagerService;
import io.harness.ccm.billing.GcpResourceManagerServiceImpl;
import io.harness.ccm.billing.GcpServiceAccountService;
import io.harness.ccm.billing.GcpServiceAccountServiceImpl;
import io.harness.ccm.config.CEGcpServiceAccountService;
import io.harness.ccm.config.CEGcpServiceAccountServiceImpl;
import io.harness.ccm.config.GcpBillingAccountService;
import io.harness.ccm.config.GcpBillingAccountServiceImpl;
import io.harness.ccm.config.GcpOrganizationService;
import io.harness.ccm.config.GcpOrganizationServiceImpl;
import io.harness.ccm.setup.service.impl.AWSAccountServiceImpl;
import io.harness.ccm.setup.service.impl.AwsEKSClusterServiceImpl;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.impl.AWSCEConfigValidationServiceImpl;
import io.harness.ccm.setup.service.support.impl.AWSOrganizationHelperServiceImpl;
import io.harness.ccm.setup.service.support.impl.AwsEKSHelperServiceImpl;
import io.harness.ccm.setup.service.support.impl.AzureCEConfigValidationServiceImpl;
import io.harness.ccm.setup.service.support.intfc.AWSCEConfigValidationService;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import io.harness.ccm.setup.service.support.intfc.AzureCEConfigValidationService;

import com.google.inject.AbstractModule;

@OwnedBy(CE)
public class CESetupServiceModule extends AbstractModule {
  private static volatile CESetupServiceModule instance;

  public static CESetupServiceModule getInstance() {
    if (instance == null) {
      instance = new CESetupServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(AwsEKSClusterService.class).to(AwsEKSClusterServiceImpl.class);
    bind(AwsEKSHelperService.class).to(AwsEKSHelperServiceImpl.class);
    bind(AWSOrganizationHelperService.class).to(AWSOrganizationHelperServiceImpl.class);
    bind(AWSAccountService.class).to(AWSAccountServiceImpl.class);
    bind(GcpBillingAccountService.class).to(GcpBillingAccountServiceImpl.class);
    bind(AWSCEConfigValidationService.class).to(AWSCEConfigValidationServiceImpl.class);
    bind(GcpOrganizationService.class).to(GcpOrganizationServiceImpl.class);
    bind(CEGcpServiceAccountService.class).to(CEGcpServiceAccountServiceImpl.class);
    bind(GcpServiceAccountService.class).to(GcpServiceAccountServiceImpl.class);
    bind(GcpResourceManagerService.class).to(GcpResourceManagerServiceImpl.class);
    bind(AzureCEConfigValidationService.class).to(AzureCEConfigValidationServiceImpl.class);
  }
}

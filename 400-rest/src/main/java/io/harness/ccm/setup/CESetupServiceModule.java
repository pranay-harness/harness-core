/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.billing.GcpResourceManagerService;
import io.harness.ccm.billing.GcpResourceManagerServiceImpl;
import io.harness.ccm.billing.GcpServiceAccountService;
import io.harness.ccm.billing.GcpServiceAccountServiceImpl;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.bigquery.BigQueryServiceImpl;
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

  private CESetupServiceModule() {}

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
    bind(BigQueryService.class).to(BigQueryServiceImpl.class);
    bind(GcpBillingAccountService.class).to(GcpBillingAccountServiceImpl.class);
    bind(AWSCEConfigValidationService.class).to(AWSCEConfigValidationServiceImpl.class);
    bind(GcpOrganizationService.class).to(GcpOrganizationServiceImpl.class);
    bind(CEGcpServiceAccountService.class).to(CEGcpServiceAccountServiceImpl.class);
    bind(GcpServiceAccountService.class).to(GcpServiceAccountServiceImpl.class);
    bind(GcpResourceManagerService.class).to(GcpResourceManagerServiceImpl.class);
    bind(AzureCEConfigValidationService.class).to(AzureCEConfigValidationServiceImpl.class);
  }
}

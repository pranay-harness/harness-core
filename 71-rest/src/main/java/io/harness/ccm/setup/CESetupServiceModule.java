package io.harness.ccm.setup;

import com.google.inject.AbstractModule;

import io.harness.ccm.setup.service.impl.AWSAccountServiceImpl;
import io.harness.ccm.setup.service.impl.AwsEKSClusterServiceImpl;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.impl.AWSOrganizationHelperServiceImpl;
import io.harness.ccm.setup.service.support.impl.AwsEKSHelperServiceImpl;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;

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
  }
}
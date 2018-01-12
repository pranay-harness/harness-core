package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageResponse;
import software.wings.integration.BaseIntegrationTest;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.EncryptionService;

public class AwsInfrastructureProviderIntegrationTest extends BaseIntegrationTest {
  @Mock private JobScheduler jobScheduler;
  @Inject @InjectMocks private AppService appService;

  @Mock EncryptionService encryptionService;
  @Inject @InjectMocks AwsHelperService awsHelperService;
  @Inject @InjectMocks AwsInfrastructureProvider infrastructureProvider;
  Application app;
  Environment environment;

  /**
   * These properties are for the AWS Setup against which these tests are running
   */
  private static final String accessKey = "AKIAJLEKM45P4PO5QUFQ";
  private static final String secretKey = "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";
  private static final String region = "us-east-1";
  private static final String vpcID = "vpc-551e322d";
  private static final String securityGroupID = "sg-7d1c7909";
  private static final String testInstanceID = "ami-cb9ec1b1";
  private AwsConfig awsConfig;
  private AwsInfrastructureMapping awsInfrastructureMapping;
  SettingAttribute computeProviderSetting;

  @Before
  public void setUp() throws Exception {
    deleteAllDocuments(asList(Application.class));
    app = appService.save(anApplication().withName("AppA").withAccountId(accountId).build());
    environment = Environment.Builder.anEnvironment().withAppId(app.getUuid()).withName("Developmenet").build();

    SettingAttribute hostConnectionAttr = aSettingAttribute()
                                              .withAppId(app.getUuid())
                                              .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes()
                                                             .withAccessType(AccessType.KEY)
                                                             .withConnectionType(ConnectionType.SSH)
                                                             .withKey("wingsKey".toCharArray())
                                                             .build())
                                              .build();

    awsConfig = AwsConfig.builder().accessKey(accessKey).secretKey(secretKey.toCharArray()).build();

    computeProviderSetting = aSettingAttribute().withAppId(app.getUuid()).withValue(awsConfig).build();

    awsInfrastructureMapping = anAwsInfrastructureMapping()
                                   .withAppId(app.getUuid())
                                   .withEnvId(environment.getUuid())
                                   .withComputeProviderSettingId(computeProviderSetting.getUuid())
                                   .withHostConnectionAttrs(hostConnectionAttr.getUuid())
                                   .withRegion(region)
                                   .build();
  }

  @Test
  public void testListFilteredHostsNoFilters() {
    /**
     * Testing with no AWSInstanceFilter . This should return all the available instances
     */
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);

    PageResponse<Host> result =
        infrastructureProvider.listHosts(awsInfrastructureMapping, computeProviderSetting, null, null);
    assertNotNull(result);
    assertTrue(result.size() > 1);
  }

  @Test
  public void testListFilteredHostOneSecurityGroupIDFilter() {
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.getSecurityGroupIds().add(securityGroupID);
    awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
    PageResponse<Host> result =
        infrastructureProvider.listHosts(awsInfrastructureMapping, computeProviderSetting, null, null);
    assertNotNull(result);
    assertTrue(result.size() == 1);
    validateTestInstance(result.get(0));
  }

  @Test
  public void testListFilteredHostOneVpcIDFilter() {
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.getVpcIds().add(vpcID);
    awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
    PageResponse result =
        infrastructureProvider.listHosts(awsInfrastructureMapping, computeProviderSetting, null, null);
    assertNotNull(result);
    assertTrue(result.size() >= 1);
  }

  @Test
  public void testListFilteredHostOneSecurityGroupIDAndVpcIDFilter() {
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.getSecurityGroupIds().add(securityGroupID);
    awsInstanceFilter.getVpcIds().add(vpcID);
    awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
    PageResponse<Host> result =
        infrastructureProvider.listHosts(awsInfrastructureMapping, computeProviderSetting, null, null);
    assertNotNull(result);
    assertTrue(result.size() == 1);
    validateTestInstance(result.get(0));
  }

  @Test
  public void testListFilteredHostIncorrectFilter() {
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.getSecurityGroupIds().add("FakeFake");
    awsInfrastructureMapping.setAwsInstanceFilter(awsInstanceFilter);
    PageResponse<Host> result =
        infrastructureProvider.listHosts(awsInfrastructureMapping, computeProviderSetting, null, null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  private void validateTestInstance(Host host) {
    assertEquals(testInstanceID, host.getEc2Instance().getImageId());
  }
}

package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 1/24/17.
 */
public class AwsInfrastructureProviderTest extends WingsBaseTest {
  @Mock private HostService hostService;
  @Spy private AwsHelperService awsHelperService;
  @Mock private SecretManager secretManager;

  @Inject @InjectMocks private AwsInfrastructureProvider infrastructureProvider = new AwsInfrastructureProvider();

  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(AwsConfig.builder().secretKey(SECRET_KEY).accessKey(ACCESS_KEY).build())
          .build();
  private AwsConfig awsConfig = (AwsConfig) awsSetting.getValue();
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(infrastructureProvider, "secretManager", secretManager);
  }

  @Test
  public void shouldListHostsPublicDns() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(
            new Instance().withPublicDnsName("HOST_NAME_1"), new Instance().withPublicDnsName("HOST_NAME_2")));

    doReturn(describeInstancesResult)
        .when(awsHelperService)
        .describeEc2Instances(
            (AwsConfig) awsSetting.getValue(), Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(true).build();
    PageResponse<Host> hosts = infrastructureProvider.listHosts(
        awsInfrastructureMapping, awsSetting, Collections.emptyList(), new PageRequest<>());

    assertThat(hosts)
        .hasSize(2)
        .hasOnlyElementsOfType(Host.class)
        .extracting(Host::getPublicDns)
        .isEqualTo(asList("HOST_NAME_1", "HOST_NAME_2"));
    verify(awsHelperService)
        .describeEc2Instances(
            (AwsConfig) awsSetting.getValue(), Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);
  }

  @Test
  public void shouldListHostsPrivateDns() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(
            new Instance().withPrivateDnsName("HOST_NAME_1"), new Instance().withPrivateDnsName("HOST_NAME_2")));

    doReturn(describeInstancesResult)
        .when(awsHelperService)
        .describeEc2Instances(
            (AwsConfig) awsSetting.getValue(), Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(false).build();
    PageResponse<Host> hosts = infrastructureProvider.listHosts(
        awsInfrastructureMapping, awsSetting, Collections.emptyList(), new PageRequest<>());

    assertThat(hosts)
        .hasSize(2)
        .hasOnlyElementsOfType(Host.class)
        .extracting(Host::getPublicDns)
        .isEqualTo(asList("HOST_NAME_1", "HOST_NAME_2"));
    verify(awsHelperService)
        .describeEc2Instances(
            (AwsConfig) awsSetting.getValue(), Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);
  }

  @Test
  public void shouldListHostsEmpty() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();

    doReturn(describeInstancesResult)
        .when(awsHelperService)
        .describeEc2Instances(
            (AwsConfig) awsSetting.getValue(), Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(true).build();
    PageResponse<Host> hosts = infrastructureProvider.listHosts(
        awsInfrastructureMapping, awsSetting, Collections.emptyList(), new PageRequest<>());

    assertThat(hosts).hasSize(0);
    verify(awsHelperService)
        .describeEc2Instances(awsConfig, Collections.emptyList(), Regions.US_EAST_1.getName(), instancesRequest);
  }

  @Test
  public void shouldSaveHost() {
    Host reqHost = aHost().withHostName(HOST_NAME).build();
    Host savedHost = aHost().withUuid(HOST_ID).withHostName(HOST_NAME).build();

    when(hostService.saveHost(reqHost)).thenReturn(savedHost);

    Host host = infrastructureProvider.saveHost(reqHost);
    assertThat(host).isNotNull().isEqualTo(savedHost);
    verify(hostService).saveHost(reqHost);
  }

  @Test
  public void shouldDeleteHost() {
    infrastructureProvider.deleteHost(APP_ID, INFRA_MAPPING_ID, HOST_NAME);
    verify(hostService).deleteByDnsName(APP_ID, INFRA_MAPPING_ID, HOST_NAME);
  }

  @Test
  public void shouldUpdateHostConnAttrs() {
    infrastructureProvider.updateHostConnAttrs(
        anAwsInfrastructureMapping().withAppId(APP_ID).withUuid(INFRA_MAPPING_ID).build(), HOST_CONN_ATTR_ID);
    verify(hostService).updateHostConnectionAttrByInfraMappingId(APP_ID, INFRA_MAPPING_ID, HOST_CONN_ATTR_ID);
  }

  @Test
  public void shouldProvisionHosts() {
    String region = Regions.US_EAST_1.getName();
    AwsInfrastructureMapping infrastructureMapping = anAwsInfrastructureMapping()
                                                         .withRegion(region)
                                                         .withProvisionInstances(true)
                                                         .withAutoScalingGroupName("AUTOSCALING_GROUP")
                                                         .withSetDesiredCapacity(true)
                                                         .withDesiredCapacity(1)
                                                         .build();

    doReturn(singletonList("INSTANCE_ID"))
        .when(awsHelperService)
        .listInstanceIdsFromAutoScalingGroup(awsConfig, Collections.emptyList(), infrastructureMapping.getRegion(),
            infrastructureMapping.getAutoScalingGroupName());

    doReturn(new DescribeInstancesResult().withReservations(
                 new Reservation().withInstances(new Instance()
                                                     .withPrivateDnsName(HOST_NAME)
                                                     .withPublicDnsName(HOST_NAME)
                                                     .withInstanceId("INSTANCE_ID")
                                                     .withState(new InstanceState().withName("running")))))
        .when(awsHelperService)
        .describeEc2Instances(
            awsConfig, Collections.emptyList(), region, new DescribeInstancesRequest().withInstanceIds("INSTANCE_ID"));

    doReturn(HOST_NAME).when(awsHelperService).getHostnameFromPrivateDnsName(HOST_NAME);

    doNothing()
        .when(awsHelperService)
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, Collections.emptyList(),
            infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName(),
            infrastructureMapping.getDesiredCapacity(), new ManagerExecutionLogCallback());

    List<Host> hosts =
        infrastructureProvider.maybeSetAutoScaleCapacityAndGetHosts(null, null, infrastructureMapping, awsSetting);

    assertThat(hosts)
        .hasSize(1)
        .hasOnlyElementsOfType(Host.class)
        .isEqualTo(singletonList(
            aHost().withHostName(HOST_NAME).withEc2Instance(new Instance().withInstanceId("INSTANCE_ID")).build()));

    verify(awsHelperService)
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, Collections.emptyList(),
            infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName(),
            infrastructureMapping.getDesiredCapacity(), new ManagerExecutionLogCallback());
    verify(awsHelperService)
        .listInstanceIdsFromAutoScalingGroup(awsConfig, Collections.emptyList(), infrastructureMapping.getRegion(),
            infrastructureMapping.getAutoScalingGroupName());
    verify(awsHelperService)
        .describeEc2Instances(
            awsConfig, Collections.emptyList(), region, new DescribeInstancesRequest().withInstanceIds("INSTANCE_ID"));
  }
}

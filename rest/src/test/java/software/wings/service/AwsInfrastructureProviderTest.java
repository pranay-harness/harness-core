package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
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

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/24/17.
 */
public class AwsInfrastructureProviderTest extends WingsBaseTest {
  @Mock private HostService hostService;
  @Mock private AwsHelperService awsHelperService;

  @Inject @InjectMocks private AwsInfrastructureProvider infrastructureProvider = new AwsInfrastructureProvider();

  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(anAwsConfig().withSecretKey(SECRET_KEY).withAccessKey(ACCESS_KEY).build())
          .build();
  private AwsConfig awsConfig = (AwsConfig) awsSetting.getValue();
  @Before
  public void setUp() throws Exception {}

  @Test
  public void shouldListHostsPublicDns() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(
            new Instance().withPublicDnsName("HOST_NAME_1"), new Instance().withPublicDnsName("HOST_NAME_2")));
    when(awsHelperService.describeEc2Instances(
             (AwsConfig) awsSetting.getValue(), Regions.US_EAST_1.getName(), instancesRequest))
        .thenReturn(describeInstancesResult);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(true).build();
    PageResponse<Host> hosts =
        infrastructureProvider.listHosts(awsInfrastructureMapping, awsSetting, new PageRequest<>());

    assertThat(hosts)
        .hasSize(2)
        .hasOnlyElementsOfType(Host.class)
        .extracting(Host::getPublicDns)
        .isEqualTo(asList("HOST_NAME_1", "HOST_NAME_2"));
    verify(awsHelperService)
        .describeEc2Instances((AwsConfig) awsSetting.getValue(), Regions.US_EAST_1.getName(), instancesRequest);
  }

  @Test
  public void shouldListHostsPrivateDns() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(
            new Instance().withPrivateDnsName("HOST_NAME_1"), new Instance().withPrivateDnsName("HOST_NAME_2")));
    when(awsHelperService.describeEc2Instances(
             (AwsConfig) awsSetting.getValue(), Regions.US_EAST_1.getName(), instancesRequest))
        .thenReturn(describeInstancesResult);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(false).build();
    PageResponse<Host> hosts =
        infrastructureProvider.listHosts(awsInfrastructureMapping, awsSetting, new PageRequest<>());

    assertThat(hosts)
        .hasSize(2)
        .hasOnlyElementsOfType(Host.class)
        .extracting(Host::getPublicDns)
        .isEqualTo(asList("HOST_NAME_1", "HOST_NAME_2"));
    verify(awsHelperService)
        .describeEc2Instances((AwsConfig) awsSetting.getValue(), Regions.US_EAST_1.getName(), instancesRequest);
  }

  @Test
  public void shouldListHostsEmpty() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult();

    when(awsHelperService.describeEc2Instances(
             (AwsConfig) awsSetting.getValue(), Regions.US_EAST_1.getName(), instancesRequest))
        .thenReturn(describeInstancesResult);

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping().withRegion(Regions.US_EAST_1.getName()).withUsePublicDns(true).build();
    PageResponse<Host> hosts =
        infrastructureProvider.listHosts(awsInfrastructureMapping, awsSetting, new PageRequest<>());

    assertThat(hosts).hasSize(0);
    verify(awsHelperService).describeEc2Instances(awsConfig, Regions.US_EAST_1.getName(), instancesRequest);
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
  public void shouldDeleteHostByInfraMappingId() {
    infrastructureProvider.deleteHostByInfraMappingId(APP_ID, INFRA_MAPPING_ID);
    verify(hostService).deleteByInfraMappingId(APP_ID, INFRA_MAPPING_ID);
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

    when(awsHelperService.listInstanceIdsFromAutoScalingGroup(awsConfig, infrastructureMapping))
        .thenReturn(singletonList("INSTANCE_ID"));

    when(awsHelperService.describeEc2Instances(
             awsConfig, region, new DescribeInstancesRequest().withInstanceIds("INSTANCE_ID")))
        .thenReturn(new DescribeInstancesResult().withReservations(
            new Reservation().withInstances(new Instance()
                                                .withPrivateDnsName(HOST_NAME)
                                                .withPublicDnsName(HOST_NAME)
                                                .withInstanceId("INSTANCE_ID")
                                                .withState(new InstanceState().withName("running")))));
    when(awsHelperService.getHostnameFromPrivateDnsName(HOST_NAME)).thenReturn(HOST_NAME);

    List<Host> hosts = infrastructureProvider.maybeSetAutoScaleCapacityAndGetHosts(infrastructureMapping, awsSetting);

    assertThat(hosts)
        .hasSize(1)
        .hasOnlyElementsOfType(Host.class)
        .isEqualTo(singletonList(
            aHost().withHostName(HOST_NAME).withEc2Instance(new Instance().withInstanceId("INSTANCE_ID")).build()));

    verify(awsHelperService).setAutoScalingGroupCapacity(awsConfig, infrastructureMapping);
    verify(awsHelperService).listInstanceIdsFromAutoScalingGroup(awsConfig, infrastructureMapping);
    verify(awsHelperService)
        .describeEc2Instances(awsConfig, region, new DescribeInstancesRequest().withInstanceIds("INSTANCE_ID"));
  }
}

package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.AwsHost;
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
  @Mock private AmazonEC2Client amazonEC2Client;
  @Mock private AmazonAutoScalingClient amazonAutoScalingClient;

  @Inject @InjectMocks private AwsInfrastructureProvider infrastructureProvider = new AwsInfrastructureProvider();

  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(anAwsConfig().withSecretKey(SECRET_KEY).withAccessKey(ACCESS_KEY).build())
          .build();

  @Before
  public void setUp() throws Exception {
    when(awsHelperService.getAmazonEc2Client(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonEC2Client);
    when(awsHelperService.getAmazonAutoScalingClient(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonAutoScalingClient);
  }

  @Test
  public void shouldListHosts() {
    DescribeInstancesRequest instancesRequest =
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", asList("running")));
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(
            new Instance().withPublicDnsName("HOST_NAME_1"), new Instance().withPublicDnsName("HOST_NAME_2")));
    when(amazonEC2Client.describeInstances(instancesRequest)).thenReturn(describeInstancesResult);

    PageResponse<Host> hosts = infrastructureProvider.listHosts(awsSetting, new PageRequest<>());

    assertThat(hosts)
        .hasSize(2)
        .hasOnlyElementsOfType(AwsHost.class)
        .extracting(Host::getHostName)
        .isEqualTo(asList("HOST_NAME_1", "HOST_NAME_2"));
    verify(awsHelperService).getAmazonEc2Client(ACCESS_KEY, SECRET_KEY);
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
    verify(hostService).deleteByHostName(APP_ID, INFRA_MAPPING_ID, HOST_NAME);
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
    when(amazonAutoScalingClient.describeLaunchConfigurations(any(DescribeLaunchConfigurationsRequest.class)))
        .thenReturn(new DescribeLaunchConfigurationsResult().withLaunchConfigurations(
            new LaunchConfiguration().withLaunchConfigurationName("LAUNCH_CONFIG")));

    when(amazonEC2Client.runInstances(any(RunInstancesRequest.class)))
        .thenReturn(new RunInstancesResult().withReservation(
            new Reservation().withInstances(new Instance().withInstanceId("INSTANCE_ID"))));

    when(amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds("INSTANCE_ID")))
        .thenReturn(new DescribeInstancesResult().withReservations(
            new Reservation().withInstances(new Instance()
                                                .withPublicDnsName(HOST_NAME)
                                                .withInstanceId("INSTANCE_ID")
                                                .withState(new InstanceState().withName("running")))));

    List<Host> hosts = infrastructureProvider.provisionHosts(awsSetting, "LAUNCH_CONFIG", 1);

    assertThat(hosts)
        .hasSize(1)
        .hasOnlyElementsOfType(AwsHost.class)
        .isEqualTo(asList(
            anAwsHost().withHostName(HOST_NAME).withInstance(new Instance().withInstanceId("INSTANCE_ID")).build()));

    verify(awsHelperService).getAmazonEc2Client(ACCESS_KEY, SECRET_KEY);
    verify(awsHelperService).getAmazonAutoScalingClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonAutoScalingClient).describeLaunchConfigurations(any(DescribeLaunchConfigurationsRequest.class));
    verify(amazonEC2Client).runInstances(any(RunInstancesRequest.class));
    verify(amazonEC2Client, times(2)).describeInstances(any(DescribeInstancesRequest.class));
  }

  @Test
  public void shouldDeProvisionHosts() {
    when(hostService.list(any(PageRequest.class)))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(anAwsHost().withInstance(new Instance().withInstanceId("INSTANCE_ID")).build()))
                .build());

    infrastructureProvider.deProvisionHosts(APP_ID, INFRA_MAPPING_ID, awsSetting, asList(HOST_NAME));

    verify(amazonEC2Client).terminateInstances(new TerminateInstancesRequest().withInstanceIds("INSTANCE_ID"));
    verify(awsHelperService).getAmazonEc2Client(ACCESS_KEY, SECRET_KEY);
  }
}

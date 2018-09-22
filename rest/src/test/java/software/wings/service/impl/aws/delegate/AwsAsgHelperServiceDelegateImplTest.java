package software.wings.service.impl.aws.delegate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.TimeLimiter;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AwsAsgHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;
  @Mock private EncryptionService mockEncryptionService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Spy @InjectMocks private AwsAsgHelperServiceDelegateImpl awsAsgHelperServiceDelegate;

  @Test
  public void testListAutoScalingGroupNames() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    List<AutoScalingGroup> groups = asList(new AutoScalingGroup().withAutoScalingGroupName("name1"),
        new AutoScalingGroup().withAutoScalingGroupName("name2"));
    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(groups))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    List<String> result = awsAsgHelperServiceDelegate.listAutoScalingGroupNames(
        AwsConfig.builder().build(), Collections.emptyList(), "us-east-1");
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("name1");
    assertThat(result.get(1)).isEqualTo("name2");
  }

  @Test
  public void testListAutoScalingGroupInstances() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    AutoScalingGroup autoScalingGroup = new AutoScalingGroup().withInstances(
        new com.amazonaws.services.autoscaling.model.Instance().withInstanceId("id1"));
    DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(autoScalingGroup);
    doReturn(describeAutoScalingGroupsResult).when(mockClient).describeAutoScalingGroups(any());
    doReturn(singletonList(new com.amazonaws.services.ec2.model.Instance().withInstanceId("id1")))
        .when(mockAwsEc2HelperServiceDelegate)
        .listEc2Instances(any(), anyList(), anyList(), anyString());
    List<com.amazonaws.services.ec2.model.Instance> instanceList =
        awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
            AwsConfig.builder().build(), emptyList(), "us-east-1", "name");
    assertThat(instanceList).isNotNull();
    assertThat(instanceList.size()).isEqualTo(1);
    assertThat(instanceList.get(0).getInstanceId()).isEqualTo("id1");
  }

  @Test
  public void testGetAutoScalingGroup() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(
                 new AutoScalingGroup().withAutoScalingGroupName("asgName")))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    AutoScalingGroup autoScalingGroup = awsAsgHelperServiceDelegate.getAutoScalingGroup(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "asgName");
    assertThat(autoScalingGroup).isNotNull();
    assertThat(autoScalingGroup.getAutoScalingGroupName()).isEqualTo("asgName");
  }

  @Test
  public void testGetLaunchConfiguration() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeLaunchConfigurationsResult().withLaunchConfigurations(
                 new LaunchConfiguration().withLaunchConfigurationName("lcName")))
        .when(mockClient)
        .describeLaunchConfigurations(any());
    LaunchConfiguration launchConfiguration = awsAsgHelperServiceDelegate.getLaunchConfiguration(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "lcName");
    assertThat(launchConfiguration).isNotNull();
    assertThat(launchConfiguration.getLaunchConfigurationName()).isEqualTo("lcName");
  }

  @Test
  public void testDeleteLaunchConfig() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    awsAsgHelperServiceDelegate.deleteLaunchConfig(AwsConfig.builder().build(), emptyList(), "us-east-1", "asgName");
    verify(mockClient).deleteLaunchConfiguration(any());
  }

  @Test
  public void testCreateLaunchConfiguration() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new CreateLaunchConfigurationResult()).when(mockClient).createLaunchConfiguration(any());
    CreateLaunchConfigurationResult result = awsAsgHelperServiceDelegate.createLaunchConfiguration(
        AwsConfig.builder().build(), emptyList(), "us-east-1", new CreateLaunchConfigurationRequest());
    assertThat(result).isNotNull();
  }

  @Test
  public void testCreateAutoScalingGroup() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new CreateAutoScalingGroupResult()).when(mockClient).createAutoScalingGroup(any());
    LogCallback mockCallback = mock(LogCallback.class);
    CreateAutoScalingGroupResult result = awsAsgHelperServiceDelegate.createAutoScalingGroup(
        AwsConfig.builder().build(), emptyList(), "us-east-1", new CreateAutoScalingGroupRequest(), mockCallback);
    assertThat(result).isNotNull();
  }

  @Test
  public void testDeleteAutoScalingGroups() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    LogCallback mockCallback = mock(LogCallback.class);
    try {
      doReturn(true).when(mockTimeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
      awsAsgHelperServiceDelegate.deleteAutoScalingGroups(
          AwsConfig.builder().build(), emptyList(), "us-east-1", singletonList(new AutoScalingGroup()), mockCallback);
      verify(mockClient).deleteAutoScalingGroup(any());
      verify(mockClient).deleteLaunchConfiguration(any());
      verify(mockTimeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
    } catch (Exception ex) {
      fail(format("Test threw an exception: [%s]", ex.getMessage()));
    }
  }

  @Test
  public void testGetDesiredCapacitiesOfAsgs() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());

    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(
                 new AutoScalingGroup().withAutoScalingGroupName("name1").withDesiredCapacity(1),
                 new AutoScalingGroup().withAutoScalingGroupName("name2").withDesiredCapacity(2)))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    Map<String, Integer> result = awsAsgHelperServiceDelegate.getDesiredCapacitiesOfAsgs(
        AwsConfig.builder().build(), emptyList(), "us-east-1", asList("name1", "name2"));
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get("name1")).isEqualTo(1);
    assertThat(result.get("name2")).isEqualTo(2);
  }

  @Test
  public void testSetAutoScalingGroupLimits() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new AutoScalingGroup().withMinSize(2)))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "foo", 1, mockCallback);
    verify(mockClient).updateAutoScalingGroup(any());
  }

  @Test
  public void testSetMinInstancesForAsg() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new DescribeAutoScalingGroupsResult().withAutoScalingGroups(
                 new AutoScalingGroup().withAutoScalingGroupName("foo")))
        .when(mockClient)
        .describeAutoScalingGroups(any());
    awsAsgHelperServiceDelegate.setMinInstancesForAsg(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "foo", 1, mockCallback);
    verify(mockClient).updateAutoScalingGroup(any());
  }

  @Test
  public void testSetAutoScalingGroupCapacityAndWaitForInstancesReadyState() {
    AmazonAutoScalingClient mockClient = mock(AmazonAutoScalingClient.class);
    doReturn(mockClient).when(awsAsgHelperServiceDelegate).getAmazonAutoScalingClient(any(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(new SetDesiredCapacityResult()).when(mockClient).setDesiredCapacity(any());
    try {
      doReturn(true).when(mockTimeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
      awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
          AwsConfig.builder().build(), emptyList(), "us-east-1", "asgName", 1, mockCallback, 10);
      verify(mockClient).setDesiredCapacity(any());
      verify(mockTimeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
    } catch (Exception ex) {
      fail(format("Test threw an exception: [%s]", ex.getMessage()));
    }
  }

  @Test
  public void testAllInstanceInReadyState() {
    doReturn(singletonList(new Instance().withState(new InstanceState().withName("warming"))))
        .doReturn(singletonList(new Instance().withState(new InstanceState().withName("running"))))
        .when(mockAwsEc2HelperServiceDelegate)
        .listEc2Instances(any(), anyList(), anyList(), anyString());
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    try {
      boolean result = (boolean) invokeMethod(awsAsgHelperServiceDelegate, true, "allInstanceInReadyState",
          new Object[] {AwsConfig.builder().build(), emptyList(), "us-east-1", singletonList("id"), mockCallback});
      assertThat(result).isFalse();
      result = (boolean) invokeMethod(awsAsgHelperServiceDelegate, true, "allInstanceInReadyState",
          new Object[] {AwsConfig.builder().build(), emptyList(), "us-east-1", singletonList("id"), mockCallback});
      assertThat(result).isTrue();
    } catch (Exception ex) {
      Assert.fail(format("Exception: [%s]", ex.getMessage()));
    }
  }
}
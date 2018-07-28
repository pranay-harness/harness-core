package software.wings.service.impl.aws.delegate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.service.impl.aws.delegate.AwsAmiHelperServiceDelegateImpl.HARNESS_AUTOSCALING_GROUP_TAG;
import static software.wings.service.impl.aws.delegate.AwsAmiHelperServiceDelegateImpl.NAME_TAG;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.TagDescription;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.exception.InvalidRequestException;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AwsAmiHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private ExecutorService mockExecutorService;
  @Mock private AwsAsgHelperServiceDelegate mockAwsAsgHelperServiceDelegate;

  @InjectMocks private AwsAmiHelperServiceDelegateImpl awsAmiHelperServiceDelegate;

  @Test
  public void testResizeAsgs() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing()
        .when(mockAwsAsgHelperServiceDelegate)
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            any(), anyList(), anyString(), anyString(), anyInt(), any(), anyInt());
    try {
      invokeMethod(awsAmiHelperServiceDelegate, true, "resizeAsgs",
          new Object[] {"us-east-1", AwsConfig.builder().build(), emptyList(), "newName", 1, "oldName", 2, mockCallback,
              true, 10});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    verify(mockAwsAsgHelperServiceDelegate, times(2))
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            any(), anyList(), anyString(), anyString(), anyInt(), any(), anyInt());
  }

  @Test
  public void testCreateNewAutoScalingGroupRequest() {
    AutoScalingGroup baseAutoScalingGroup =
        new AutoScalingGroup()
            .withTags(new TagDescription().withKey("k1").withValue("v1"),
                new TagDescription().withKey("k2").withValue("v2"),
                new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("id__1"),
                new TagDescription().withKey(NAME_TAG).withValue("oldName"))
            .withMaxSize(10)
            .withDefaultCooldown(11)
            .withAvailabilityZones("z1", "z2")
            .withTerminationPolicies("p1", "p2")
            .withNewInstancesProtectedFromScaleIn(true)
            .withDefaultCooldown(12)
            .withHealthCheckType("hcType")
            .withHealthCheckGracePeriod(13)
            .withPlacementGroup("pGroup")
            .withVPCZoneIdentifier("vpcI");
    CreateAutoScalingGroupRequest request = null;
    try {
      request = (CreateAutoScalingGroupRequest) invokeMethod(awsAmiHelperServiceDelegate, true,
          "createNewAutoScalingGroupRequest",
          new Object[] {"id", asList("lb1", "lb2"), asList("a1", "a2"), "newName", baseAutoScalingGroup, 2});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    assertThat(request).isNotNull();
    assertThat(request.getTags().size()).isEqualTo(4);
    assertThat(request.getAutoScalingGroupName()).isEqualTo("newName");
    assertThat(request.getLaunchConfigurationName()).isEqualTo("newName");
    assertThat(request.getDesiredCapacity()).isEqualTo(0);
    assertThat(request.getMinSize()).isEqualTo(0);
    assertThat(request.getMaxSize()).isEqualTo(10);
    assertThat(request.getDefaultCooldown()).isEqualTo(12);
    assertThat(request.getAvailabilityZones()).isEqualTo(asList("z1", "z2"));
    assertThat(request.getTerminationPolicies()).isEqualTo(asList("p1", "p2"));
    assertThat(request.getNewInstancesProtectedFromScaleIn()).isEqualTo(true);
    assertThat(request.getLoadBalancerNames()).isEqualTo(asList("lb1", "lb2"));
    assertThat(request.getTargetGroupARNs()).isEqualTo(asList("a1", "a2"));
    assertThat(request.getHealthCheckType()).isEqualTo("hcType");
    assertThat(request.getHealthCheckGracePeriod()).isEqualTo(13);
    assertThat(request.getPlacementGroup()).isEqualTo("pGroup");
    assertThat(request.getVPCZoneIdentifier()).isEqualTo("vpcI");
  }

  @Test
  public void testCreateNewLaunchConfigurationRequest() {
    LaunchConfiguration cloneBaseLaunchConfiguration =
        new LaunchConfiguration()
            .withSecurityGroups("sg1", "sg2")
            .withClassicLinkVPCId("cLVI")
            .withBlockDeviceMappings(new BlockDeviceMapping().withDeviceName("dName"))
            .withEbsOptimized(true)
            .withAssociatePublicIpAddress(true)
            .withInstanceType("iType")
            .withKernelId("kId")
            .withRamdiskId("rId")
            .withInstanceMonitoring(new InstanceMonitoring().withEnabled(true))
            .withSpotPrice("sPrice")
            .withIamInstanceProfile("iAmProfile")
            .withPlacementTenancy("pTency")
            .withKeyName("key");
    CreateLaunchConfigurationRequest request = null;
    try {
      request = (CreateLaunchConfigurationRequest) invokeMethod(awsAmiHelperServiceDelegate, true,
          "createNewLaunchConfigurationRequest",
          new Object[] {"aRev", cloneBaseLaunchConfiguration, "newName", "userData"});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    assertThat(request).isNotNull();
    assertThat(request.getLaunchConfigurationName()).isEqualTo("newName");
    assertThat(request.getImageId()).isEqualTo("aRev");
    assertThat(request.getSecurityGroups()).isEqualTo(asList("sg1", "sg2"));
    assertThat(request.getClassicLinkVPCId()).isEqualTo("cLVI");
    assertThat(request.getBlockDeviceMappings().get(0).getDeviceName()).isEqualTo("dName");
    assertThat(request.getEbsOptimized()).isEqualTo(true);
    assertThat(request.getAssociatePublicIpAddress()).isEqualTo(true);
    assertThat(request.getUserData()).isEqualTo("userData");
    assertThat(request.getInstanceType()).isEqualTo("iType");
    assertThat(request.getKernelId()).isEqualTo("kId");
    assertThat(request.getRamdiskId()).isEqualTo("rId");
    assertThat(request.getInstanceMonitoring().getEnabled()).isEqualTo(true);
    assertThat(request.getSpotPrice()).isEqualTo("sPrice");
    assertThat(request.getIamInstanceProfile()).isEqualTo("iAmProfile");
    assertThat(request.getPlacementTenancy()).isEqualTo("pTency");
    assertThat(request.getKeyName()).isEqualTo("key");
  }

  @Test
  public void testGetNewHarnessVersion() {
    List<AutoScalingGroup> groups = asList(
        new AutoScalingGroup().withTags(new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("id__1")),
        new AutoScalingGroup().withTags(
            new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("id__2")));
    Integer nextRev = null;
    try {
      nextRev =
          (Integer) invokeMethod(awsAmiHelperServiceDelegate, true, "getNewHarnessVersion", new Object[] {groups});

    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    assertThat(nextRev).isNotNull();
    assertThat(nextRev).isEqualTo(3);
  }

  @Test
  public void testGetLastDeployedAsgNameWithNonZeroCapacity() {
    List<AutoScalingGroup> groups =
        asList(new AutoScalingGroup().withDesiredCapacity(1).withAutoScalingGroupName("name1"),
            new AutoScalingGroup().withDesiredCapacity(0).withAutoScalingGroupName("name0"));
    String groupName = null;
    try {
      groupName = (String) invokeMethod(
          awsAmiHelperServiceDelegate, true, "getLastDeployedAsgNameWithNonZeroCapacity", new Object[] {groups});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    assertThat(groupName).isNotNull();
    assertThat(groupName).isEqualTo("name1");
  }

  @Test
  public void testListAllHarnessManagedAsgs() {
    List<AutoScalingGroup> groups = asList(new AutoScalingGroup(),
        new AutoScalingGroup()
            .withAutoScalingGroupName("foo")
            .withTags(new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("idFoo"))
            .withCreatedTime(new Date(10)),
        new AutoScalingGroup()
            .withAutoScalingGroupName("bar")
            .withTags(new TagDescription().withKey(HARNESS_AUTOSCALING_GROUP_TAG).withValue("idBar"))
            .withCreatedTime(new Date(20)));
    doReturn(groups).when(mockAwsAsgHelperServiceDelegate).listAllAsgs(any(), anyList(), anyString());
    List<AutoScalingGroup> result = null;
    try {
      result = (List<AutoScalingGroup>) invokeMethod(awsAmiHelperServiceDelegate, true, "listAllHarnessManagedAsgs",
          new Object[] {AwsConfig.builder().build(), emptyList(), "us-east-1", "id"});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0).getAutoScalingGroupName()).isEqualTo("bar");
    assertThat(result.get(1).getAutoScalingGroupName()).isEqualTo("foo");
  }

  @Test
  public void testEnsureAndGetBaseLaunchConfiguration() {
    ExecutionLogCallback mockLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any());
    doReturn(null)
        .when(mockAwsAsgHelperServiceDelegate)
        .getLaunchConfiguration(any(), anyList(), anyString(), anyString());
    try {
      invokeMethod(awsAmiHelperServiceDelegate, true, "ensureAndGetBaseLaunchConfiguration",
          new Object[] {AwsConfig.builder().build(), emptyList(), "us-east-1", "asgName", new AutoScalingGroup(),
              mockLogCallback});
      fail("Exception should have been thrown");
    } catch (InvocationTargetException ex) {
      assertTrue(ex.getTargetException() instanceof InvalidRequestException);
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
  }

  @Test
  public void testEnsureAndGetBaseAutoScalingGroup() {
    ExecutionLogCallback mockLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any());
    doReturn(null)
        .when(mockAwsAsgHelperServiceDelegate)
        .getAutoScalingGroup(any(), anyList(), anyString(), anyString());
    try {
      invokeMethod(awsAmiHelperServiceDelegate, true, "ensureAndGetBaseAutoScalingGroup",
          new Object[] {AwsConfig.builder().build(), emptyList(), "us-east-1", "asgName", mockLogCallback});
      fail("Exception should have been thrown");
    } catch (InvocationTargetException ex) {
      assertTrue(ex.getTargetException() instanceof InvalidRequestException);
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }
  }
}
package software.wings.service.impl.aws.delegate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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

import com.google.common.collect.ImmutableMap;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.TagDescription;
import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse.AwsAmiServiceSetupResponseBuilder;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesResponse;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AwsAmiHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private ExecutorService mockExecutorService;
  @Mock private AwsAsgHelperServiceDelegate mockAwsAsgHelperServiceDelegate;
  @Mock private AwsElbHelperServiceDelegate mockAwsElbHelperServiceDelegate;
  @Mock private EncryptionService mockEncryptionService;

  @InjectMocks private AwsAmiHelperServiceDelegateImpl awsAmiHelperServiceDelegate;

  @Test
  public void testSwitchAmiRoutes() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    AwsAmiSwitchRoutesRequest request = AwsAmiSwitchRoutesRequest.builder()
                                            .awsConfig(AwsConfig.builder().build())
                                            .encryptionDetails(emptyList())
                                            .region("us-east-1")
                                            .primaryClassicLBs(singletonList("primaryClassicLB"))
                                            .primaryTargetGroupARNs(singletonList("primaryTargetGpArn"))
                                            .stageClassicLBs(singletonList("stageClassicLB"))
                                            .stageTargetGroupARNs(singletonList("stageTargetGroupArn"))
                                            .registrationTimeout(10)
                                            .oldAsgName("Old_Asg")
                                            .newAsgName("New_Asg")
                                            .downscaleOldAsg(true)
                                            .build();
    AwsAmiSwitchRoutesResponse response = awsAmiHelperServiceDelegate.switchAmiRoutes(request, mockCallback);
    verify(mockAwsAsgHelperServiceDelegate)
        .registerAsgWithTargetGroups(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsElbHelperServiceDelegate)
        .waitForAsgInstancesToRegisterWithTargetGroup(
            any(), anyList(), anyString(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .registerAsgWithClassicLBs(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsElbHelperServiceDelegate)
        .waitForAsgInstancesToRegisterWithClassicLB(
            any(), anyList(), anyString(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate, times(2))
        .deRegisterAsgWithTargetGroups(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsAsgHelperServiceDelegate, times(2))
        .deRegisterAsgWithClassicLBs(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .setAutoScalingGroupLimits(any(), anyList(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            any(), anyList(), anyString(), anyString(), anyInt(), any(), anyInt());
  }

  @Test
  public void testRollbackSwitchAmiRoutes() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    AwsAmiPreDeploymentData preDeploymentData = AwsAmiPreDeploymentData.builder()
                                                    .asgNameToMinCapacity(ImmutableMap.of("Old_Asg", 0))
                                                    .asgNameToDesiredCapacity(ImmutableMap.of("Old_Asg", 1))
                                                    .build();
    AwsAmiSwitchRoutesRequest request = AwsAmiSwitchRoutesRequest.builder()
                                            .awsConfig(AwsConfig.builder().build())
                                            .encryptionDetails(emptyList())
                                            .region("us-east-1")
                                            .primaryClassicLBs(singletonList("primaryClassicLB"))
                                            .primaryTargetGroupARNs(singletonList("primaryTargetGpArn"))
                                            .stageClassicLBs(singletonList("stageClassicLB"))
                                            .stageTargetGroupARNs(singletonList("stageTargetGroupArn"))
                                            .registrationTimeout(10)
                                            .oldAsgName("Old_Asg")
                                            .newAsgName("New_Asg")
                                            .downscaleOldAsg(true)
                                            .preDeploymentData(preDeploymentData)
                                            .build();
    AwsAmiSwitchRoutesResponse response = awsAmiHelperServiceDelegate.rollbackSwitchAmiRoutes(request, mockCallback);
    verify(mockAwsAsgHelperServiceDelegate, times(2))
        .setAutoScalingGroupLimits(any(), anyList(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate, times(2))
        .setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            any(), anyList(), anyString(), anyString(), anyInt(), any(), anyInt());
    verify(mockAwsAsgHelperServiceDelegate)
        .setMinInstancesForAsg(any(), anyList(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .registerAsgWithTargetGroups(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsElbHelperServiceDelegate)
        .waitForAsgInstancesToRegisterWithTargetGroup(
            any(), anyList(), anyString(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .registerAsgWithClassicLBs(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsElbHelperServiceDelegate)
        .waitForAsgInstancesToRegisterWithClassicLB(
            any(), anyList(), anyString(), anyString(), anyString(), anyInt(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .deRegisterAsgWithTargetGroups(any(), anyList(), anyString(), anyString(), anyList(), any());
    verify(mockAwsAsgHelperServiceDelegate)
        .deRegisterAsgWithClassicLBs(any(), anyList(), anyString(), anyString(), anyList(), any());
  }

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
          new Object[] {"us-east-1", AwsConfig.builder().build(), emptyList(), "newName", 2,
              singletonList(AwsAmiResizeData.builder().asgName("oldName").desiredCount(0).build()), mockCallback, true,
              10, 2, 0, AwsAmiPreDeploymentData.builder().build(), emptyList(), emptyList(), false});
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
          new Object[] {"id", asList("lb1", "lb2"), asList("a1", "a2"), "newName", baseAutoScalingGroup, 2, 10});
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

  @Test
  public void testPopulatePreDeploymentData() {
    List<AutoScalingGroup> scalingGroups =
        asList(new AutoScalingGroup().withAutoScalingGroupName("name_2").withDesiredCapacity(3).withMinSize(2),
            new AutoScalingGroup().withAutoScalingGroupName("name_1").withDesiredCapacity(5).withMinSize(4));
    AwsAmiServiceSetupResponseBuilder builder = AwsAmiServiceSetupResponse.builder();
    try {
      invokeMethod(
          awsAmiHelperServiceDelegate, true, "populatePreDeploymentData", new Object[] {scalingGroups, builder});
    } catch (Exception ex) {
      fail(format("Exception: [%s]", ex.getMessage()));
    }

    AwsAmiServiceSetupResponse response = builder.build();
    List<String> oldAsgNames = response.getOldAsgNames();
    assertThat(oldAsgNames).isNotEmpty();
    assertThat(oldAsgNames.size()).isEqualTo(2);
    assertThat(oldAsgNames.get(0)).isEqualTo("name_1");
    assertThat(oldAsgNames.get(1)).isEqualTo("name_2");
  }
}
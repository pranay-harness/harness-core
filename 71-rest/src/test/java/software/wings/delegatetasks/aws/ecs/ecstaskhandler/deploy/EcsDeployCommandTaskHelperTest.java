package software.wings.delegatetasks.aws.ecs.ecstaskhandler.deploy;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;
import static wiremock.com.google.common.collect.Lists.newArrayList;

import com.google.inject.Inject;

import com.amazonaws.services.applicationautoscaling.model.DeregisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsResult;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.ecs.model.Service;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceData;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.EcsResizeParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHelper;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcsDeployCommandTaskHelperTest extends WingsBaseTest {
  @Mock private AwsClusterService mockAwsClusterService;
  @Mock private AwsAppAutoScalingHelperServiceDelegate mockAwsAppAutoScalingService;
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private AwsEcsHelperServiceDelegate mockAwsEcsHelperServiceDelegate;
  @Mock private EcsContainerService mockEcsContainerService;
  @Mock private EcsCommandTaskHelper mockEcsCommandTaskHelper;
  @Spy @InjectMocks @Inject private EcsDeployCommandTaskHelper helper;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testDeregisterAutoScalarsIfExists() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    ContextData data = ContextData.builder()
                           .awsConfig(AwsConfig.builder().build())
                           .resizeParams(anEcsResizeParams()
                                             .withRegion("us-east-1")
                                             .withPreviousEcsAutoScalarsAlreadyRemoved(false)
                                             .withPreviousAwsAutoScalarConfigs(singletonList(
                                                 AwsAutoScalarConfig.builder().scalableTargetJson("ScalTJson").build()))
                                             .build())
                           .build();
    ScalableTarget target = new ScalableTarget().withResourceId("resId").withScalableDimension("scalDim");
    doReturn(target).when(mockAwsAppAutoScalingService).getScalableTargetFromJson(anyString());
    DescribeScalableTargetsResult result = new DescribeScalableTargetsResult().withScalableTargets(
        new ScalableTarget().withResourceId("resId").withScalableDimension("scalDim"));
    doReturn(result).when(mockAwsAppAutoScalingService).listScalableTargets(anyString(), any(), anyList(), any());
    helper.deregisterAutoScalarsIfExists(data, mockCallback);
    ArgumentCaptor<DeregisterScalableTargetRequest> captor =
        ArgumentCaptor.forClass(DeregisterScalableTargetRequest.class);
    verify(mockAwsAppAutoScalingService).deregisterScalableTarget(anyString(), any(), anyList(), captor.capture());
    DeregisterScalableTargetRequest request = captor.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getResourceId()).isEqualTo("resId");
    assertThat(request.getScalableDimension()).isEqualTo("scalDim");
    assertThat(request.getServiceNamespace()).isEqualTo("ecs");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testDeleteAutoScalarForNewService() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    ContextData data = ContextData.builder()
                           .awsConfig(AwsConfig.builder().build())
                           .resizeParams(anEcsResizeParams()
                                             .withRegion("us-east-1")
                                             .withContainerServiceName("foo__1")
                                             .withClusterName("cluster")
                                             .withPreviousEcsAutoScalarsAlreadyRemoved(false)
                                             .withPreviousAwsAutoScalarConfigs(singletonList(
                                                 AwsAutoScalarConfig.builder().scalableTargetJson("ScalTJson").build()))
                                             .build())
                           .build();
    String resId = "resId";
    doReturn(resId).when(mockEcsCommandTaskHelper).getResourceIdForEcsService(anyString(), anyString());
    DescribeScalableTargetsResult result = new DescribeScalableTargetsResult().withScalableTargets(
        new ScalableTarget().withResourceId("resId").withScalableDimension("scalDim"));
    doReturn(result).when(mockAwsAppAutoScalingService).listScalableTargets(anyString(), any(), anyList(), any());
    helper.deleteAutoScalarForNewService(data, mockCallback);
    ArgumentCaptor<DeregisterScalableTargetRequest> captor =
        ArgumentCaptor.forClass(DeregisterScalableTargetRequest.class);
    verify(mockAwsAppAutoScalingService).deregisterScalableTarget(anyString(), any(), anyList(), captor.capture());
    DeregisterScalableTargetRequest request = captor.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getResourceId()).isEqualTo("resId");
    assertThat(request.getScalableDimension()).isEqualTo("scalDim");
    assertThat(request.getServiceNamespace()).isEqualTo("ecs");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRestoreAutoScalarConfigs() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    ContextData data = ContextData.builder()
                           .awsConfig(AwsConfig.builder().build())
                           .resizeParams(anEcsResizeParams()
                                             .withRollback(true)
                                             .withRegion("us-east-1")
                                             .withContainerServiceName("foo__1")
                                             .withClusterName("cluster")
                                             .withPreviousEcsAutoScalarsAlreadyRemoved(false)
                                             .withPreviousAwsAutoScalarConfigs(
                                                 singletonList(AwsAutoScalarConfig.builder()
                                                                   .scalableTargetJson("ScalTJson")
                                                                   .scalingPolicyJson(new String[] {"ScalPJson"})
                                                                   .build()))
                                             .build())
                           .build();
    doNothing().when(helper).deleteAutoScalarForNewService(any(), any());
    ScalableTarget target = new ScalableTarget().withResourceId("resId").withScalableDimension("scalDim");
    doReturn(target).when(mockAwsAppAutoScalingService).getScalableTargetFromJson(anyString());
    DescribeScalableTargetsResult result = new DescribeScalableTargetsResult();
    doReturn(result).when(mockAwsAppAutoScalingService).listScalableTargets(anyString(), any(), anyList(), any());
    helper.restoreAutoScalarConfigs(data, mock(ContainerServiceData.class), mockCallback);
    verify(mockEcsCommandTaskHelper)
        .registerScalableTargetForEcsService(any(), anyString(), any(), anyList(), any(), any());
    verify(mockEcsCommandTaskHelper)
        .upsertScalingPolicyIfRequired(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyList(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateAutoScalarConfigIfServiceReachedMaxSize() {
    ContextData data = ContextData.builder()
                           .awsConfig(AwsConfig.builder().build())
                           .resizeParams(anEcsResizeParams()
                                             .withRegion("us-east-1")
                                             .withContainerServiceName("foo__1")
                                             .withClusterName("cluster")
                                             .withUseFixedInstances(true)
                                             .withFixedInstances(2)
                                             .withAwsAutoScalarConfigForNewService(
                                                 singletonList(AwsAutoScalarConfig.builder()
                                                                   .scalableTargetJson("ScalTJson")
                                                                   .scalingPolicyJson(new String[] {"ScalPJson"})
                                                                   .build()))
                                             .build())
                           .build();
    ContainerServiceData containerServiceData = ContainerServiceData.builder().desiredCount(2).build();
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    String resId = "resId";
    doReturn(resId).when(mockEcsCommandTaskHelper).getResourceIdForEcsService(anyString(), anyString());
    ScalableTarget target = new ScalableTarget().withResourceId("resId").withScalableDimension("scalDim");
    doReturn(target).when(mockAwsAppAutoScalingService).getScalableTargetFromJson(anyString());
    helper.createAutoScalarConfigIfServiceReachedMaxSize(data, containerServiceData, mockCallback);
    ArgumentCaptor<ScalableTarget> captor = ArgumentCaptor.forClass(ScalableTarget.class);
    verify(mockEcsCommandTaskHelper)
        .registerScalableTargetForEcsService(any(), anyString(), any(), anyList(), any(), captor.capture());
    ScalableTarget value = captor.getValue();
    assertThat(value).isNotNull();
    assertThat(value.getResourceId()).isEqualTo("resId");
    verify(mockEcsCommandTaskHelper)
        .upsertScalingPolicyIfRequired(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyList(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetDeployingToHundredPercent() {
    EcsResizeParams resizeParams = anEcsResizeParams()
                                       .withRollback(false)
                                       .withInstanceUnitType(COUNT)
                                       .withUseFixedInstances(true)
                                       .withInstanceCount(2)
                                       .withFixedInstances(2)
                                       .build();
    assertThat(helper.getDeployingToHundredPercent(resizeParams)).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetNewInstanceData() {
    ContextData data = ContextData.builder()
                           .resizeParams(anEcsResizeParams()
                                             .withRegion("us-east-1")
                                             .withClusterName("cluster")
                                             .withContainerServiceName("foo__1")
                                             .withInstanceCount(2)
                                             .withUseFixedInstances(true)
                                             .withFixedInstances(2)
                                             .withInstanceUnitType(COUNT)
                                             .build())
                           .build();
    doReturn(singletonList(new Service().withServiceName("foo__1").withDesiredCount(1)))
        .when(mockAwsClusterService)
        .getServices(anyString(), any(), anyList(), anyString());
    ContainerServiceData instanceData = helper.getNewInstanceData(data);
    assertThat(instanceData).isNotNull();
    assertThat(instanceData.getDesiredCount()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetOldInstanceData() {
    ContextData data = ContextData.builder()
                           .resizeParams(anEcsResizeParams()
                                             .withRegion("us-east-1")
                                             .withClusterName("cluster")
                                             .withContainerServiceName("foo__3")
                                             .withInstanceCount(2)
                                             .withUseFixedInstances(true)
                                             .withFixedInstances(2)
                                             .withInstanceUnitType(COUNT)
                                             .withDownsizeInstanceCount(0)
                                             .build())
                           .build();
    ContainerServiceData newServiceData = ContainerServiceData.builder().name("foo__3").previousCount(0).build();
    LinkedHashMap<String, Integer> oldActiverServiceCounts = new LinkedHashMap<>();
    oldActiverServiceCounts.put("foo__1", 1);
    oldActiverServiceCounts.put("foo__2", 1);
    oldActiverServiceCounts.put("foo__3", 0);
    doReturn(oldActiverServiceCounts)
        .when(mockAwsClusterService)
        .getActiveServiceCounts(anyString(), any(), anyList(), anyString(), anyString());
    Map<String, String> imageMap = new HashMap<>();
    imageMap.put("foo__1", "img__1");
    imageMap.put("foo__2", "img__2");
    imageMap.put("foo__3", "img__3");
    doReturn(imageMap)
        .when(mockAwsClusterService)
        .getActiveServiceImages(anyString(), any(), anyList(), anyString(), anyString(), anyString());
    List<ContainerServiceData> oldInstanceData = helper.getOldInstanceData(data, newServiceData);
    assertThat(oldInstanceData).isNotNull();
    assertThat(oldInstanceData.size()).isEqualTo(2);
    assertThat(oldInstanceData.get(0).getDesiredCount()).isEqualTo(0);
    assertThat(oldInstanceData.get(1).getDesiredCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListOfStringArrayToMap() {
    Map<String, Integer> map =
        helper.listOfStringArrayToMap(newArrayList(new String[] {"foo__1", "1"}, new String[] {"foo__2", "2"}));
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.get("foo__1")).isEqualTo(1);
    assertThat(map.get("foo__2")).isEqualTo(2);
  }
}
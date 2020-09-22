package io.harness.stateutils.buildstate;

import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.managerclient.ManagerCIResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;

public class BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Mock private ManagerCIResource managerCIResource;
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private Ambiance ambiance;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;

  private static final String CLUSTER_NAME = "K8";

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
    on(k8BuildSetupUtils).set("managerCIResource", managerCIResource);
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCILiteEngineTask() throws IOException {
    Call<RestResponse<K8sTaskExecutionResponse>> requestCall = mock(Call.class);
    BuildNumber buildNumber = BuildNumber.builder().buildNumber(1L).build();

    when(requestCall.execute())
        .thenReturn(Response.success(new RestResponse<>(K8sTaskExecutionResponse.builder().build())));
    when(managerCIResource.createK8PodTask(any(), any(), any(), any(), any())).thenReturn(requestCall);
    when(engineExpressionService.renderExpression(any(), any())).thenReturn(CLUSTER_NAME);
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(
            K8PodDetails.builder().clusterName("cluster").namespace("namespace").buildNumber(buildNumber).build());

    buildSetupUtils.executeCILiteEngineTask(
        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod(), ambiance);

    verify(managerCIResource, times(1)).createK8PodTask(any(), any(), any(), any(), any());
  }
}

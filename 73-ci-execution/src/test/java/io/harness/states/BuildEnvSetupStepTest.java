package io.harness.states;

import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.managerclient.ManagerCIResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;

public class BuildEnvSetupStepTest extends CIExecutionTest {
  @Inject private BuildEnvSetupStep buildEnvSetupStep;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private ManagerCIResource managerCIResource;
  @Mock private BuildSetupUtils buildSetupUtils;

  @Before
  public void setUp() {
    on(buildEnvSetupStep).set("managerCIResource", managerCIResource);
    on(buildEnvSetupStep).set("buildSetupUtils", buildSetupUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCISetupTask() throws IOException {
    Call<RestResponse<K8sTaskExecutionResponse>> requestCall = mock(Call.class);
    RestResponse<K8sTaskExecutionResponse> restResponse =
        new RestResponse<>(K8sTaskExecutionResponse.builder().build());

    when(requestCall.execute())
        .thenReturn(Response.success(new RestResponse<>(K8sTaskExecutionResponse.builder().build())));
    when(buildSetupUtils.executeCISetupTask(any(), any())).thenReturn(restResponse);

    buildEnvSetupStep.executeSync(null, BuildEnvSetupStepInfo.builder().build(), null, null);

    verify(buildSetupUtils, times(1)).executeCISetupTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldNotExecuteCISetupTask() throws IOException {
    Call<RestResponse<K8sTaskExecutionResponse>> requestCall = mock(Call.class);
    when(requestCall.execute())
        .thenReturn(Response.success(new RestResponse<>(K8sTaskExecutionResponse.builder().build())));
    when(buildSetupUtils.executeCISetupTask(any(), any())).thenThrow(new RuntimeException());

    buildEnvSetupStep.executeSync(null, BuildEnvSetupStepInfo.builder().build(), null, null);

    verify(buildSetupUtils, times(1)).executeCISetupTask(any(), any());
  }
}

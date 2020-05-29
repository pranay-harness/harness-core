package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.category.element.UnitTests;
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

public class BuildStepTest extends CIExecutionTest {
  @Inject private BuildStep buildStep;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private ManagerCIResource managerCIResource;

  @Before
  public void setUp() {
    on(buildStep).set("managerCIResource", managerCIResource);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCIBuildTask() throws IOException {
    Call<RestResponse<K8sTaskExecutionResponse>> requestCall = mock(Call.class);

    when(requestCall.execute())
        .thenReturn(Response.success(new RestResponse<>(K8sTaskExecutionResponse.builder().build())));
    when(managerCIResource.podCommandExecutionTask(any(), any())).thenReturn(requestCall);

    buildStep.executeSync(null,
        BuildStepInfo.builder().scriptInfos(ciExecutionPlanTestHelper.getBuildCommandSteps()).build(), null, null);

    verify(managerCIResource, times(1)).podCommandExecutionTask(any(), any());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldNotExecuteCIBuildTask() throws IOException {
    Call<RestResponse<K8sTaskExecutionResponse>> requestCall = mock(Call.class);

    when(requestCall.execute())
        .thenReturn(Response.success(new RestResponse<>(K8sTaskExecutionResponse.builder().build())));
    when(managerCIResource.podCommandExecutionTask(any(), any())).thenThrow(new RuntimeException());

    buildStep.executeSync(null,
        BuildStepInfo.builder().scriptInfos(ciExecutionPlanTestHelper.getBuildCommandSteps()).build(), null, null);

    verify(managerCIResource, times(1)).podCommandExecutionTask(any(), any());
  }
}

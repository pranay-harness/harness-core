package io.harness.perpetualtask.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.managerclient.ManagerClientV2;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;

import java.io.IOException;
import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactPerpetualTaskExecutorTest extends CategoryTest {
  private ArtifactPerpetualTaskExecutor artifactPerpetualTaskExecutor;

  @Mock private ArtifactRepositoryServiceImpl artifactRepositoryService;
  @Mock private ManagerClientV2 managerClient;
  @Mock private Call<RestResponse<Boolean>> call;

  private BuildSourceParameters buildSourceParameters;
  private PerpetualTaskId perpetualTaskId;

  @Before
  public void setUp() throws Exception {
    artifactPerpetualTaskExecutor = new ArtifactPerpetualTaskExecutor(artifactRepositoryService, managerClient);
    perpetualTaskId = PerpetualTaskId.newBuilder().setId(UUIDGenerator.generateUuid()).build();
    buildSourceParameters = BuildSourceParameters.builder().build();
  }

  @Test
  @Owner(developers = OwnerRule.SRINIVAS)
  @Category(UnitTests.class)
  public void shouldRunArtifactCollection() throws IOException {
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(buildSourceParameters));
    ArtifactCollectionTaskParams artifactCollectionTaskParams = ArtifactCollectionTaskParams.newBuilder()
                                                                    .setArtifactStreamId("ARTIFACT_STREAM_ID")
                                                                    .setBuildSourceParams(bytes)
                                                                    .build();
    when(managerClient.publishArtifactCollectionResult(
             anyString(), anyString(), any(BuildSourceExecutionResponse.class)))
        .thenReturn(call);
    when(call.execute()).thenReturn(Response.success(new RestResponse<>()));

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(artifactCollectionTaskParams)).build();
    assertThat(artifactPerpetualTaskExecutor.runOnce(perpetualTaskId, params, Instant.now())).isTrue();
    verify(artifactRepositoryService).publishCollectedArtifacts(any(BuildSourceParameters.class));
    verify(managerClient)
        .publishArtifactCollectionResult(anyString(), anyString(), any(BuildSourceExecutionResponse.class));
    verify(call).execute();
  }
}
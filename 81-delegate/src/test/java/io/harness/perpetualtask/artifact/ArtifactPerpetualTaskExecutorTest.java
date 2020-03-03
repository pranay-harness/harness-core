package io.harness.perpetualtask.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;

import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactPerpetualTaskExecutorTest extends CategoryTest {
  private ArtifactPerpetualTaskExecutor artifactPerpetualTaskExecutor;

  @Mock private ArtifactRepositoryServiceImpl artifactRepositoryService;
  private BuildSourceParameters buildSourceParameters;
  private PerpetualTaskId perpetualTaskId;

  @Before
  public void setUp() throws Exception {
    artifactPerpetualTaskExecutor = new ArtifactPerpetualTaskExecutor(artifactRepositoryService);
    perpetualTaskId = PerpetualTaskId.newBuilder().setId(UUIDGenerator.generateUuid()).build();
    buildSourceParameters = BuildSourceParameters.builder().build();
  }

  @Test
  @Owner(developers = OwnerRule.SRINIVAS)
  @Category(UnitTests.class)
  public void shouldRunArtifactCollection() {
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(buildSourceParameters));
    ArtifactCollectionTaskParams artifactCollectionTaskParams = ArtifactCollectionTaskParams.newBuilder()
                                                                    .setArtifactStreamId("ARTIFACT_STREAM_ID")
                                                                    .setBuildSourceParams(bytes)
                                                                    .build();
    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(artifactCollectionTaskParams)).build();
    assertThat(artifactPerpetualTaskExecutor.runOnce(perpetualTaskId, params, Instant.now())).isTrue();
    verify(artifactRepositoryService).publishCollectedArtifacts(any(BuildSourceParameters.class));
  }
}
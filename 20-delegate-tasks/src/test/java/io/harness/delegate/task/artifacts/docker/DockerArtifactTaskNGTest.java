package io.harness.delegate.task.artifacts.docker;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DockerArtifactTaskNGTest extends CategoryTest {
  @Mock DockerArtifactTaskHelper dockerArtifactTaskHelper;
  @InjectMocks
  private DockerArtifactTaskNG dockerArtifactTaskNG = new DockerArtifactTaskNG(
      DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunObjectParams() throws IOException {
    assertThatThrownBy(() -> dockerArtifactTaskNG.run(new Object[10]))
        .hasMessage("not implemented")
        .isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRun() throws IOException {
    ArtifactTaskResponse artifactTaskResponse = ArtifactTaskResponse.builder().build();
    when(dockerArtifactTaskHelper.getArtifactCollectResponse(any())).thenReturn(artifactTaskResponse);
    assertThat(dockerArtifactTaskNG.run(ArtifactTaskParameters.builder().build())).isEqualTo(artifactTaskResponse);

    verify(dockerArtifactTaskHelper).getArtifactCollectResponse(ArtifactTaskParameters.builder().build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunDoThrowException() throws IOException {
    ArtifactTaskResponse artifactTaskResponse =
        ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ExceptionUtils.getMessage(new GeneralException("General Exception")))
            .errorCode(ErrorCode.INVALID_ARGUMENT)
            .build();
    when(dockerArtifactTaskHelper.getArtifactCollectResponse(any()))
        .thenThrow(new GeneralException("General Exception"));
    assertThat(dockerArtifactTaskNG.run(ArtifactTaskParameters.builder().build())).isEqualTo(artifactTaskResponse);

    verify(dockerArtifactTaskHelper).getArtifactCollectResponse(ArtifactTaskParameters.builder().build());
  }
}

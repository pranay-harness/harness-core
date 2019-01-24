package software.wings.helpers.ext.customrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse;
import software.wings.helpers.ext.shell.response.ShellExecutionService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRepositoryServiceImplTest extends WingsBaseTest {
  @Inject @InjectMocks private CustomRepositoryServiceImpl customRepositoryService;
  @Mock private ShellExecutionService shellExecutionService;
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Test
  public void testGetBuildDetails() throws IOException {
    File file =
        new File(getClass().getClassLoader().getResource("./ext/customrepository/testGetBuildDetails.out").getFile());
    Map<String, String> map = new HashMap<>();
    map.put(ARTIFACT_RESULT_PATH, file.getAbsolutePath());
    ShellExecutionResponse shellExecutionResponse =
        ShellExecutionResponse.builder().exitValue(0).shellExecutionData(map).build();
    when(shellExecutionService.execute(any(ShellExecutionRequest.class))).thenReturn(shellExecutionResponse);
    ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.Builder.anArtifactStreamAttributes().withCustomScript("echo \"hello\"").build();
    List<BuildDetails> buildDetails = customRepositoryService.getBuildDetails(artifactStreamAttributes);
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.size()).isEqualTo(2);
    assertThat(buildDetails.get(0).getBuildNo()).isEqualTo("21");
  }
}

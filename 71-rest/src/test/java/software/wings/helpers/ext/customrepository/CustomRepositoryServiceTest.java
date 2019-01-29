package software.wings.helpers.ext.customrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse;
import software.wings.helpers.ext.shell.response.ShellExecutionService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRepositoryServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private CustomRepositoryServiceImpl customRepositoryService;
  @Mock private ShellExecutionService shellExecutionService;
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Test
  public void testGetBuildDetails() throws IOException {
    File file = new File("testGetBuildDetails.json");
    String json = "{\n"
        + "  \"total\": 100,\n"
        + "  \"offset\": 20,\n"
        + "  \"limit\": 40,\n"
        + "  \"results\": [\n"
        + "    {\n"
        + "      \"buildNo\": \"21\",\n"
        + "      \"metadata\": {\n"
        + "        \"tag1\": \"value1\"\n"
        + "      }\n"
        + "    },\n"
        + "    {\n"
        + "      \"buildNo\": \"22\",\n"
        + "      \"metadata\": {\n"
        + "        \"tag1\": \"value1\"\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(json);

    writer.close();

    Map<String, String> map = new HashMap<>();
    map.put(ARTIFACT_RESULT_PATH, file.getAbsolutePath());
    ShellExecutionResponse shellExecutionResponse =
        ShellExecutionResponse.builder().exitValue(0).shellExecutionData(map).build();
    when(shellExecutionService.execute(any(ShellExecutionRequest.class))).thenReturn(shellExecutionResponse);
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.Builder.anArtifactStreamAttributes()
                                                            .withCustomArtifactStreamScript("echo \"hello\"")
                                                            .build();
    List<BuildDetails> buildDetails = customRepositoryService.getBuilds(artifactStreamAttributes);
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.size()).isEqualTo(2);
    assertThat(buildDetails.get(0).getNumber()).isEqualTo("21");

    FileUtils.deleteQuietly(file);
  }
}

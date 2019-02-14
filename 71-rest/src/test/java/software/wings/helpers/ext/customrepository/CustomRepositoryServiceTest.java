package software.wings.helpers.ext.customrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.shell.ShellExecutionRequest;
import io.harness.shell.ShellExecutionResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
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
    File file = new File(System.getProperty("java.io.tmpdir") + "/"
        + "testGetBuildDetails.json");
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

  @Test
  public void testGetBuildsWithCustomMapping() throws IOException {
    File file = new File(System.getProperty("java.io.tmpdir") + "/"
        + "raw.json");
    String json =
        "{\"items\":[{\"id\":\"bWF2ZW4tcmVsZWFzZXM6MWM3ODdhMDNkYjgyMDllYjhjY2IyMDYwMTJhMWU0MmI\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"group\":\"mygroup\",\"name\":\"myartifact\",\"version\":\"1.0\",\"assets\":[{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war\",\"path\":\"mygroup/myartifact/1.0/myartifact-1.0.war\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6ZDQ4MTE3NTQxZGNiODllYzYxM2IyMzk3MzIwMWQ3YmE\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war.md5\",\"path\":\"mygroup/myartifact/1.0/myartifact-1.0.war.md5\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6MGFiODBhNzQzOTIxZTQyNjYxOWJlZjJiYmRhYTU5MWQ\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"67a74306b06d0c01624fe0d0249a570f4d093747\",\"md5\":\"74be16979710d4c4e7c6647856088456\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war.sha1\",\"path\":\"mygroup/myartifact/1.0/myartifact-1.0.war.sha1\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6MTNiMjllNDQ5ZjBlM2I4ZDM5OTY0ZWQzZTExMGUyZTM\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"10a34637ad661d98ba3344717656fcc76209c2f8\",\"md5\":\"0144712dd81be0c3d9724f5e56ce6685\"}}]},{\"id\":\"bWF2ZW4tcmVsZWFzZXM6ZGZiZWYwOWVmZTE2NDRlYTYzNTAwMWQ3MjVhYzgxMTY\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"group\":\"mygroup\",\"name\":\"myartifact\",\"version\":\"1.1\",\"assets\":[{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war\",\"path\":\"mygroup/myartifact/1.1/myartifact-1.1.war\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6MGFiODBhNzQzOTIxZTQyNmQ1ZThjYjBmNWY0ODYwODc\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war.md5\",\"path\":\"mygroup/myartifact/1.1/myartifact-1.1.war.md5\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6ZDQ4MTE3NTQxZGNiODllYzlhMzlhNjIzMGVkMzI2ZTY\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"67a74306b06d0c01624fe0d0249a570f4d093747\",\"md5\":\"74be16979710d4c4e7c6647856088456\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.1/myartifact-1.1.war.sha1\",\"path\":\"mygroup/myartifact/1.1/myartifact-1.1.war.sha1\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6ODUxMzU2NTJhOTc4YmU5YTRjOWY0MGI0ZWY0MjM1NTk\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"10a34637ad661d98ba3344717656fcc76209c2f8\",\"md5\":\"0144712dd81be0c3d9724f5e56ce6685\"}}]},{\"id\":\"bWF2ZW4tcmVsZWFzZXM6NzZkN2Q3ZTQxODZhMzkwZmQ5NmRiMjk1YjgwOTg2YWI\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"group\":\"mygroup\",\"name\":\"myartifact\",\"version\":\"1.2\",\"assets\":[{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war\",\"path\":\"mygroup/myartifact/1.2/myartifact-1.2.war\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6MTNiMjllNDQ5ZjBlM2I4ZDYwZGQ0ZjAyNmY4ZjVkYWU\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war.md5\",\"path\":\"mygroup/myartifact/1.2/myartifact-1.2.war.md5\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6ODUxMzU2NTJhOTc4YmU5YWZhNjRiNTEwYzAwODUzOGU\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"67a74306b06d0c01624fe0d0249a570f4d093747\",\"md5\":\"74be16979710d4c4e7c6647856088456\"}},{\"downloadUrl\":\"http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.2/myartifact-1.2.war.sha1\",\"path\":\"mygroup/myartifact/1.2/myartifact-1.2.war.sha1\",\"id\":\"bWF2ZW4tcmVsZWFzZXM6MGFiODBhNzQzOTIxZTQyNjRkOTU3MWZkZTEzNTJmYzQ\",\"repository\":\"maven-releases\",\"format\":\"maven2\",\"checksum\":{\"sha1\":\"10a34637ad661d98ba3344717656fcc76209c2f8\",\"md5\":\"0144712dd81be0c3d9724f5e56ce6685\"}}]}],\"continuationToken\":null}";
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(json);

    writer.close();
    Map<String, String> map = new HashMap<>();
    map.put(ARTIFACT_RESULT_PATH, file.getAbsolutePath());
    ShellExecutionResponse shellExecutionResponse =
        ShellExecutionResponse.builder().exitValue(0).shellExecutionData(map).build();
    when(shellExecutionService.execute(any(ShellExecutionRequest.class))).thenReturn(shellExecutionResponse);

    Map<String, String> attributeMapping = new HashMap<>();
    attributeMapping.put("assets[0].downloadUrl", "metadata.downloadUrl");
    attributeMapping.put("assets[0].repository", null);
    attributeMapping.put("assets[0].checksum", "checksum");
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.Builder.anArtifactStreamAttributes()
                                                            .withCustomArtifactStreamScript("echo \"hello\"")
                                                            .withArtifactRoot("$.items")
                                                            .withBuildNoPath("version")
                                                            .withArtifactAttributes(attributeMapping)
                                                            .build();
    List<BuildDetails> buildDetails = customRepositoryService.getBuilds(artifactStreamAttributes);
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.size()).isEqualTo(3);
    assertThat(buildDetails.get(0).getNumber()).isEqualTo("1.0");
    assertThat(buildDetails.get(0).getMetadata().size()).isEqualTo(3);
    assertThat(buildDetails.get(0).getMetadata().get("downloadUrl"))
        .isEqualTo("http://localhost:8081/repository/maven-releases/mygroup/myartifact/1.0/myartifact-1.0.war");
    assertThat(buildDetails.get(0).getMetadata().get("repository")).isEqualTo("maven-releases");
    assertThat(buildDetails.get(0).getMetadata().get("checksum")).isNotNull();
    FileUtils.deleteQuietly(file);
  }

  @Test(expected = WingsException.class)
  public void testGetBuildsWithInvalidCustomMappingWithoutArtifactRoot() {
    Map<String, String> attributeMapping = new HashMap<>();
    attributeMapping.put("assets[0].downloadUrl", "metadata.downloadUrl");

    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.Builder.anArtifactStreamAttributes()
                                                            .withCustomArtifactStreamScript("echo \"hello\"")
                                                            .withArtifactAttributes(attributeMapping)
                                                            .withBuildNoPath("version")
                                                            .withCustomAttributeMappingNeeded(true)
                                                            .build();
    customRepositoryService.getBuilds(artifactStreamAttributes);
  }

  @Test(expected = WingsException.class)
  public void testGetBuildsWithInvalidCustomMappingWithoutBuildNo() {
    Map<String, String> attributeMapping = new HashMap<>();
    attributeMapping.put("assets[0].downloadUrl", "metadata.downloadUrl");
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.Builder.anArtifactStreamAttributes()
                                                            .withCustomArtifactStreamScript("echo \"hello\"")
                                                            .withArtifactRoot("$.items[*]")
                                                            .withArtifactAttributes(attributeMapping)
                                                            .withCustomAttributeMappingNeeded(true)
                                                            .build();
    customRepositoryService.getBuilds(artifactStreamAttributes);
  }
}

package software.wings.helpers.ext.customrepository;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import io.harness.shell.ShellExecutionRequest;
import io.harness.shell.ShellExecutionResponse;
import io.harness.shell.ShellExecutionService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.CustomRepositoryResponse;
import software.wings.helpers.ext.jenkins.CustomRepositoryResponse.CustomRepositoryResponseBuilder;
import software.wings.helpers.ext.jenkins.CustomRepositoryResponse.Result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Singleton
@Slf4j
public class CustomRepositoryServiceImpl implements CustomRepositoryService {
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Inject private ShellExecutionService shellExecutionService;

  @Override
  public List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes) {
    if (artifactStreamAttributes.isCustomAttributeMappingNeeded()) {
      validateAttributeMapping(artifactStreamAttributes.getArtifactRoot(), artifactStreamAttributes.getBuildNoPath());
    }
    // Call Shell Executor with Request
    ShellExecutionRequest shellExecutionRequest =
        ShellExecutionRequest.builder()
            .workingDirectory(System.getProperty("java.io.tmpdir", "/tmp"))
            .scriptString(artifactStreamAttributes.getCustomArtifactStreamScript())
            .timeoutSeconds(artifactStreamAttributes.getCustomScriptTimeout() == null
                    ? 60
                    : Long.parseLong(artifactStreamAttributes.getCustomScriptTimeout()))
            .build();
    logger.info("Retrieving build details of Custom Repository");
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    List<BuildDetails> buildDetails = new ArrayList<>();
    // Get the output variables
    if (shellExecutionResponse.getExitValue() == 0) {
      Map<String, String> map = shellExecutionResponse.getShellExecutionData();
      // Read the file
      String artifactResultPath = map.get(ARTIFACT_RESULT_PATH);
      if (artifactResultPath == null) {
        logger.info("ShellExecution did not return artifact result path");
        throw new InvalidArtifactServerException("ShellExecution did not return artifact result path", USER);
      }
      // Convert to Build details
      File file = new File(artifactResultPath);
      CustomRepositoryResponse customRepositoryResponse;
      try {
        if (EmptyPredicate.isNotEmpty(artifactStreamAttributes.getArtifactRoot())) {
          JsonNode jsonObject = (JsonNode) JsonUtils.readFromFile(file, JsonNode.class);
          String json = JsonUtils.asJson(jsonObject);
          customRepositoryResponse = mapToCustomRepositoryResponse(json, artifactStreamAttributes.getArtifactRoot(),
              artifactStreamAttributes.getBuildNoPath(), artifactStreamAttributes.getArtifactAttributes());
        } else {
          customRepositoryResponse =
              (CustomRepositoryResponse) JsonUtils.readFromFile(file, CustomRepositoryResponse.class);
        }

        List<Result> results = customRepositoryResponse.getResults();
        List<String> buildNumbers = new ArrayList<>();
        if (isNotEmpty(results)) {
          results.forEach(result -> {
            final String buildNo = result.getBuildNo();
            if (isNotEmpty(buildNo)) {
              if (buildNumbers.contains(buildNo)) {
                logger.warn(
                    "There is an entry with buildNo {} already exists. So, skipping the result. Please ensure that buildNo is unique across the results",
                    buildNo);
                return;
              }
              buildDetails.add(aBuildDetails()
                                   .withNumber(buildNo)
                                   .withMetadata(result.getMetadata())
                                   .withUiDisplayName("Build# " + buildNo)
                                   .build());
              buildNumbers.add(buildNo);
            } else {
              logger.warn("There is an object in output without mandatory build number");
            }
          });
        } else {
          logger.warn("Results are empty");
        }
        logger.info("Retrieving build details of Custom Repository success");
      } catch (Exception ex) {
        String msg =
            "Failed to transform results to the Custom Repository Response. Please verify if the script output is in the required format. Reason ["
            + ExceptionUtils.getMessage(ex) + "]";
        logger.error(msg);
        throw new WingsException(msg);

      } finally {
        // Finally delete the file
        try {
          deleteFileIfExists(file.getAbsolutePath());
        } catch (IOException e) {
          logger.warn("Error occurred while deleting the file {}", file.getAbsolutePath());
        }
      }

    } else {
      throw new InvalidArtifactServerException("ShellExecution returned non-zero exit code...", USER);
    }

    return buildDetails;
  }

  private void validateAttributeMapping(String artifactRoot, String buildNoPath) {
    if (EmptyPredicate.isEmpty(artifactRoot)) {
      throw new InvalidArtifactServerException(
          "Artifacts Array Path cannot be null or empty. Please provide a valid value for Artifacts Array Path.", USER);
    }
    if (EmptyPredicate.isEmpty(buildNoPath)) {
      throw new InvalidArtifactServerException(
          "BuildNo Path cannot be null or empty. Please provide a valid value for BuildNo Path", USER);
    }
  }

  public CustomRepositoryResponse mapToCustomRepositoryResponse(
      String json, String artifactRoot, String buildNoPath, Map<String, String> map) {
    DocumentContext ctx = JsonUtils.parseJson(json);
    CustomRepositoryResponseBuilder customRepositoryResponse = CustomRepositoryResponse.builder();
    List<Result> result = new ArrayList<>();

    LinkedList<LinkedHashMap> children = JsonUtils.jsonPath(ctx, artifactRoot + "[*]");
    for (int i = 0; i < children.size(); i++) {
      Map<String, String> metadata = new HashMap<>();
      CustomRepositoryResponse.Result.ResultBuilder res = CustomRepositoryResponse.Result.builder();
      res.buildNo(JsonUtils.jsonPath(ctx, artifactRoot + "[" + i + "]." + buildNoPath));
      for (Entry<String, String> entry : map.entrySet()) {
        String mappedAttribute = EmptyPredicate.isEmpty(entry.getValue())
            ? entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1)
            : entry.getValue().substring(entry.getValue().lastIndexOf('.') + 1);
        String value = JsonUtils.jsonPath(ctx, artifactRoot + "[" + i + "]." + entry.getKey()).toString();
        metadata.put(mappedAttribute, value);
      }
      res.metadata(metadata);
      result.add(res.build());
    }
    customRepositoryResponse.results(result);
    return customRepositoryResponse.build();
  }

  @Override
  public boolean validateArtifactSource(ArtifactStreamAttributes artifactStreamAttributes) {
    List<BuildDetails> buildDetails = getBuilds(artifactStreamAttributes);
    if (isEmpty(buildDetails)) {
      throw new InvalidArtifactServerException(
          "Script execution was successful. However, no artifacts were found matching the criteria provided in script.",
          USER);
    }
    return true;
  }
}
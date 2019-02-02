package software.wings.helpers.ext.customrepository;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.CustomRepositoryResponse;
import software.wings.helpers.ext.jenkins.CustomRepositoryResponse.Result;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse;
import software.wings.helpers.ext.shell.response.ShellExecutionService;
import software.wings.utils.Misc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class CustomRepositoryServiceImpl implements CustomRepositoryService {
  private static final Logger logger = LoggerFactory.getLogger(CustomRepositoryServiceImpl.class);
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Inject private ShellExecutionService shellExecutionService;

  @Override
  public List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes) {
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
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, WingsException.USER)
            .addParam("message", "ShellExecution did not return artifact result path");
      }
      // Convert to Build details
      File file = new File(artifactResultPath);
      try {
        final CustomRepositoryResponse customRepositoryResponse =
            (CustomRepositoryResponse) JsonUtils.readFromFile(file, CustomRepositoryResponse.class);
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
              buildDetails.add(aBuildDetails().withNumber(buildNo).withMetadata(result.getMetadata()).build());
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
            + Misc.getMessage(ex) + "]";
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
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "ShellExecution returned non-zero exit code...");
    }

    return buildDetails;
  }
}
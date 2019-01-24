package software.wings.helpers.ext.customrepository;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.deleteFileIfExists;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse;
import software.wings.helpers.ext.shell.response.ShellExecutionService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Singleton
public class CustomRepositoryServiceImpl implements CustomRepositoryService {
  private static final Logger logger = LoggerFactory.getLogger(CustomRepositoryServiceImpl.class);
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Inject private ShellExecutionService shellExecutionService;

  @Override
  public List<BuildDetails> getBuildDetails(ArtifactStreamAttributes artifactStreamAttributes) throws IOException {
    // Call Shell Executor with Request
    ShellExecutionRequest shellExecutionRequest = ShellExecutionRequest.builder()
                                                      .workingDirectory("/tmp")
                                                      .scriptString(artifactStreamAttributes.getCustomScript())
                                                      .build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    List<BuildDetails> buildDetails = new ArrayList<>();
    // Get the output variables
    if (shellExecutionResponse.getExitValue() == 0) {
      Map<String, String> map = shellExecutionResponse.getShellExecutionData();
      if (isNotEmpty(map)) {
        // Read the file
        String artifactResultPath = map.get(ARTIFACT_RESULT_PATH);
        if (artifactResultPath == null) {
          logger.error("ShellExecution did not return artifact result path");
          throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER)
              .addParam("message", "ShellExecution did not return artifact result path");
        }
        File file = new File(artifactResultPath);
        if (file != null && file.exists()) {
          // Convert to Build details
          JsonParser jsonParser = new JsonParser();
          JsonObject jo;
          Reader fileReader = null;
          try {
            fileReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            jo = (JsonObject) jsonParser.parse(fileReader);
            JsonArray jsonArr = jo.getAsJsonArray("result");
            Gson gson = new Gson();
            Type collectionType = new TypeToken<Collection<BuildDetails>>() {}.getType();
            buildDetails = gson.fromJson(jsonArr, collectionType);
          } catch (FileNotFoundException e) {
            logger.error("Exception reading file: " + file.getName(), e);
            throw new WingsException(ErrorCode.FILE_READ_FAILED, WingsException.USER);
          } finally {
            // Finally delete the file
            deleteFileIfExists(file.getAbsolutePath());
            if (fileReader != null) {
              fileReader.close();
            }
          }
        }
      }
    } else {
      logger.error("Shell Execution returned non-zero exit code");
      throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER)
          .addParam("message", "ShellExecution returned non-zero exit code");
    }
    return buildDetails;
  }
}
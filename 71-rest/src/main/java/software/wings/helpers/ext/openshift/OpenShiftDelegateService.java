package software.wings.helpers.ext.openshift;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class OpenShiftDelegateService {
  @Inject private OpenShiftClient openShiftClient;

  public List<ManifestFile> processTemplatization(@NotEmpty String manifestFilesDirectory,
      @NotEmpty String ocBinaryPath, @NotEmpty String templateFilePath, ExecutionLogCallback executionLogCallback,
      List<String> paramFilesContent) {
    List<String> paramsFilePaths = null;
    if (isNotEmpty(paramFilesContent)) {
      paramsFilePaths = writeParamsToFile(manifestFilesDirectory, paramFilesContent);
    }

    CliResponse cliResponse = openShiftClient.process(
        ocBinaryPath, templateFilePath, paramsFilePaths, manifestFilesDirectory, executionLogCallback);

    if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      String processedTemplateFile = cliResponse.getOutput();
      if (isEmpty(processedTemplateFile)) {
        throw new InvalidRequestException("Oc process result can't be empty", WingsException.USER);
      }
      String kubernetesReadyYaml = prepareKubernetesReadyYaml(processedTemplateFile);

      return Collections.singletonList(
          ManifestFile.builder().fileName("manifest.yaml").fileContent(kubernetesReadyYaml).build());
    } else {
      throw new InvalidRequestException("Oc process command failed. " + cliResponse.getOutput());
    }
  }

  // Get resources from oc process result and appends with yaml delimiter ---
  private String prepareKubernetesReadyYaml(String processedTemplateFile) {
    StringBuilder resultYaml = new StringBuilder();
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);

    Object obj = yaml.load(processedTemplateFile);
    if (obj instanceof Map) {
      Object items = ((Map) obj).get("items");
      if (items instanceof List) {
        List itemsList = (List) items;

        for (int i = 0; i < itemsList.size(); i++) {
          Object item = itemsList.get(i);

          if (item instanceof Map) {
            String yamlResource = yaml.dump(item);
            resultYaml.append(yamlResource);
            if (i != itemsList.size() - 1) {
              resultYaml.append("\n---\n");
            }
          }
        }
      }
    }
    if (isEmpty(resultYaml.toString())) {
      throw new InvalidRequestException("Items list can't be empty", WingsException.USER);
    }
    return resultYaml.toString();
  }

  private List<String> writeParamsToFile(String directoryPath, List<String> paramFiles) {
    List<String> paramsFilePaths = new ArrayList<>();

    for (int i = 0; i < paramFiles.size(); i++) {
      String paramsFileName = format("params-%d", i);
      try {
        FileIo.writeUtf8StringToFile(directoryPath + '/' + paramsFileName, paramFiles.get(i));
      } catch (IOException e) {
        throw new InvalidRequestException(
            "IO Failure while writing params file. " + e.getMessage(), e, WingsException.USER);
      }
      paramsFilePaths.add(paramsFileName);
    }

    return paramsFilePaths;
  }
}

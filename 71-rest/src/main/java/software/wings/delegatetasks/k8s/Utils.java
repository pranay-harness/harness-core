package software.wings.delegatetasks.k8s;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static java.lang.String.format;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Singleton;

import io.harness.exception.KubernetesYamlException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.GetCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.RolloutStatusCommand;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.utils.Misc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  private static String eventOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,MESSAGE:.message,REASON:.reason";

  public static boolean applyManifests(Kubectl client, List<KubernetesResource> resources,
      K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8sCommandTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true);

    executionLogCallback.saveExecutionLog(applyCommand.command() + "\n");

    ProcessResult result = applyCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, INFO);
          }
        },
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, ERROR);
          }
        });

    if (result.getExitValue() != 0) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public static boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    final String eventFormat = "%-7s: %s";
    final String statusFormat = "%n%-7s: %s";

    GetCommand getEventsCommand =
        client.get().resources("events").namespace(resourceId.getNamespace()).output(eventOutputFormat).watchOnly(true);

    executionLogCallback.saveExecutionLog(getEventsCommand.command() + "\n");

    boolean success = false;

    StartedProcess eventWatchProcess = null;
    try {
      eventWatchProcess = getEventsCommand.executeInBackground(k8sCommandTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              if (line.contains(resourceId.getName())) {
                executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), INFO);
              }
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), ERROR);
            }
          });

      RolloutStatusCommand rolloutStatusCommand =
          client.rollout().status().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace()).watch(true);

      executionLogCallback.saveExecutionLog(rolloutStatusCommand.command() + "\n");

      ProcessResult result = rolloutStatusCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), INFO);
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), ERROR);
            }
          });

      success = result.getExitValue() == 0;

      if (!success) {
        logger.warn(result.outputString());
      }
      return success;
    } finally {
      if (eventWatchProcess != null) {
        eventWatchProcess.getProcess().destroyForcibly().waitFor();
      }
      if (success) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      }
    }
  }

  public static String getLatestRevision(
      Kubectl client, KubernetesResourceId resourceId, K8sCommandTaskParams k8sCommandTaskParams) throws Exception {
    RolloutHistoryCommand rolloutHistoryCommand =
        client.rollout().history().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace());

    try (LogOutputStream logOutputStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             };
         LogOutputStream logErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             }) {
      ProcessResult result =
          rolloutHistoryCommand.execute(k8sCommandTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        return parseLatestRevisionNumberFromRolloutHistory(result.outputString());
      }
    }
    return "";
  }

  public static List<ManifestFile> renderTemplate(K8sCommandTaskParams k8sCommandTaskParams,
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) throws Exception {
    Optional<ManifestFile> valuesFile =
        manifestFiles.stream()
            .filter(manifestFile -> StringUtils.equals(values_filename, manifestFile.getFileName()))
            .findFirst();

    if (!valuesFile.isPresent()) {
      executionLogCallback.saveExecutionLog("No values.yaml file found. Skipping template rendering.");
      return manifestFiles;
    }

    FileIo.writeUtf8StringToFile(
        k8sCommandTaskParams.getWorkingDirectory() + '/' + values_filename, valuesFile.get().getFileContent());

    List<ManifestFile> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
        continue;
      }

      FileIo.writeUtf8StringToFile(
          k8sCommandTaskParams.getWorkingDirectory() + "/template.yaml", manifestFile.getFileContent());

      ProcessExecutor processExecutor =
          new ProcessExecutor()
              .timeout(10, TimeUnit.SECONDS)
              .directory(new File(k8sCommandTaskParams.getWorkingDirectory()))
              .commandSplit(encloseWithQuotesIfNeeded(k8sCommandTaskParams.getGoTemplateClientPath())
                  + " -t template.yaml -f " + values_filename)
              .readOutput(true);
      ProcessResult processResult = processExecutor.execute();
      result.add(ManifestFile.builder()
                     .fileName(manifestFile.getFileName())
                     .fileContent(processResult.outputString())
                     .build());
    }

    return result;
  }

  public static List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (!StringUtils.equals(values_filename, manifestFile.getFileName())) {
        try {
          result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          if (e instanceof KubernetesYamlException) {
            executionLogCallback.saveExecutionLog(e.getMessage(), ERROR);
          } else {
            executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
          }
          throw e;
        }
      }
    }

    return result.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());
  }

  public static String getResourcesInTableFormat(List<KubernetesResource> resources) {
    StringBuilder sb = new StringBuilder(1024);
    final String tableFormat = "%-20s%-40s%-6s";
    sb.append(System.lineSeparator())
        .append(format(tableFormat, "Kind", "Name", "Versioned"))
        .append(System.lineSeparator())
        .append(format(tableFormat, "----", "----", "---------"))
        .append(System.lineSeparator());

    for (KubernetesResource resource : resources) {
      KubernetesResourceId id = resource.getResourceId();
      sb.append(format(tableFormat, id.getKind(), id.getName(), id.isVersioned())).append(System.lineSeparator());
    }

    return sb.toString();
  }
}

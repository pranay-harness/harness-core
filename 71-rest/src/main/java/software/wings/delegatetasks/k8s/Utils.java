package software.wings.delegatetasks.k8s;

import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Singleton;

import io.harness.exception.KubernetesYamlException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.GetCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutStatusCommand;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  private static String eventOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,MESSAGE:.message,REASON:.reason";

  public static boolean applyManifests(Kubectl client, List<KubernetesResource> resources, String namespace,
      K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8sCommandTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    ApplyCommand applyCommand =
        client.apply().filename("manifests.yaml").namespace(namespace).record(true).output("yaml");

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
      executionLogCallback.saveExecutionLog("Failed", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("Success", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public static boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      K8sCommandTaskParams k8sCommandTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    GetCommand getEventsCommand =
        client.get().resources("events").namespace(resourceId.getNamespace()).output(eventOutputFormat).watchOnly(true);

    executionLogCallback.saveExecutionLog(getEventsCommand.command() + "\n");

    StartedProcess eventWatchProcess = null;
    try {
      eventWatchProcess = getEventsCommand.executeInBackground(k8sCommandTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              if (line.contains(resourceId.getName())) {
                executionLogCallback.saveExecutionLog("Event: " + line, INFO);
              }
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(line, ERROR);
            }
          });

      RolloutStatusCommand rolloutStatusCommand =
          client.rollout().status().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace()).watch(true);

      executionLogCallback.saveExecutionLog(rolloutStatusCommand.command() + "\n");

      ProcessResult result = rolloutStatusCommand.execute(k8sCommandTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog("Rollout Status: " + line, INFO);
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog("Rollout Status: " + line, ERROR);
            }
          });

      if (result.getExitValue() == 0) {
        executionLogCallback.saveExecutionLog("Success", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        executionLogCallback.saveExecutionLog("Failed", INFO, CommandExecutionStatus.FAILURE);
        logger.warn(result.outputString());
        return false;
      }
    } finally {
      if (eventWatchProcess != null) {
        eventWatchProcess.getProcess().destroy();
      }
    }
  }

  public static List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, int revision, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      for (ManifestFile manifestFile : manifestFiles) {
        result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
      }

      result = result.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());

      executionLogCallback.saveExecutionLog(
          "Manifests processed. Found following resources: \n" + getResourceKindRefs(result));

      addRevisionNumber(result, revision);
    } catch (Exception e) {
      if (e instanceof KubernetesYamlException) {
        executionLogCallback.saveExecutionLog(e.getMessage(), ERROR);
      }

      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      throw e;
    }

    executionLogCallback.saveExecutionLog("Success.", INFO, CommandExecutionStatus.SUCCESS);

    return result;
  }

  public static int getRevisionNumber() {
    // ToDo: Implement version history
    return 1;
  }

  private static String getResourceKindRefs(List<KubernetesResource> resources) {
    StringBuilder sb = new StringBuilder(1024);
    for (KubernetesResource resource : resources) {
      sb.append(resource.getResourceId().kindNameRef()).append(System.lineSeparator());
    }
    return sb.toString();
  }
}

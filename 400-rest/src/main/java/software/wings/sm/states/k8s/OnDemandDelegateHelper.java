package software.wings.sm.states.k8s;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
public class OnDemandDelegateHelper {
  private static final String kubectlBaseDir = "./client-tools/kubectl/";
  private static final String defaultKubectlVersion = "v1.13.2";
  private static String kubectlPath;
  private boolean runCommands(String dirctory, String commandToRun) {
    //    try {
    //      ProcessExecutor processExecutor = new ProcessExecutor()
    //              .timeout(10, TimeUnit.MINUTES)
    //              .directory(new File(dirctory))
    //              .command("/bin/bash", "-c", commandToRun)
    //              .readOutput(true);
    //      ProcessResult result = processExecutor.execute();
    //
    //      if (result.getExitValue() == 0) {
    //        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
    //        log.info(result.outputUTF8());
    //        if (validateKubectlExists(kubectlDirectory)) {
    //          log.info("kubectl path: {}", kubectlPath);
    //          return true;
    //        } else {
    //          log.error("kubectl not validated after download: {}", kubectlPath);
    //          return false;
    //        }
    //      } else {
    //        log.error("kubectl install failed");
    //        log.error(result.outputUTF8());
    //        return false;
    //      }
    //    } catch (Exception e) {
    //        log.error("Error installing kubectl", e);
    //        return false;
    //      }
    return false;
  }

  public static boolean installKubectl() {
    try {
      String kubectlDirectory = kubectlBaseDir + defaultKubectlVersion;

      if (validateKubectlExists(kubectlDirectory)) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        log.info("kubectl version {} already installed", defaultKubectlVersion);
        return true;
      }

      log.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = "https://app.harness.io/"
          + "storage/harness-download/kubernetes-release/release/" + defaultKubectlVersion + "/bin/" + getOsPath()
          + "/amd64/kubectl";

      log.info("download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
          + "chmod +x ./kubectl\n"
          + "./kubectl version --short --client\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(kubectlDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        log.info(result.outputUTF8());
        if (validateKubectlExists(kubectlDirectory)) {
          log.info("kubectl path: {}", kubectlPath);
          return true;
        } else {
          log.error("kubectl not validated after download: {}", kubectlPath);
          return false;
        }
      } else {
        log.error("kubectl install failed");
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing kubectl", e);
      return false;
    }
  }

  static String getOsPath() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "windows";
    }
    if (SystemUtils.IS_OS_MAC) {
      return "darwin";
    }
    return "linux";
  }

  private static boolean validateKubectlExists(String kubectlDirectory) {
    try {
      if (!Files.exists(Paths.get(kubectlDirectory + "/kubectl"))) {
        return false;
      }

      String script = "./kubectl version --short --client\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(kubectlDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error checking kubectl", e);
      return false;
    }
  }

  public static String encloseWithQuotesIfNeeded(String path) {
    String result = path.trim();
    if (StringUtils.containsWhitespace(result)) {
      result = "\"" + result + "\"";
    }
    return result;
  }

  public static boolean executeCommand(String command, int timeoutMinutes) {
    try {
      ProcessExecutor processExecutor =
          new ProcessExecutor().timeout(timeoutMinutes, TimeUnit.MINUTES).commandSplit(command).readOutput(true);
      ProcessResult processResult = processExecutor.execute();
      return processResult.getExitValue() == 0;
    } catch (Exception ex) {
      return false;
    }
  }
}

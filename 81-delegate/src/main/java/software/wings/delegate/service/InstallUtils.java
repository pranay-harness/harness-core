package software.wings.delegate.service;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.network.Http.getDomain;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.delegate.app.DelegateConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class InstallUtils {
  private static final Logger logger = LoggerFactory.getLogger(InstallUtils.class);

  private static final String defaultKubectlVersion = "v1.13.2";
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static final String goTemplateClientVersion = "v0.2";
  private static final String goTemplateClientBaseDir = "./client-tools/go-template/";

  private static String kubectlPath = "kubectl";
  private static String goTemplateToolPath = "go-template";

  public static String getKubectlPath() {
    return kubectlPath;
  }

  public static String getGoTemplateToolPath() {
    return goTemplateToolPath;
  }

  static void installKubectl(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getKubectlPath())) {
        kubectlPath = configuration.getKubectlPath();
        logger.info("Found user configured kubectl at {}. Skipping Install.", kubectlPath);
        return;
      }

      if (isWindows()) {
        logger.info("Skipping kubectl install on Windows");
        return;
      }

      String version = System.getenv().get("KUBECTL_VERSION");

      if (StringUtils.isEmpty(version)) {
        version = defaultKubectlVersion;
        logger.info("No version configured. Using default kubectl version", version);
      }

      String kubectlDirectory = kubectlBaseDir + version;

      if (validateKubectlExists(kubectlDirectory)) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        logger.info("kubectl version {} already installed", version);
        return;
      }

      logger.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(getManagerDomain(configuration.getManagerUrl()), version);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + "\n"
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
        logger.info(result.outputString());
        logger.info("kubectl path: {}", kubectlPath);
      } else {
        logger.error("kubectl install failed");
        logger.error(result.outputString());
      }
    } catch (Exception e) {
      logger.error("Error installing kubectl", e);
    }
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
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error checking kubectl", e);
      return false;
    }
  }

  private static String getKubectlDownloadUrl(String managerDomain, String version) {
    return "https://" + managerDomain + "/storage/harness-download/kubernetes-release/release/" + version + "/bin/"
        + getOsPath() + "/amd64/kubectl";
  }

  static void installGoTemplateTool(DelegateConfiguration configuration) {
    try {
      if (isWindows()) {
        logger.info("Skipping go-template install on Windows");
        return;
      }

      String goTemplateClientDirectory = goTemplateClientBaseDir + goTemplateClientVersion;

      if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info("go-template version {} already installed", goTemplateClientVersion);
        return;
      }

      logger.info("Installing go-template");

      createDirectoryIfDoesNotExist(goTemplateClientDirectory);

      String downloadUrl =
          getGoTemplateDownloadUrl(getManagerDomain(configuration.getManagerUrl()), goTemplateClientVersion);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./go-template\n"
          + "./go-template -v\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(goTemplateClientDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());
        logger.info("go-template path: {}", goTemplateToolPath);
      } else {
        logger.error("go-template install failed");
        logger.error(result.outputString());
      }
    } catch (Exception e) {
      logger.error("Error installing go-template", e);
    }
  }

  private static boolean validateGoTemplateClientExists(String goTemplateClientDirectory) {
    try {
      if (!Files.exists(Paths.get(goTemplateClientDirectory + "/go-template"))) {
        return false;
      }

      String script = "./go-template -v\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(goTemplateClientDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        logger.info(result.outputString());
        return true;
      } else {
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error checking go-template", e);
      return false;
    }
  }

  private static String getGoTemplateDownloadUrl(String managerDomain, String version) {
    return "https://" + managerDomain + "/storage/harness-download/snapshot-go-template/release/" + version + "/bin/"
        + getOsPath() + "/amd64/go-template";
  }

  private static String getManagerDomain(String managerUrl) {
    String managerDomain = getDomain(managerUrl);
    if (managerDomain.contains("localhost") || managerDomain.contains("127.0.0.1")) {
      managerDomain = "app.harness.io";
    }
    return managerDomain;
  }

  private static String getOsPath() {
    String osName = System.getProperty("os.name").toLowerCase();

    if (osName.startsWith("windows")) {
      return "windows";
    }

    if (osName.startsWith("mac")) {
      return "darwin";
    }

    return "linux";
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").startsWith("Windows");
  }
}

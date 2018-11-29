package software.wings.delegate.service;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

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

  private static final String defaultKubectlVersion = "v1.12.2";
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static final String goTemplateClientVersion = "v0.1";
  private static final String goTemplateClientBaseDir = "./client-tools/go-template/";

  private static String kubectlPath = "kubectl";
  private static String goTemplateToolPath = "go-template";

  public static String getKubectlPath() {
    return kubectlPath;
  }

  public static String getGoTemplateToolPath() {
    return goTemplateToolPath;
  }

  static void installKubectl(DelegateConfiguration configuration, String proxySetupScript) {
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

      if (Files.exists(Paths.get(kubectlDirectory + "/kubectl"))) {
        kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        logger.info("kubectl version {} already installed", version);
        return;
      }

      logger.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(configuration.getManagerUrl(), version);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl -LO " + downloadUrl + "\n"
          + "chmod +x ./kubectl\n"
          + "./kubectl version --short --client\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(kubectlDirectory))
                                            .command("/bin/bash", "-c", proxySetupScript + script)
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

  private static String getKubectlDownloadUrl(String managerUrl, String version) {
    String baseUrl = managerUrl.substring(0, managerUrl.lastIndexOf("/api"));
    if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
      baseUrl = "https://app.harness.io";
    }
    return baseUrl + "/storage/harness-download/kubernetes-release/release/" + version + "/bin/" + getOsPath()
        + "/amd64/kubectl";
  }

  static void installGoTemplateTool(DelegateConfiguration configuration, String proxySetupScript) {
    try {
      if (isWindows()) {
        logger.info("Skipping go-template install on Windows");
        return;
      }

      String goTemplateClientDirectory = goTemplateClientBaseDir + goTemplateClientVersion;

      if (Files.exists(Paths.get(goTemplateClientDirectory + "/go-template"))) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info("go-template version {} already installed", goTemplateClientVersion);
        return;
      }

      logger.info("Installing go-template");

      createDirectoryIfDoesNotExist(goTemplateClientDirectory);

      String downloadUrl = getGoTemplateDownloadUrl(configuration.getManagerUrl(), goTemplateClientVersion);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl -LO " + downloadUrl + "\n"
          + "chmod +x ./go-template\n"
          + "./go-template help\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(goTemplateClientDirectory))
                                            .command("/bin/bash", "-c", proxySetupScript + script)
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

  private static String getGoTemplateDownloadUrl(String managerUrl, String version) {
    String baseUrl = managerUrl.substring(0, managerUrl.lastIndexOf("/api"));
    if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
      baseUrl = "https://app.harness.io";
    }
    return baseUrl + "/storage/harness-download/snapshot-go-template/release/" + version + "/bin/" + getOsPath()
        + "/amd64/go-template";
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

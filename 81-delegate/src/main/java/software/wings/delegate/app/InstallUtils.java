package software.wings.delegate.app;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class InstallUtils {
  private static final Logger logger = LoggerFactory.getLogger(InstallUtils.class);

  private static final String defaultKubectlVersion = "v1.12.2";
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static String kubectlPath = "kubectl";

  public static String getKubectlPath() {
    return kubectlPath;
  }

  public static void installClientTools(DelegateConfiguration configuration) {
    installKubectl(configuration);
  }

  private static void installKubectl(DelegateConfiguration configuration) {
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

      String installScriptPath =
          new File(InstallUtils.class.getResource("/install-kubectl.sh").getFile()).getAbsolutePath();

      String kubectlDirectory = kubectlBaseDir + version;

      if (Files.exists(Paths.get(kubectlDirectory + "/kubectl"))) {
        logger.info("kubectl version {} already installed", version);
        return;
      }

      logger.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(configuration.getManagerUrl(), version);

      logger.info("download Url is {}", downloadUrl);

      ProcessExecutor processExecutor =
          new ProcessExecutor()
              .timeout(10, TimeUnit.MINUTES)
              .command("/bin/bash", "-c", installScriptPath + " " + kubectlDirectory + " " + downloadUrl)
              .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        kubectlPath = Paths.get(kubectlBaseDir + version + "/kubectl").toAbsolutePath().normalize().toString();
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

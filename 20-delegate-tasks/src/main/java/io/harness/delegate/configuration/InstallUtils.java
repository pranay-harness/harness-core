package io.harness.delegate.configuration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.network.Http.getBaseUrl;
import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@UtilityClass
@Slf4j
public class InstallUtils {
  private static final String defaultKubectlVersion = "v1.13.2";
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static final String goTemplateClientVersion = "v0.3";
  private static final String goTemplateClientBaseDir = "./client-tools/go-template/";

  static final String helm3Version = "v3.1.2";

  static final String helm2Version = "v2.13.1";

  private static final List<String> helmVersions = Arrays.asList(helm2Version, helm3Version);

  private static final String helmBaseDir = "./client-tools/helm/";

  private static final String chartMuseumVersion = "v0.8.2";
  private static final String chartMuseumBaseDir = "./client-tools/chartmuseum/";

  private static final String ocVersion = "v4.2.16";
  private static final String ocBaseDir = "./client-tools/oc/";

  private static String kubectlPath = "kubectl";

  private static String kustomizeBaseDir = "./client-tools/kustomize/";
  private static String kustomizeVersion = "v3.5.4";
  private static String kustomizePath = "kustomize";

  private static String goTemplateToolPath = "go-template";
  private static Map<String, String> helmPaths = new HashMap<>();

  static {
    helmPaths.put(helm2Version, "helm");
    helmPaths.put(helm3Version, "helm");
  }

  private static String chartMuseumPath = "chartmuseum";
  private static String ocPath = "oc";

  private static final String terraformConfigInspectBaseDir = "./client-tools/tf-config"
      + "-inspect";
  private static final String terraformConfigInspectBinary = "terraform-config-inspect";
  private static final String terraformConfigInspectVersion = "v1.0"; // This is not the
  // version provided by Hashicorp because currently they do not maintain releases as such

  private static final String KUBECTL_CDN_PATH = "public/shared/tools/kubectl/release/%s/bin/%s/amd64/kubectl";
  private static final String CHART_MUSEUM_CDN_PATH =
      "public/shared/tools/chartmuseum/release/%s/bin/%s/amd64/chartmuseum";
  private static final String GO_TEMPLATE_CDN_PATH =
      "public/shared/tools/go-template/release/%s/bin/%s/amd64/go-template";
  private static final String OC_CDN_PATH = "public/shared/tools/oc/release/%s/bin/%s/amd64/oc";
  private static final String HELM_CDN_PATH = "public/shared/tools/helm/release/%s/bin/%s/amd64/helm";
  private static final String TERRAFORM_CONFIG_CDN_PATH =
      "public/shared/tools/terraform-config-inspect/%s/%s/amd64/terraform-config-inspect";
  private static final String KUSTOMIZE_CDN_PATH = "public/shared/tools/kustomize/release/%s/bin/%s/amd64/kustomize";

  public static String getTerraformConfigInspectPath() {
    return join("/", terraformConfigInspectBaseDir, terraformConfigInspectVersion, getOsPath(), "amd64",
        terraformConfigInspectBinary);
  }

  public static String getKubectlPath() {
    return kubectlPath;
  }

  public static String getGoTemplateToolPath() {
    return goTemplateToolPath;
  }

  public static String getHelm2Path() {
    return helmPaths.get(helm2Version);
  }

  public static String getHelm3Path() {
    return helmPaths.get(helm3Version);
  }

  public static String getChartMuseumPath() {
    return chartMuseumPath;
  }

  public static String getOcPath() {
    return ocPath;
  }

  public static String getKustomizePath() {
    return kustomizePath;
  }

  public static boolean installKubectl(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getKubectlPath())) {
        kubectlPath = configuration.getKubectlPath();
        logger.info("Found user configured kubectl at {}. Skipping Install.", kubectlPath);
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping kubectl install on Windows");
        return true;
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
        return true;
      }

      logger.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(configuration, version);

      logger.info("download Url is {}", downloadUrl);

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
        logger.info(result.outputString());
        if (validateKubectlExists(kubectlDirectory)) {
          logger.info("kubectl path: {}", kubectlPath);
          return true;
        } else {
          logger.error("kubectl not validated after download: {}", kubectlPath);
          return false;
        }
      } else {
        logger.error("kubectl install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing kubectl", e);
      return false;
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

  private static String getKubectlDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(KUBECTL_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/kubernetes-release/release/" + version + "/bin/" + getOsPath() + "/amd64/kubectl";
  }

  public static boolean installGoTemplateTool(DelegateConfiguration configuration) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping go-template install on Windows");
        return true;
      }

      String goTemplateClientDirectory = goTemplateClientBaseDir + goTemplateClientVersion;

      if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        logger.info("go-template version {} already installed", goTemplateClientVersion);
        return true;
      }

      logger.info("Installing go-template");

      createDirectoryIfDoesNotExist(goTemplateClientDirectory);

      String downloadUrl = getGoTemplateDownloadUrl(configuration, goTemplateClientVersion);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
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
        if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
          logger.info("go-template path: {}", goTemplateToolPath);
          return true;
        } else {
          logger.error("go-template not validated after download: {}", goTemplateToolPath);
          return false;
        }
      } else {
        logger.error("go-template install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing go-template", e);
      return false;
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

  private static String getGoTemplateDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(GO_TEMPLATE_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/snapshot-go-template/release/" + version + "/bin/" + getOsPath()
        + "/amd64/go-template";
  }

  private static String getManagerBaseUrl(String managerUrl) {
    if (managerUrl.contains("localhost") || managerUrl.contains("127.0.0.1")) {
      return "https://app.harness.io/";
    }

    return getBaseUrl(managerUrl);
  }

  @VisibleForTesting
  static String getOsPath() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "windows";
    }
    if (SystemUtils.IS_OS_MAC) {
      return "darwin";
    }
    return "linux";
  }

  private static boolean validateHelmExists(String helmDirectory) {
    try {
      if (!Files.exists(Paths.get(helmDirectory + "/helm"))) {
        return false;
      }

      String script = "./helm version -c";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
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
      logger.error("Error checking helm", e);
      return false;
    }
  }

  private static String getKustomizeDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(KUSTOMIZE_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/harness-kustomize/release/" + version + "/bin/" + getOsPath() + "/amd64/kustomize";
  }

  private static String getHelmDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(HELM_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl()) + "storage/harness-download/harness-helm/release/"
        + version + "/bin/" + getOsPath() + "/amd64/helm";
  }

  private static boolean initHelmClient(String helmVersion) throws InterruptedException, TimeoutException, IOException {
    if (isHelmV2(helmVersion)) {
      logger.info("Init helm client only");

      String helmDirectory = helmBaseDir + helmVersion;
      String script = "./helm init -c --skip-refresh \n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        logger.info("Successfully init helm client");
        return true;
      } else {
        logger.error("Helm client init failed");
        logger.error(result.outputString());
        return false;
      }
    } else {
      logger.info("Init helm not needed for helm v3");
      return true;
    }
  }

  static boolean isHelmV2(String helmVersion) {
    return helmVersion.toLowerCase().startsWith("v2");
  }

  static boolean isHelmV3(String helmVersion) {
    return helmVersion.toLowerCase().startsWith("v3");
  }

  public static boolean installHelm(DelegateConfiguration configuration) {
    boolean helmInstalled = true;
    for (String version : helmVersions) {
      helmInstalled = helmInstalled && installHelm(configuration, version);
    }
    return helmInstalled;
  }

  private static boolean installHelm(DelegateConfiguration configuration, String helmVersion) {
    try {
      if (delegateConfigHasHelmPath(configuration, helmVersion)) {
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping helm install on Windows");
        return true;
      }

      String helmDirectory = helmBaseDir + helmVersion;
      if (validateHelmExists(helmDirectory)) {
        String helmPath = Paths.get(helmDirectory + "/helm").toAbsolutePath().normalize().toString();
        helmPaths.put(helmVersion, helmPath);
        logger.info(format("helm version %s already installed", helmVersion));

        return initHelmClient(helmVersion);
      }

      logger.info(format("Installing helm %s", helmVersion));
      createDirectoryIfDoesNotExist(helmDirectory);

      String downloadUrl = getHelmDownloadUrl(configuration, helmVersion);
      logger.info("Download Url is " + downloadUrl);

      String versionCommand = getHelmVersionCommand(helmVersion);
      String initCommand = getHelmInitCommand(helmVersion);
      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + " \n"
          + "chmod +x ./helm \n" + versionCommand + initCommand;

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        String helmPath = Paths.get(helmDirectory + "/helm").toAbsolutePath().normalize().toString();
        helmPaths.put(helmVersion, helmPath);
        logger.info(result.outputString());

        if (validateHelmExists(helmDirectory)) {
          logger.info("helm path: {}", helmPath);
          return true;
        } else {
          logger.error("helm not validated after download: {}", helmPath);
          return false;
        }
      } else {
        logger.error("helm install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing helm", e);
      return false;
    }
  }

  private static String getHelmInitCommand(String helmVersion) {
    return isHelmV2(helmVersion) ? "./helm init -c --skip-refresh \n" : StringUtils.EMPTY;
  }

  private static String getHelmVersionCommand(String helmVersion) {
    return isHelmV2(helmVersion) ? "./helm version -c \n" : "./helm version \n";
  }

  private static boolean validateChartMuseumExists(String chartMuseumDirectory) {
    try {
      if (!Files.exists(Paths.get(chartMuseumDirectory + "/chartmuseum"))) {
        return false;
      }

      String script = "./chartmuseum -v";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(chartMuseumDirectory))
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
      logger.error("Error checking chart museum", e);
      return false;
    }
  }

  private static String getChartMuseumDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(CHART_MUSEUM_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/harness-chartmuseum/release/" + version + "/bin/" + getOsPath()
        + "/amd64/chartmuseum";
  }

  public static boolean installChartMuseum(DelegateConfiguration configuration) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping chart museum install on Windows");
        return true;
      }

      String chartMuseumDirectory = chartMuseumBaseDir + chartMuseumVersion;
      if (validateChartMuseumExists(chartMuseumDirectory)) {
        chartMuseumPath = Paths.get(chartMuseumDirectory + "/chartmuseum").toAbsolutePath().normalize().toString();
        logger.info("chartmuseum version %s already installed", chartMuseumVersion);
        return true;
      }

      logger.info("Installing chartmuseum");
      createDirectoryIfDoesNotExist(chartMuseumDirectory);

      String downloadUrl = getChartMuseumDownloadUrl(configuration, chartMuseumVersion);
      logger.info("Download Url is " + downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
          + "chmod +x ./chartmuseum \n"
          + "./chartmuseum -v \n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(chartMuseumDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);

      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        chartMuseumPath = Paths.get(chartMuseumDirectory + "/chartmuseum").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());

        if (validateChartMuseumExists(chartMuseumDirectory)) {
          logger.info("chartmuseum path: {}", chartMuseumPath);
          return true;
        } else {
          logger.error("chartmuseum not validated after download: {}", chartMuseumPath);
          return false;
        }
      } else {
        logger.error("chart museum install failed");
        logger.error(result.outputString());
        return false;
      }

    } catch (Exception e) {
      logger.error("Error installing chart museum", e);
      return false;
    }
  }

  public static boolean installTerraformConfigInspect(DelegateConfiguration configuration) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping terraform-config-inspect install on Windows");
        return true;
      }

      final String terraformConfigInspectVersionedDirectory =
          Paths.get(getTerraformConfigInspectPath()).getParent().toString();
      if (validateTerraformConfigInspectExists(terraformConfigInspectVersionedDirectory)) {
        logger.info("terraform-config-inspect already installed at {}", terraformConfigInspectVersionedDirectory);
        return true;
      }

      logger.info("Installing terraform-config-inspect");
      createDirectoryIfDoesNotExist(terraformConfigInspectVersionedDirectory);

      String downloadUrl = getTerraformConfigInspectDownloadUrl(configuration);
      logger.info("Download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./terraform-config-inspect";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(terraformConfigInspectVersionedDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        String tfConfigInspectPath = Paths.get(getTerraformConfigInspectPath()).toAbsolutePath().toString();
        logger.info("terraform config inspect installed at {}", tfConfigInspectPath);
        return true;
      } else {
        logger.error("Error installing terraform config inspect");
        return false;
      }

    } catch (Exception ex) {
      logger.error("Error installing terraform config inspect", ex);
      return false;
    }
  }

  private static String getTerraformConfigInspectDownloadUrl(DelegateConfiguration delegateConfiguration) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(),
          String.format(TERRAFORM_CONFIG_CDN_PATH, terraformConfigInspectVersion, getOsPath()));
    }
    return join("/", getManagerBaseUrl(delegateConfiguration.getManagerUrl()),
        "storage/harness-download/harness-terraform-config"
            + "-inspect",
        terraformConfigInspectVersion, getOsPath(), "amd64", terraformConfigInspectBinary);
  }

  private static boolean validateTerraformConfigInspectExists(String terraformConfigInspectVersionedDirectory) {
    if (Files.exists(Paths.get(join("/", terraformConfigInspectVersionedDirectory, terraformConfigInspectBinary)))) {
      return true;
    }
    return false;
  }

  public static boolean installOc(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getOcPath())) {
        ocPath = configuration.getOcPath();
        logger.info("Found user configured oc at {}. Skipping Install.", ocPath);
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping oc install on Windows");
        return true;
      }

      String version = System.getenv().get("OC_VERSION");
      if (StringUtils.isEmpty(version)) {
        version = ocVersion;
        logger.info("No version configured. Using default oc version {}", version);
      }

      String ocDirectory = ocBaseDir + version;
      if (validateOcExists(ocDirectory)) {
        ocPath = Paths.get(ocDirectory, "oc").toAbsolutePath().normalize().toString();
        logger.info("oc version {} already installed", version);
        return true;
      }

      logger.info("Installing oc");
      createDirectoryIfDoesNotExist(ocDirectory);

      String downloadUrl = getOcDownloadUrl(configuration, version);
      logger.info("download url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
          + "chmod +x ./oc\n"
          + "./oc version --client\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(ocDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        ocPath = Paths.get(ocDirectory, "oc").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());
        if (validateOcExists(ocDirectory)) {
          logger.info("oc path: {}", ocPath);
          return true;
        } else {
          logger.error("oc not validated after download: {}", ocPath);
          return false;
        }
      } else {
        logger.error("oc install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing oc", e);
      return false;
    }
  }

  private static boolean validateOcExists(String ocDirectory) {
    try {
      Path path = Paths.get(ocDirectory, "oc");
      if (!path.toFile().exists()) {
        return false;
      }

      String script = "./oc version --client\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(ocDirectory))
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
      logger.error("Error checking oc", e);
      return false;
    }
  }

  private static String getOcDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(OC_CDN_PATH, version, getOsPath()));
    }
    return getManagerBaseUrl(delegateConfiguration.getManagerUrl()) + "storage/harness-download/harness-oc/release/"
        + version + "/bin/" + getOsPath() + "/amd64/oc";
  }

  public static boolean installKustomize(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getKustomizePath())) {
        kustomizePath = configuration.getKustomizePath();
        logger.info("Found user configured kustomize at {}. Skipping Install.", kustomizePath);
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        logger.info("Skipping kustomize install on Windows");
        return true;
      }

      String kustomizeDir = kustomizeBaseDir + kustomizeVersion;

      if (validateKustomizeExists(kustomizeDir)) {
        kustomizePath = Paths.get(kustomizeDir + "/kustomize").toAbsolutePath().normalize().toString();
        logger.info("kustomize version {} already installed", kustomizeVersion);
        return true;
      }

      logger.info("Installing kustomize");

      createDirectoryIfDoesNotExist(kustomizeDir);

      String downloadUrl = getKustomizeDownloadUrl(configuration, kustomizeVersion);

      logger.info("download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
          + "chmod +x ./kustomize\n"
          + "./kustomize version --short\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(kustomizeDir))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        kustomizePath = Paths.get(kustomizeDir + "/kustomize").toAbsolutePath().normalize().toString();
        logger.info(result.outputString());
        if (validateKustomizeExists(kustomizeDir)) {
          logger.info("kustomize path: {}", kustomizePath);
          return true;
        } else {
          logger.error("kustomize not validated after download: {}", kustomizePath);
          return false;
        }
      } else {
        logger.error("kustomize install failed");
        logger.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      logger.error("Error installing kustomize", e);
      return false;
    }
  }

  private static boolean validateKustomizeExists(String kustomizeDir) {
    try {
      if (!Paths.get(kustomizeDir + "/kustomize").toFile().exists()) {
        return false;
      }

      String script = "./kustomize version --short\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(kustomizeDir))
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
      logger.error("Error checking kustomize", e);
      return false;
    }
  }

  @VisibleForTesting
  static boolean delegateConfigHasHelmPath(DelegateConfiguration configuration, String helmVersion) {
    if (helm2Version.equals(helmVersion)) {
      if (isNotEmpty(configuration.getHelmPath())) {
        String helmPath = configuration.getHelmPath();
        helmPaths.put(helmVersion, helmPath);
        logger.info("Found user configured helm2 at {}. Skipping Install.", helmPath);
        return true;
      }
    } else if (helm3Version.equals(helmVersion)) {
      if (isNotEmpty(configuration.getHelm3Path())) {
        String helmPath = configuration.getHelm3Path();
        helmPaths.put(helmVersion, helmPath);
        logger.info("Found user configured helm3 at {}. Skipping Install.", helmPath);
        return true;
      }
    }
    return false;
  }
}

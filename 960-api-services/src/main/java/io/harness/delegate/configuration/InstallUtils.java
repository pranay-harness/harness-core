/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.configuration;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.network.Http.getBaseUrl;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ProcessExecutionException;

import com.google.common.annotations.VisibleForTesting;
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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@UtilityClass
@Slf4j
@OwnedBy(DEL)
public class InstallUtils {
  private static final String defaultKubectlVersion = "v1.13.2";
  private static final String newKubectlVersion = "v1.19.2";
  private static final List<String> kubectlVersions = Arrays.asList(defaultKubectlVersion, newKubectlVersion);
  private static final String kubectlBaseDir = "./client-tools/kubectl/";

  private static final String goTemplateClientVersion = "v0.4";
  private static final String goTemplateClientBaseDir = "./client-tools/go-template/";

  private static final String harnessPywinrmVersion = "v0.4-dev";
  private static final String harnessPywinrmBaseDir = "./client-tools/harness-pywinrm/";

  static final String helm3Version = "v3.1.2";

  static final String helm2Version = "v2.13.1";

  private static final List<String> helmVersions = Arrays.asList(helm2Version, helm3Version);

  private static final String helmBaseDir = "./client-tools/helm/";

  private static final String chartMuseumVersionOld = "v0.8.2";
  private static final String chartMuseumVersionNew = "v0.13.0"; // updated version from v0.8.2 to v0.13.0
  // to enable IRSA for chartmuseum
  private static final List<String> chartMuseumVersions = Arrays.asList(chartMuseumVersionOld, chartMuseumVersionNew);
  private static final Map<String, String> chartMuseumPaths = new HashMap<>();
  static {
    chartMuseumPaths.put(chartMuseumVersionOld, "chartMuseum");
    chartMuseumPaths.put(chartMuseumVersionNew, "chartMuseum");
  }

  private static final String chartMuseumBaseDir = "./client-tools/chartmuseum/";

  private static final String ocVersion = "v4.2.16";
  private static final String ocBaseDir = "./client-tools/oc/";

  private static Map<String, String> kubectlPaths = new HashMap<>();

  private static String kustomizeBaseDir = "./client-tools/kustomize/";
  private static String kustomizeVersionOld = "v3.5.4";
  private static String kustomizeVersionNew = "v4.0.0";
  private static String kustomizePath = "kustomize";
  private static boolean isCustomKustomizePath;
  private static String kustomizeBinaryName = "kustomize";

  private static final List<String> kustomizeVersions = Arrays.asList(kustomizeVersionOld, kustomizeVersionNew);

  private static String goTemplateToolPath = "go-template";
  private static String harnessPywinrmToolPath = "harness-pywinrm";
  private static Map<String, String> helmPaths = new HashMap<>();

  static {
    helmPaths.put(helm2Version, "helm");
    helmPaths.put(helm3Version, "helm");
    kubectlPaths.put(defaultKubectlVersion, "kubectl");
    kubectlPaths.put(newKubectlVersion, "kubectl");
  }

  private static String chartMuseumPath = "chartmuseum";
  private static String ocPath = "oc";

  private static final String terraformConfigInspectBaseDir = "./client-tools/tf-config"
      + "-inspect";
  private static final String terraformConfigInspectBinary = "terraform-config-inspect";
  private static final String terraformConfigInspectCurrentVersion = "v1.0";
  private static final String terraformConfigInspectLatestVersion = "v1.1"; // This is not the
  // version provided by Hashicorp because currently they do not maintain releases as such
  private static final List<String> terraformConfigInspectVersions =
      Arrays.asList(terraformConfigInspectCurrentVersion, terraformConfigInspectLatestVersion);

  private static final String scmBaseDir = "./client-tools/scm/";
  private static final String scmBinary = "scm";
  private static final String defaultScmVersion = "0e23b6f1";

  private static final String KUBECTL_CDN_PATH = "public/shared/tools/kubectl/release/%s/bin/%s/amd64/kubectl";
  private static final String CHART_MUSEUM_CDN_PATH =
      "public/shared/tools/chartmuseum/release/%s/bin/%s/amd64/chartmuseum";
  private static final String GO_TEMPLATE_CDN_PATH =
      "public/shared/tools/go-template/release/%s/bin/%s/amd64/go-template";
  private static final String HARNESS_PYWINRM_CDN_PATH =
      "public/shared/tools/harness-pywinrm/release/%s/bin/%s/amd64/harness-pywinrm";
  private static final String OC_CDN_PATH = "public/shared/tools/oc/release/%s/bin/%s/amd64/oc";
  private static final String HELM_CDN_PATH = "public/shared/tools/helm/release/%s/bin/%s/amd64/helm";
  private static final String TERRAFORM_CONFIG_CDN_PATH =
      "public/shared/tools/terraform-config-inspect/%s/%s/amd64/terraform-config-inspect";
  private static final String KUSTOMIZE_CDN_PATH = "public/shared/tools/kustomize/release/%s/bin/%s/amd64/kustomize";
  private static final String CF_VERSION_COMMAND = "cf --version";
  private static final String SCM_CDN_PATH = "public/shared/tools/scm/release/%s/bin/%s/amd64/scm";

  public static String getTerraformConfigInspectPath(String version) {
    return join("/", terraformConfigInspectBaseDir, version, getOsPath(), "amd64", terraformConfigInspectBinary);
  }
  public static String getTerraformConfigInspectPath(boolean useLatestVersion) {
    if (useLatestVersion) {
      return join("/", terraformConfigInspectBaseDir, terraformConfigInspectLatestVersion, getOsPath(), "amd64",
          terraformConfigInspectBinary);
    } else {
      return join("/", terraformConfigInspectBaseDir, terraformConfigInspectCurrentVersion, getOsPath(), "amd64",
          terraformConfigInspectBinary);
    }
  }

  public static String getScmPath() {
    return join("/", getScmFolderPath(), getScmBinary());
  }

  public static String getScmBinary() {
    return scmBinary;
  }

  public static String getScmFolderPath() {
    String version = getScmVersion();
    return join("/", scmBaseDir, version, getOsPath(), "amd64");
  }

  public static String getDefaultKubectlPath() {
    return kubectlPaths.get(defaultKubectlVersion);
  }

  public static String getNewKubectlPath() {
    return kubectlPaths.get(newKubectlVersion);
  }

  public static String getGoTemplateToolPath() {
    return goTemplateToolPath;
  }

  public static String getHarnessPywinrmToolPath() {
    return harnessPywinrmToolPath;
  }

  public static String getHelm2Path() {
    return helmPaths.get(helm2Version);
  }

  public static String getHelm3Path() {
    return helmPaths.get(helm3Version);
  }

  public static String getChartMuseumPath(boolean useLastestVersion) {
    if (useLastestVersion) {
      return chartMuseumPaths.get(chartMuseumVersionNew);
    }
    return chartMuseumPaths.get(chartMuseumVersionOld);
  }

  public static String getOcPath() {
    return ocPath;
  }

  public static String getKustomizePath() {
    return kustomizePath;
  }

  public static String getKustomizePath(boolean useLatestVersion) {
    if (isCustomKustomizePath) {
      return kustomizePath;
    }
    return Paths
        .get(kustomizeBaseDir, useLatestVersion ? kustomizeVersionNew : kustomizeVersionOld, kustomizeBinaryName)
        .toAbsolutePath()
        .normalize()
        .toString();
  }

  public static boolean installKubectl(DelegateConfiguration configuration) {
    boolean kubectlInstalled = true;
    for (String version : kubectlVersions) {
      kubectlInstalled = kubectlInstalled && installKubectl(configuration, version);
    }
    return kubectlInstalled;
  }

  public static boolean installKubectl(DelegateConfiguration configuration, String kubectlVersion) {
    try {
      if (isNotEmpty(configuration.getKubectlPath())) {
        String kubectlPathFromConfig = configuration.getKubectlPath();
        kubectlPaths.put(kubectlVersion, kubectlPathFromConfig);
        log.info("Found user configured kubectl at {}. Skipping Install.", kubectlPathFromConfig);
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping kubectl install on Windows");
        return true;
      }

      String version = System.getenv().get("KUBECTL_VERSION");

      if (StringUtils.isEmpty(version)) {
        version = kubectlVersion;
        log.info("No version configured. Using kubectl version", version);
      }

      String kubectlDirectory = kubectlBaseDir + version;

      if (validateKubectlExists(kubectlDirectory)) {
        String kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        kubectlPaths.put(version, kubectlPath);
        log.info("kubectl version {} already installed", version);
        return true;
      }

      log.info("Installing kubectl");

      createDirectoryIfDoesNotExist(kubectlDirectory);

      String downloadUrl = getKubectlDownloadUrl(configuration, version);

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
        String kubectlPath = Paths.get(kubectlDirectory + "/kubectl").toAbsolutePath().normalize().toString();
        kubectlPaths.put(version, kubectlPath);
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
        log.info("Skipping go-template install on Windows");
        return true;
      }

      String goTemplateClientDirectory = goTemplateClientBaseDir + goTemplateClientVersion;

      if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
        goTemplateToolPath =
            Paths.get(goTemplateClientDirectory + "/go-template").toAbsolutePath().normalize().toString();
        log.info("go-template version {} already installed", goTemplateClientVersion);
        return true;
      }

      log.info("Installing go-template");

      createDirectoryIfDoesNotExist(goTemplateClientDirectory);

      String downloadUrl = getGoTemplateDownloadUrl(configuration, goTemplateClientVersion);

      log.info("download Url is {}", downloadUrl);

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
        log.info(result.outputUTF8());
        if (validateGoTemplateClientExists(goTemplateClientDirectory)) {
          log.info("go-template path: {}", goTemplateToolPath);
          return true;
        } else {
          log.error("go-template not validated after download: {}", goTemplateToolPath);
          return false;
        }
      } else {
        log.error("go-template install failed");
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing go-template", e);
      return false;
    }
  }

  public static boolean installHarnessPywinrm(DelegateConfiguration configuration) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping harness-pywinrm install on Windows");
        return true;
      }

      String harnessPywinrmClientDirectory = harnessPywinrmBaseDir + harnessPywinrmVersion;

      if (validateHarnessPywinrmExists(harnessPywinrmClientDirectory)) {
        harnessPywinrmToolPath =
            Paths.get(harnessPywinrmClientDirectory + "/harness-pywinrm").toAbsolutePath().normalize().toString();
        log.info("harness-pywinrm version {} already installed", harnessPywinrmVersion);
        return true;
      }

      log.info("Installing harness-pywinrm");

      createDirectoryIfDoesNotExist(harnessPywinrmClientDirectory);

      String downloadUrl = getHarnessPywinrmDownloadUrl(configuration, harnessPywinrmVersion);

      log.info("download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -kLO " + downloadUrl + "\n"
          + "chmod +x ./harness-pywinrm\n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(harnessPywinrmClientDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        harnessPywinrmToolPath =
            Paths.get(harnessPywinrmClientDirectory + "/harness-pywinrm").toAbsolutePath().normalize().toString();
        log.info(format("harness-pywinrm version: %s", result.outputUTF8()));
        if (validateHarnessPywinrmExists(harnessPywinrmClientDirectory)) {
          log.info("harness-pywinrm path: {}", harnessPywinrmToolPath);
          return true;
        } else {
          log.error("harness-pywinrm not validated after download: {}", harnessPywinrmToolPath);
          return false;
        }
      } else {
        log.error("harness-pywinrm install failed\n" + result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing harness-pywinrm", e);
      return false;
    }
  }

  private static boolean validateHarnessPywinrmExists(String harnessPywinrmClientDirectory) {
    try {
      if (!Files.exists(Paths.get(harnessPywinrmClientDirectory + "/harness-pywinrm"))) {
        return false;
      }

      String script = "./harness-pywinrm -v\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(harnessPywinrmClientDirectory))
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
      log.error("Error checking harness-winrm", e);
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
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error checking go-template", e);
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

  private static String getHarnessPywinrmDownloadUrl(DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join(
          "/", delegateConfiguration.getCdnUrl(), String.format(HARNESS_PYWINRM_CDN_PATH, version, getOsPath()));
    }

    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/snapshot-harness-pywinrm/release/" + version + "/bin/" + getOsPath()
        + "/amd64/harness-pywinrm";
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
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }

    } catch (Exception e) {
      log.error("Error checking helm", e);
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
      log.info("Init helm client only");

      String helmDirectory = helmBaseDir + helmVersion;
      String script = "./helm init -c --skip-refresh \n";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(helmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        log.info("Successfully init helm client");
        return true;
      } else {
        log.error("Helm client init failed");
        log.error(result.outputUTF8());
        return false;
      }
    } else {
      log.info("Init helm not needed for helm v3");
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
        log.info("Skipping helm install on Windows");
        return true;
      }

      String helmDirectory = helmBaseDir + helmVersion;
      if (validateHelmExists(helmDirectory)) {
        String helmPath = Paths.get(helmDirectory + "/helm").toAbsolutePath().normalize().toString();
        helmPaths.put(helmVersion, helmPath);
        log.info(format("helm version %s already installed", helmVersion));

        return initHelmClient(helmVersion);
      }

      log.info(format("Installing helm %s", helmVersion));
      createDirectoryIfDoesNotExist(helmDirectory);

      String downloadUrl = getHelmDownloadUrl(configuration, helmVersion);
      log.info("Download Url is " + downloadUrl);

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
        log.info(result.outputUTF8());

        if (validateHelmExists(helmDirectory)) {
          log.info("helm path: {}", helmPath);
          return true;
        } else {
          log.error("helm not validated after download: {}", helmPath);
          return false;
        }
      } else {
        log.error("helm install failed");
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing helm", e);
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
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }

    } catch (Exception e) {
      log.error("Error checking chart museum", e);
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
    boolean chartMuseumVersionsInstalled = true;
    for (String version : chartMuseumVersions) {
      chartMuseumVersionsInstalled = chartMuseumVersionsInstalled && installChartMuseum(configuration, version);
    }
    return chartMuseumVersionsInstalled;
  }
  public static boolean installChartMuseum(DelegateConfiguration configuration, String version) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping chart museum install on Windows");
        return true;
      }

      String chartMuseumDirectory = chartMuseumBaseDir + version;
      if (validateChartMuseumExists(chartMuseumDirectory)) {
        chartMuseumPath = Paths.get(chartMuseumDirectory + "/chartmuseum").toAbsolutePath().normalize().toString();
        chartMuseumPaths.put(version, chartMuseumPath);
        log.info("chartmuseum version %s already installed", version);
        return true;
      }

      log.info("Installing chartmuseum");
      createDirectoryIfDoesNotExist(chartMuseumDirectory);

      String downloadUrl = getChartMuseumDownloadUrl(configuration, version);
      log.info("Download Url is " + downloadUrl);

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
        chartMuseumPaths.put(version, chartMuseumPath);
        log.info(result.outputUTF8());

        if (validateChartMuseumExists(chartMuseumDirectory)) {
          log.info("chartmuseum path: {}", chartMuseumPath);
          return true;
        } else {
          log.error("chartmuseum not validated after download: {}", chartMuseumPath);
          return false;
        }
      } else {
        log.error("chart museum install failed");
        log.error(result.outputUTF8());
        return false;
      }

    } catch (Exception e) {
      log.error("Error installing chart museum", e);
      return false;
    }
  }

  public static boolean installTerraformConfigInspect(DelegateConfiguration configuration) {
    boolean terraformConfigInspectInstalled = true;
    for (String version : terraformConfigInspectVersions) {
      terraformConfigInspectInstalled =
          terraformConfigInspectInstalled && installTerraformConfigInspect(configuration, version);
    }
    return terraformConfigInspectInstalled;
  }

  public static boolean installTerraformConfigInspect(DelegateConfiguration configuration, String version) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        log.info(format("Skipping terraform-config-inspect version %s install on Windows", version));
        return true;
      }

      final String terraformConfigInspectVersionedDirectory =
          Paths.get(getTerraformConfigInspectPath(version)).getParent().toString();
      if (validateTerraformConfigInspectExists(terraformConfigInspectVersionedDirectory, version)) {
        log.info(format("terraform-config-inspect version %s already installed at {}", version),
            terraformConfigInspectVersionedDirectory);
        return true;
      }

      log.info(format("Installing terraform-config-inspect version %s", version));
      createDirectoryIfDoesNotExist(terraformConfigInspectVersionedDirectory);

      String downloadUrl = getTerraformConfigInspectDownloadUrl(configuration, version);
      log.info("Download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./terraform-config-inspect";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(terraformConfigInspectVersionedDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        String tfConfigInspectPath = Paths.get(getTerraformConfigInspectPath(version)).toAbsolutePath().toString();
        log.info(format("terraform config inspect version %s installed at {}", version), tfConfigInspectPath);
        return true;
      } else {
        log.error(format("Error installing Terraform Config Inspect version %s", version));
        return false;
      }

    } catch (Exception ex) {
      log.error(format("Error installing Terraform Config Inspect version %s", version), ex);
      return false;
    }
  }

  @VisibleForTesting
  protected static String getTerraformConfigInspectDownloadUrl(
      DelegateConfiguration delegateConfiguration, String version) {
    if (delegateConfiguration.isUseCdn()) {
      return join(
          "/", delegateConfiguration.getCdnUrl(), String.format(TERRAFORM_CONFIG_CDN_PATH, version, getOsPath()));
    }
    return getManagerBaseUrl(delegateConfiguration.getManagerUrl())
        + "storage/harness-download/harness-terraform-config-inspect/" + version + "/" + getOsPath() + "/amd64/"
        + terraformConfigInspectBinary;
  }

  private static boolean validateTerraformConfigInspectExists(
      String terraformConfigInspectVersionedDirectory, String version) {
    try {
      Path path = Paths.get(join("/", terraformConfigInspectVersionedDirectory, terraformConfigInspectBinary));
      if (!path.toFile().exists()) {
        return false;
      }
      String terraformConfigInspectBinary = "./terraform-config-inspect";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(terraformConfigInspectVersionedDirectory))
                                            .command("/bin/bash", "-c", terraformConfigInspectBinary)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      return result.getExitValue() == 0;

    } catch (Exception e) {
      log.error(format("Error checking Terraform Config Inspect version %s", version), e);
      return false;
    }
  }

  public static boolean installScm(DelegateConfiguration configuration) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping scm install on Windows");
        return true;
      }

      final String scmVersionedDirectory = Paths.get(getScmPath()).getParent().toString();
      if (validateScmExists(scmVersionedDirectory)) {
        log.info("scm already installed at {}", scmVersionedDirectory);
        return true;
      }

      log.info("Installing scm");
      createDirectoryIfDoesNotExist(scmVersionedDirectory);

      String downloadUrl = getScmDownloadUrl(configuration);
      log.info("Download Url is {}", downloadUrl);

      String script = "curl $MANAGER_PROXY_CURL -LO " + downloadUrl + "\n"
          + "chmod +x ./scm";

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .directory(new File(scmVersionedDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();
      if (result.getExitValue() == 0) {
        String scmPath = Paths.get(getScmPath()).toAbsolutePath().toString();
        log.info(result.outputUTF8());
        if (validateScmExists(scmVersionedDirectory)) {
          log.info("scm path: {}", scmPath);
          return true;
        } else {
          log.error("scm not validated after download: {}", scmPath);
          return false;
        }
      } else {
        log.error("scm install failed");
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing scm", e);
      return false;
    }
  }

  public static String getScmVersion() {
    String version = System.getenv().get("SCM_VERSION");
    if (StringUtils.isEmpty(version)) {
      version = defaultScmVersion;
      log.info("No version configured. Using default scm version {}", version);
    }
    return version;
  }

  private static boolean validateScmExists(String scmDirectory) {
    try {
      Path path = Paths.get(scmDirectory, scmBinary);
      if (!path.toFile().exists()) {
        return false;
      }

      String script = "./scm --version\n";
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(1, TimeUnit.MINUTES)
                                            .directory(new File(scmDirectory))
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true);
      ProcessResult result = processExecutor.execute();

      if (result.getExitValue() == 0) {
        log.info(result.outputString());
        return true;
      } else {
        log.error(result.outputString());
        return false;
      }
    } catch (Exception e) {
      log.error("Error checking scm", e);
      return false;
    }
  }

  @VisibleForTesting
  protected static String getScmDownloadUrl(DelegateConfiguration delegateConfiguration) {
    String scmVersion = getScmVersion();
    if (delegateConfiguration.isUseCdn()) {
      return join("/", delegateConfiguration.getCdnUrl(), String.format(SCM_CDN_PATH, scmVersion, getOsPath()));
    }
    return getManagerBaseUrl(delegateConfiguration.getManagerUrl()) + "storage/harness-download/harness-scm/release/"
        + scmVersion + "/bin/" + getOsPath() + "/amd64/" + scmBinary;
  }

  public static boolean installOc(DelegateConfiguration configuration) {
    try {
      if (StringUtils.isNotEmpty(configuration.getOcPath())) {
        ocPath = configuration.getOcPath();
        log.info("Found user configured oc at {}. Skipping Install.", ocPath);
        return true;
      }

      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping oc install on Windows");
        return true;
      }

      String version = System.getenv().get("OC_VERSION");
      if (StringUtils.isEmpty(version)) {
        version = ocVersion;
        log.info("No version configured. Using default oc version {}", version);
      }

      String ocDirectory = ocBaseDir + version;
      if (validateOcExists(ocDirectory)) {
        ocPath = Paths.get(ocDirectory, "oc").toAbsolutePath().normalize().toString();
        log.info("oc version {} already installed", version);
        return true;
      }

      log.info("Installing oc");
      createDirectoryIfDoesNotExist(ocDirectory);

      String downloadUrl = getOcDownloadUrl(configuration, version);
      log.info("download url is {}", downloadUrl);

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
        log.info(result.outputUTF8());
        if (validateOcExists(ocDirectory)) {
          log.info("oc path: {}", ocPath);
          return true;
        } else {
          log.error("oc not validated after download: {}", ocPath);
          return false;
        }
      } else {
        log.error("oc install failed");
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing oc", e);
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
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error checking oc", e);
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
    boolean kustomizeInstalled = true;
    for (String version : kustomizeVersions) {
      kustomizeInstalled = kustomizeInstalled && installKustomize(configuration, version);
    }
    return kustomizeInstalled;
  }

  public static boolean installKustomize(DelegateConfiguration configuration, String kustomizeVersion) {
    try {
      if (StringUtils.isNotEmpty(configuration.getKustomizePath())) {
        isCustomKustomizePath = true;
        kustomizePath = configuration.getKustomizePath();
        log.info("Found user configured kustomize at {}. Skipping Install.", kustomizePath);
        return true;
      }
      isCustomKustomizePath = false;
      if (SystemUtils.IS_OS_WINDOWS) {
        log.info("Skipping kustomize install on Windows");
        return true;
      }

      String kustomizeDir = kustomizeBaseDir + kustomizeVersion;

      if (validateKustomizeExists(kustomizeDir)) {
        kustomizePath = Paths.get(kustomizeDir + "/kustomize").toAbsolutePath().normalize().toString();
        log.info("kustomize version {} already installed", kustomizeVersion);
        return true;
      }

      log.info("Installing kustomize");

      createDirectoryIfDoesNotExist(kustomizeDir);

      String downloadUrl = getKustomizeDownloadUrl(configuration, kustomizeVersion);

      log.info("download Url is {}", downloadUrl);

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
        log.info(result.outputUTF8());
        if (validateKustomizeExists(kustomizeDir)) {
          log.info("kustomize path: {}", kustomizePath);
          return true;
        } else {
          log.error("kustomize not validated after download: {}", kustomizePath);
          return false;
        }
      } else {
        log.error("kustomize version {} install failed", kustomizeVersion);
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error installing kustomize", e);
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
        log.info(result.outputUTF8());
        return true;
      } else {
        log.error(result.outputUTF8());
        return false;
      }
    } catch (Exception e) {
      log.error("Error checking kustomize", e);
      return false;
    }
  }

  @VisibleForTesting
  static boolean delegateConfigHasHelmPath(DelegateConfiguration configuration, String helmVersion) {
    if (helm2Version.equals(helmVersion)) {
      if (isNotEmpty(configuration.getHelmPath())) {
        String helmPath = configuration.getHelmPath();
        helmPaths.put(helmVersion, helmPath);
        log.info("Found user configured helm2 at {}. Skipping Install.", helmPath);
        return true;
      }
    } else if (helm3Version.equals(helmVersion)) {
      if (isNotEmpty(configuration.getHelm3Path())) {
        String helmPath = configuration.getHelm3Path();
        helmPaths.put(helmVersion, helmPath);
        log.info("Found user configured helm3 at {}. Skipping Install.", helmPath);
        return true;
      }
    }
    return false;
  }

  public static void validateCfCliExists() {
    ProcessResult processResult = executeCommand(CF_VERSION_COMMAND, 1);
    if (processResult.getExitValue() == 0) {
      log.info(format("Found CF CLI installed by package manager : %s", processResult.outputUTF8()));
    }
  }

  private static ProcessResult executeCommand(final String cmd, long timeoutInMin) {
    try {
      return new ProcessExecutor()
          .timeout(timeoutInMin, TimeUnit.MINUTES)
          .command("/bin/bash", "-c", cmd)
          .readOutput(true)
          .redirectOutput(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              log.info(line);
            }
          })
          .redirectError(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              log.error(line);
            }
          })
          .execute();
    } catch (Exception ex) {
      throw new ProcessExecutionException(format("Unable to execute bash command: %s", cmd), ex);
    }
  }
}

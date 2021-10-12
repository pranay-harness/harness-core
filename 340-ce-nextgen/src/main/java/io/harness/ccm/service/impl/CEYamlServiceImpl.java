package io.harness.ccm.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.ccm.commons.beans.config.CEFeatures;
import io.harness.ccm.remote.beans.K8sClusterSetupRequest;
import io.harness.ccm.service.intf.CEYamlService;
import io.harness.delegate.beans.connector.k8Connector.K8sServiceAccountInfoResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class CEYamlServiceImpl implements CEYamlService {
  private static final Configuration templateConfiguration = new Configuration(VERSION_2_3_23);
  private static final String YAML_FTL = ".yaml.ftl";

  @Inject private K8sServiceAccountDelegateTaskClient k8sTaskClient;

  static {
    templateConfiguration.setTemplateLoader(new ClassTemplateLoader(CEYamlServiceImpl.class, "/yamltemplates"));
  }

  @Override
  @Deprecated
  public File downloadCostOptimisationYaml(
      String accountId, String connectorIdentifier, String harnessHost, String serverName) throws IOException {
    final String costOptimisationFileName = "cost-optimisation-crd";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("accountId", accountId)
                                                    .put("connectorIdentifier", connectorIdentifier)
                                                    .put("envoyHarnessHostname", serverName)
                                                    .put("harnessHostname", harnessHost)
                                                    .build();

    File yaml = File.createTempFile(costOptimisationFileName, DOT_YAML);

    saveProcessedTemplate(scriptParams, yaml, costOptimisationFileName + YAML_FTL);
    return new File(yaml.getAbsolutePath());
  }

  @Override
  @Deprecated
  public String unifiedCloudCostK8sClusterYaml(@NonNull String accountId, String harnessHost, String serverName,
      @NonNull K8sClusterSetupRequest request) throws IOException {
    String yamlFileContent = "";

    K8sServiceAccountInfoResponse serviceAccount;
    try {
      serviceAccount = k8sTaskClient.fetchServiceAccount(
          request.getConnectorIdentifier(), accountId, request.getOrgIdentifier(), request.getProjectIdentifier());

      log.info(
          "serviceAccount associated with accountId:{}, connectorIdentifier:{}, orgIdentifier:{}, projectIdentifier:{} is {}",
          accountId, request.getConnectorIdentifier(), request.getOrgIdentifier(), request.getProjectIdentifier(),
          serviceAccount);
    } catch (Exception ex) {
      log.error("Failed delegate task K8S_SERVICE_ACCOUNT_INFO", ex);
      throw ex;
    }

    if (request.getFeaturesEnabled().contains(CEFeatures.VISIBILITY)) {
      yamlFileContent = getK8sVisibilityYaml(serviceAccount);
    }

    if (request.getFeaturesEnabled().contains(CEFeatures.OPTIMIZATION)) {
      yamlFileContent += getK8sOptimisationYaml(
          accountId, request.getCcmConnectorIdentifier(), harnessHost, serverName, serviceAccount);
    }

    return yamlFileContent;
  }

  @Override
  public String unifiedCloudCostK8sClusterYaml(@NonNull String accountId, String harnessHost, String serverName,
      @NonNull K8sClusterSetupRequest request, boolean includeVisibility, boolean includeOptimization)
      throws IOException {
    if (!includeOptimization && !includeVisibility) {
      throw new InvalidArgumentsException(
          "Nothing to generate when includeOptimization=false and includeVisibility=false");
    }

    String yamlFileContent = "";

    K8sServiceAccountInfoResponse serviceAccount;
    try {
      serviceAccount = k8sTaskClient.fetchServiceAccount(
          request.getConnectorIdentifier(), accountId, request.getOrgIdentifier(), request.getProjectIdentifier());

      log.info(
          "serviceAccount associated with accountId:{}, connectorIdentifier:{}, orgIdentifier:{}, projectIdentifier:{} is {}",
          accountId, request.getConnectorIdentifier(), request.getOrgIdentifier(), request.getProjectIdentifier(),
          serviceAccount);
    } catch (Exception ex) {
      log.error("Failed delegate task K8S_SERVICE_ACCOUNT_INFO", ex);
      throw ex;
    }

    if (includeVisibility) {
      yamlFileContent = getK8sVisibilityYaml(serviceAccount);
    } else if (includeOptimization) {
      yamlFileContent = getClusterroleYaml(serviceAccount);

      yamlFileContent += getK8sOptimisationYaml(
          accountId, request.getCcmConnectorIdentifier(), harnessHost, serverName, serviceAccount);
    }

    return yamlFileContent;
  }

  private String getK8sOptimisationYaml(@NonNull String accountId, @NonNull String ccmConnectorIdentifier,
      @NonNull String harnessHost, @NonNull String serverName, K8sServiceAccountInfoResponse serviceAccount)
      throws IOException {
    final String costOptimisationFileName = "cost-optimisation-crd";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("accountId", accountId)
                                                    .put("connectorIdentifier", ccmConnectorIdentifier)
                                                    .put("envoyHarnessHostname", serverName)
                                                    .put("harnessHostname", harnessHost)
                                                    .put("serviceAccountName", serviceAccount.getName())
                                                    .put("serviceAccountNamespace", serviceAccount.getNamespace())
                                                    .build();

    return getProcessedYaml(costOptimisationFileName, scriptParams);
  }

  private String getK8sVisibilityYaml(K8sServiceAccountInfoResponse serviceAccount) throws IOException {
    final String visibilityYamlFileName = "k8s-visibility-clusterrole";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("serviceAccountName", serviceAccount.getName())
                                                    .put("serviceAccountNamespace", serviceAccount.getNamespace())
                                                    .build();

    return getProcessedYaml(visibilityYamlFileName, scriptParams);
  }

  private String getClusterroleYaml(K8sServiceAccountInfoResponse serviceAccount) throws IOException {
    final String visibilityYamlFileName = "ce-clusterrole";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("serviceAccountName", serviceAccount.getName())
                                                    .put("serviceAccountNamespace", serviceAccount.getNamespace())
                                                    .build();

    return getProcessedYaml(visibilityYamlFileName, scriptParams);
  }

  private String getProcessedYaml(@NonNull String yamlFileName, @NonNull ImmutableMap<String, String> params)
      throws IOException {
    File file = File.createTempFile(yamlFileName, DOT_YAML);
    saveProcessedTemplate(params, file, yamlFileName + YAML_FTL);

    final String yamlFileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    file.delete();
    return yamlFileContent;
  }

  private void saveProcessedTemplate(Map<String, String> params, File start, String template) throws IOException {
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start), UTF_8)) {
      templateConfiguration.getTemplate(template).process(params, fileWriter);
    } catch (TemplateException ex) {
      throw new UnexpectedException("This templates are included in the jar, they should be safe to process", ex);
    }
  }
}

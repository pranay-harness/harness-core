package software.wings.service.impl.yaml.service;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rktummala on 10/17/17
 */
@Singleton
public class YamlHelper {
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;
  @Inject SettingsService settingsService;

  public SettingAttribute getCloudProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.CLOUD_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getArtifactServer(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.ARTIFACT_SERVER, yamlFilePath);
  }

  public SettingAttribute getCollaborationProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.COLLABORATION_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getVerificationProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.VERIFICATION_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getLoadBalancerProvider(String accountId, String yamlFilePath) {
    return getSettingAttribute(accountId, YamlType.LOADBALANCER_PROVIDER, yamlFilePath);
  }

  public SettingAttribute getSettingAttribute(String accountId, YamlType yamlType, String yamlFilePath) {
    String settingAttributeName =
        extractEntityNameFromYamlPath(yamlType.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Setting Attribute name null in the given yaml file: " + yamlFilePath, settingAttributeName);
    return settingsService.getSettingAttributeByName(accountId, settingAttributeName);
  }

  public String getAppId(String accountId, String yamlFilePath) {
    Application app = getApp(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, app);
    return app.getUuid();
  }

  public Application getApp(String accountId, String yamlFilePath) {
    String appName = extractParentEntityName(YamlType.APPLICATION.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);

    Validator.notNullCheck("App name null in the given yaml file: " + yamlFilePath, appName);
    return appService.getAppByName(accountId, appName);
  }

  public String getAppName(String yamlFilePath) {
    return extractParentEntityName(YamlType.APPLICATION.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public String getServiceId(String appId, String yamlFilePath) {
    Service service = getService(appId, yamlFilePath);
    Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, service);
    return service.getUuid();
  }

  public Service getService(String appId, String yamlFilePath) {
    String serviceName = extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Service name null in the given yaml file: " + yamlFilePath, serviceName);
    return serviceResourceService.getServiceByName(appId, serviceName);
  }

  public String getServiceName(String yamlFilePath) {
    return extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public String getInfrastructureProvisionerId(String appId, String yamlFilePath) {
    InfrastructureProvisioner provisioner = getInfrastructureProvisioner(appId, yamlFilePath);
    Validator.notNullCheck("InfrastructureProvisioner null in the given yaml file: " + yamlFilePath, provisioner);
    return provisioner.getUuid();
  }

  public InfrastructureProvisioner getInfrastructureProvisioner(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String provisionerName = getNameFromYamlFilePath(yamlFilePath);
    Validator.notNullCheck(
        "InfrastructureProvisioner name null in the given yaml file: " + yamlFilePath, provisionerName);
    return infrastructureProvisionerService.getByName(appId, provisionerName);
  }

  public String getEnvironmentId(String appId, String yamlFilePath) {
    Environment environment = getEnvironment(appId, yamlFilePath);
    Validator.notNullCheck("Environment null in the given yaml file: " + yamlFilePath, environment);
    return environment.getUuid();
  }

  public Environment getEnvironment(String appId, String yamlFilePath) {
    String envName = extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Environment name null in the given yaml file: " + yamlFilePath, envName);
    return environmentService.getEnvironmentByName(appId, envName);
  }

  public Environment getEnvironmentFromAccount(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String envName = extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Environment name null in the given yaml file: " + yamlFilePath, envName);
    return environmentService.getEnvironmentByName(appId, envName);
  }

  public String getEnvironmentName(String yamlFilePath) {
    return extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
  }

  public ArtifactStream getArtifactStream(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String serviceId = getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Service null in the given yaml file: " + yamlFilePath, serviceId);
    String artifactStreamName =
        extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
    return artifactStreamService.getArtifactStreamByName(appId, serviceId, artifactStreamName);
  }

  public Workflow getWorkflow(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String workflowName =
        extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Workflow name null in the given yaml file: " + yamlFilePath, workflowName);
    return workflowService.readWorkflowByName(appId, workflowName);
  }

  public Pipeline getPipeline(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String pipelineName =
        extractEntityNameFromYamlPath(YamlType.PIPELINE.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Pipeline name null in the given yaml file: " + yamlFilePath, pipelineName);
    return pipelineService.getPipelineByName(appId, pipelineName);
  }

  public InfrastructureMapping getInfraMapping(String accountId, String yamlFilePath) {
    String appId = getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("App null in the given yaml file: " + yamlFilePath, appId);
    String envId = getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Env null in the given yaml file: " + yamlFilePath, envId);
    String infraMappingName =
        extractEntityNameFromYamlPath(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Inframapping with name: " + infraMappingName, infraMappingName);
    return infraMappingService.getInfraMappingByName(appId, envId, infraMappingName);
  }

  private String extractParentEntityName(String regex, String yamlFilePath, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/*/Services/*/
    // yamlFilePath - Setup/Applications/App1/Services/service1/Commands/command1
    if (matcher.find()) {
      // first extract the value that matches the pattern
      // extractedValue - Setup/Applications/App1/Services/service1/
      String extractedValue = matcher.group();

      StringBuilder stringBuilder = new StringBuilder(extractedValue);
      // strip off the last character, which would be the delimiter
      // stringBuilder - Setup/Applications/App1/Services/service1
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
      // Get the sub string between the last delimiter and the end of the string
      // result - service1
      return stringBuilder.substring(stringBuilder.lastIndexOf(delimiter) + 1, stringBuilder.length());
    }
    return null;
  }

  public String extractEntityNameFromYamlPath(String regex, String yamlFilePath, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/.[^/]*/Services/.[^/]*/.[^/]*?.yaml
    // yamlFilePath - Setup/Applications/App1/Services/service1/service1.yaml
    if (matcher.find()) {
      // first extract the value that matches the pattern
      // extractedValue = service1.yaml
      String extractedValue = matcher.group();

      StringBuilder stringBuilder = new StringBuilder(extractedValue);
      // Get the sub string between the last delimiter and the .yaml suffix at the end
      // result - service1
      return stringBuilder.substring(stringBuilder.lastIndexOf(delimiter) + 1, stringBuilder.length() - 5);
    }
    return null;
  }

  /**
   * This works for all yaml except App, Service and Environment. The yamls for those entities end with Index.yaml since
   * those entities are folder entries. In other words, you could use this method to extract name from any leaf entity.
   * @param yamlFilePath yaml file path
   * @return
   */
  public String getNameFromYamlFilePath(String yamlFilePath) {
    return extractEntityNameFromYamlPath(YamlConstants.YAML_FILE_NAME_PATTERN, yamlFilePath, PATH_DELIMITER);
  }

  public Optional<Application> getApplicationIfPresent(String accountId, String yamlFilePath) {
    Application application = getApp(accountId, yamlFilePath);
    if (application == null) {
      return Optional.empty();
    } else {
      return Optional.of(application);
    }
  }

  public Optional<Service> getServiceIfPresent(String applicationId, String yamlFilePath) {
    Service service = getService(applicationId, yamlFilePath);
    if (service != null) {
      return Optional.of(service);
    }
    return Optional.empty();
  }

  public Optional<Environment> getEnvIfPresent(String applicationId, String yamlFilePath) {
    Environment environment = getEnvironment(applicationId, yamlFilePath);
    if (environment != null) {
      return Optional.of(environment);
    }
    return Optional.empty();
  }

  public InfrastructureMapping getInfraMappingByAppIdYamlPath(String applicationId, String envId, String yamlFilePath) {
    String infraMappingName =
        extractEntityNameFromYamlPath(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Inframapping with name: " + infraMappingName, infraMappingName);
    return infraMappingService.getInfraMappingByName(applicationId, envId, infraMappingName);
  }

  public InfrastructureProvisioner getInfrastructureProvisionerByAppIdYamlPath(
      String applicationId, String yamlFilePath) {
    String provisionerName = getNameFromYamlFilePath(yamlFilePath);
    Validator.notNullCheck(
        "InfrastructureProvisioner name null in the given yaml file: " + yamlFilePath, provisionerName);
    return infrastructureProvisionerService.getByName(applicationId, provisionerName);
  }

  public Pipeline getPipelineByAppIdYamlPath(String applicationId, String yamlFilePath) {
    String pipelineName =
        extractEntityNameFromYamlPath(YamlType.PIPELINE.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Pipeline name null in the given yaml file: " + yamlFilePath, pipelineName);
    return pipelineService.getPipelineByName(applicationId, pipelineName);
  }

  public Workflow getWorkflowByAppIdYamlPath(String applicationId, String yamlFilePath) {
    String workflowName =
        extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Workflow name null in the given yaml file: " + yamlFilePath, workflowName);
    return workflowService.readWorkflowByName(applicationId, workflowName);
  }

  public ArtifactStream getArtifactStream(String applicationId, String serviceId, String yamlFilePath) {
    String artifactStreamName =
        extractEntityNameFromYamlPath(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Artifact stream name null in the given yaml file: " + yamlFilePath, artifactStreamName);
    return artifactStreamService.getArtifactStreamByName(applicationId, serviceId, artifactStreamName);
  }
}

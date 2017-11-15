package software.wings.service.impl.yaml.sync;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.YamlType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rktummala on 10/17/17
 */
public class YamlSyncHelper {
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;

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
    Validator.notNullCheck("Env null in the given yaml file: " + yamlFilePath, appId);
    String infraMappingName =
        extractEntityNameFromYamlPath(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("Env null in the given yaml file: " + yamlFilePath, appId);
    return infraMappingService.getInfraMappingByName(appId, envId, infraMappingName);
  }

  private String extractParentEntityName(String regex, String applyOn, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(applyOn);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/*/Services/*/
    // applyOn - Setup/Applications/App1/Services/service1/Commands/command1
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

  public String extractEntityNameFromYamlPath(String regex, String applyOn, String delimiter) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(applyOn);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/.[^/]*/Services/.[^/]*/.[^/]*?.yaml
    // applyOn - Setup/Applications/App1/Services/service1/service1.yaml
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
}

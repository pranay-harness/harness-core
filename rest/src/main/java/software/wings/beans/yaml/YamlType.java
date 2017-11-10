package software.wings.beans.yaml;

import static software.wings.beans.yaml.YamlConstants.ANY;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COMMANDS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONFIG_FILES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONNECTORS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SERVERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INFRA_MAPPING_FOLDER;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.PIPELINES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXPRESSION;
import static software.wings.utils.Util.generatePath;

import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NameValuePair;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;

/**
 * @author rktummala on 10/17/17
 */
public enum YamlType {
  CLOUD_PROVIDER(Category.CLOUD_PROVIDER.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CLOUD_PROVIDERS_FOLDER, ANY), SettingAttribute.class),
  ARTIFACT_SERVER(YamlConstants.ARTIFACT_SERVER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CONNECTORS_FOLDER, ARTIFACT_SERVERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CONNECTORS_FOLDER, ARTIFACT_SERVERS_FOLDER, ANY),
      SettingAttribute.class),
  COLLABORATION_PROVIDER(YamlConstants.COLLABORATION_PROVIDER,
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, CONNECTORS_FOLDER, COLLABORATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CONNECTORS_FOLDER, COLLABORATION_PROVIDERS_FOLDER, ANY),
      SettingAttribute.class),
  LOADBALANCER_PROVIDER(YamlConstants.LOADBALANCER_PROVIDER,
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, CONNECTORS_FOLDER, LOAD_BALANCERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CONNECTORS_FOLDER, LOAD_BALANCERS_FOLDER, ANY),
      SettingAttribute.class),
  VERIFICATION_PROVIDER(YamlConstants.VERIFICATION_PROVIDER,
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, CONNECTORS_FOLDER, VERIFICATION_PROVIDERS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, CONNECTORS_FOLDER, VERIFICATION_PROVIDERS_FOLDER, ANY),
      SettingAttribute.class),
  APPLICATION(EntityType.APPLICATION.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY), Application.class),
  SERVICE(EntityType.SERVICE.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY), Service.class),
  ARTIFACT_SOURCE(EntityType.ARTIFACT_STREAM.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          ARTIFACT_SOURCES_FOLDER, ANY),
      ArtifactStream.class),
  COMMAND(EntityType.COMMAND.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER,
          YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, COMMANDS_FOLDER, ANY),
      Command.class),
  CONFIG_FILE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(
          PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, SERVICES_FOLDER, ANY, CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  ENVIRONMENT(EntityType.ENVIRONMENT.name(),
      generatePath(
          PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY),
      Environment.class),
  CONFIG_FILE_OVERRIDE(EntityType.CONFIG.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          CONFIG_FILES_FOLDER, ANY),
      ConfigFile.class),
  INFRA_MAPPING(EntityType.INFRASTRUCTURE_MAPPING.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, ENVIRONMENTS_FOLDER, ANY,
          INFRA_MAPPING_FOLDER, ANY),
      InfrastructureMapping.class),
  WORKFLOW(EntityType.WORKFLOW.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, WORKFLOWS_FOLDER, ANY),
      Workflow.class),
  PIPELINE(EntityType.PIPELINE.name(),
      generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, YAML_EXPRESSION),
      generatePath(PATH_DELIMITER, true, SETUP_FOLDER, APPLICATIONS_FOLDER, ANY, PIPELINES_FOLDER, ANY),
      Pipeline.class),

  // Some of these bean classes are embedded within other entities and don't have an yaml file path
  NAME_VALUE_PAIR(ObjectType.NAME_VALUE_PAIR, "", "", NameValuePair.class),
  PHASE(ObjectType.PHASE, "", "", WorkflowPhase.class),
  PHASE_STEP(ObjectType.PHASE_STEP, "", "", PhaseStep.class),
  TEMPLATE_EXPRESSION(ObjectType.TEMPLATE_EXPRESSION, "", "", TemplateExpression.class),
  VARIABLE(ObjectType.VARIABLE, "", "", Variable.class),
  STEP(ObjectType.STEP, "", "", Node.class),
  FAILURE_STRATEGY(ObjectType.FAILURE_STRATEGY, "", "", FailureStrategy.class),
  NOTIFICATION_RULE(ObjectType.NOTIFICATION_RULE, "", "", NotificationRule.class),
  PIPELINE_STAGE(ObjectType.PIPELINE_STAGE, "", "", PipelineStage.class),
  // TODO this should have a path, when this yaml is migrated to the new model, make sure this is taken care of
  NOTIFICATION_GROUP(ObjectType.NOTIFICATION_GROUP, "", "", NotificationGroup.class);

  private String entityType;
  private String pathExpression;
  private String prefixExpression;
  private Class beanClass;

  YamlType(String entityType, String pathExpression, String prefixExpression, Class beanClass) {
    this.entityType = entityType;
    this.pathExpression = pathExpression;
    this.prefixExpression = prefixExpression;
    this.beanClass = beanClass;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getPathExpression() {
    return pathExpression;
  }

  public String getPrefixExpression() {
    return prefixExpression;
  }

  public Class getBeanClass() {
    return beanClass;
  }
}

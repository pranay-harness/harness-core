package software.wings.beans.yaml;

/**
 * @author rktummala on 10/17/17
 */
public interface YamlConstants {
  String PATH_DELIMITER = "/";
  String ANY = ".[^/]*?";
  //  String SETUP_ENTITY_ID = "setup";
  String SETUP_FOLDER = "Setup";
  String APPLICATIONS_FOLDER = "Applications";
  String SERVICES_FOLDER = "Services";
  String COMMANDS_FOLDER = "Commands";
  String CONFIG_FILES_FOLDER = "Config Files";
  String ENVIRONMENTS_FOLDER = "Environments";
  String INFRA_MAPPING_FOLDER = "Service Infrastructure";
  String WORKFLOWS_FOLDER = "Workflows";
  String PIPELINES_FOLDER = "Pipelines";
  String TRIGGERS_FOLDER = "Triggers";

  String CLOUD_PROVIDERS_FOLDER = "Cloud Providers";
  String CONNECTORS_FOLDER = "Connectors";
  String ARTIFACT_SERVERS_FOLDER = "Artifact Servers";
  String COLLABORATION_PROVIDERS_FOLDER = "Collaboration Providers";
  String LOAD_BALANCER_PROVIDERS_FOLDER = "Load Balancer Providers";
  String VERIFICATION_PROVIDERS_FOLDER = "Verification Providers";
  String AWS_FOLDER = "Amazon Web Services";
  String GCP_FOLDER = "Google Cloud Platform";
  String PHYSICAL_DATA_CENTER_FOLDER = "Physical Data Centers";

  String ARTIFACT_SOURCES_FOLDER = "Artifact Servers";
  String DEPLOYMENT_SPECIFICATION_FOLDER = "Deployment Specifications";

  String SMTP_FOLDER = "SMTP";
  String SLACK_FOLDER = "Slack";

  String LOAD_BALANCERS_FOLDER = "Load Balancers";
  String ELB_FOLDER = "Elastic Classic Load Balancers";

  String JENKINS_FOLDER = "Jenkins";
  String APP_DYNAMICS_FOLDER = "AppDynamics";
  String SPLUNK_FOLDER = "Splunk";
  String ELK_FOLDER = "ELK";
  String LOGZ_FOLDER = "LOGZ";

  String YAML_EXPRESSION = ".[^/]*?\\.yaml";
  String YAML_EXTENSION = ".yaml";
  String ARTIFACT_SERVER = "ARTIFACT_SERVER";
  String COLLABORATION_PROVIDER = "COLLABORATION_PROVIDER";
  String LOADBALANCER_PROVIDER = "LOADBALANCER_PROVIDER";
  String VERIFICATION_PROVIDER = "VERIFICATION_PROVIDER";

  String DEPLOYMENT_SPECIFICATION = "DEPLOYMENT_SPECIFICATION";

  String LAMBDA_SPEC_YAML_FILE_NAME = "Lambda";
  String ECS_CONTAINER_TASK_YAML_FILE_NAME = "Ecs";
  String KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME = "Kubernetes";

  String LINK_PREFIX = "link";
  String NODE_PREFIX = "node";

  int DEFAULT_COORDINATE = 50;
  int COORDINATE_INCREMENT_BY = 150;

  String NODE_PROPERTY_REFERENCEID = "referenceId";
  String NODE_PROPERTY_COMMAND_STRING = "commandString";
  String NODE_PROPERTY_TAIL_FILES = "tailFiles";
  String NODE_PROPERTY_TAIL_PATTERNS = "tailPatterns";
  String NODE_PROPERTY_COMMAND_TYPE = "commandType";
  String NODE_PROPERTY_COMMAND_PATH = "commandPath";
  String NODE_PROPERTY_FILE_CATEGORY = "fileCategory";
  String NODE_PROPERTY_DESTINATION_DIR_PATH = "destinationDirectoryPath";
  String NODE_PROPERTY_DESTINATION_PARENT_PATH = "destinationParentPath";
}

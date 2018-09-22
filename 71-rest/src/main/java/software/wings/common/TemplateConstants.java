package software.wings.common;

public interface TemplateConstants {
  String LATEST_TAG = "latest";
  String PATH_DELIMITER = "/";

  /**
   * Gallery constants
   */
  String HARNESS_GALLERY = "Harness";

  /**
   * Constants for templates
   */
  String COMMAND_PATH = "templategalleries/harness/command/";
  String TOMCAT_WAR_STOP_PATH = COMMAND_PATH + "tomcat/WarStop.yaml";
  String TOMCAT_WAR_START_PATH = COMMAND_PATH + "tomcat/WarStart.yaml";
  String TOMCAT_WAR_INSTALL_PATH = COMMAND_PATH + "tomcat/WarInstall.yaml";
  String JBOSS_WAR_STOP_PATH = COMMAND_PATH + "jboss/WarStop.yaml";
  String JBOSS_WAR_START_PATH = COMMAND_PATH + "jboss/WarStart.yaml";
  String JBOSS_WAR_INSTALL_PATH = COMMAND_PATH + "jboss/WarInstall.yaml";
  String POWER_SHELL_IIS_INSTALL_PATH = COMMAND_PATH + "powershell/Install.yaml";
  String GENERIC_JSON_PATH = COMMAND_PATH + "generic/ExportJsonToEnvVariables.yaml";

  String HTTP_PATH = "templategalleries/harness/http/";

  String HTTP_HEALTH_CHECK = HTTP_PATH + "HealthCheck.yaml";

  /**
   * Template Folder Constants
   */
  String LOAD_BALANCERS = "Load Balancers";
  String F5_LOAD_BALANCER = "F5 Load Balancer";
  String TOMCAT_COMMANDS = "Tomcat Commands";
  String JBOSS_COMMANDS = "JBoss Commands";
  String POWER_SHELL_COMMANDS = "Power Shell Commands";
  String GENERIC_COMMANDS = "Generic Commands";
  String HTTP_VERIFICATION = "HTTP Verifications";
}

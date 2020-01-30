package software.wings.common;

import com.google.common.collect.ImmutableList;

import java.util.List;

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
  String POWER_SHELL_IIS_V2_INSTALL_PATH = COMMAND_PATH + "powershell/Install-V2.yaml";
  String POWER_SHELL_IIS_V3_INSTALL_PATH = COMMAND_PATH + "powershell/Install-V3.yaml";
  String POWER_SHELL_IIS_V4_INSTALL_PATH = COMMAND_PATH + "powershell/Install-V4.yaml";
  String POWER_SHELL_IIS_V5_INSTALL_PATH = COMMAND_PATH + "powershell/Install-V5.yaml";
  String POWER_SHELL_IIS_WEBSITE_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISWebsite.yaml";
  String POWER_SHELL_IIS_WEBSITE_V2_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISWebsite-V2.yaml";
  String POWER_SHELL_IIS_WEBSITE_V3_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISWebsite-V3.yaml";
  String POWER_SHELL_IIS_WEBSITE_V4_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISWebsite-V4.yaml";
  String POWER_SHELL_IIS_APP_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISApp.yaml";
  String POWER_SHELL_IIS_APP_V2_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISApp-V2.yaml";
  String POWER_SHELL_IIS_APP_V3_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISApp-V3.yaml";
  String POWER_SHELL_IIS_APP_V4_INSTALL_PATH = COMMAND_PATH + "powershell/Install-IISApp-V4.yaml";
  String GENERIC_JSON_PATH = COMMAND_PATH + "generic/ExportJsonToEnvVariables.yaml";

  String HTTP_PATH = "templategalleries/harness/http/";

  String HTTP_HEALTH_CHECK = HTTP_PATH + "HealthCheck.yaml";

  String SHELL_SCRIPT_PATH = "templategalleries/harness/shellscript/";
  String SHELL_SCRIPT_EXAMPLE = SHELL_SCRIPT_PATH + "ShellScript.yaml";

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
  String SHELL_SCRIPTS = "Shell Scripts";

  /**
   * Template types
   */
  String SSH = "SSH";
  String SHELL_SCRIPT = "SHELL_SCRIPT";
  String HTTP = "HTTP";
  String ARTIFACT_SOURCE = "ARTIFACT_SOURCE";
  List<String> TEMPLATE_TYPES_WITH_YAML_SUPPORT = ImmutableList.of(SHELL_SCRIPT, HTTP, SSH, ARTIFACT_SOURCE);

  String TEMPLATE_REF_COMMAND = "TEMPLATE_REF_COMMAND";

  String PCF_PLUGIN = "PCF_PLUGIN";

  /**
   * Artifact Types
   */

  String CUSTOM = "CUSTOM";
}

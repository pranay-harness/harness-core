package io.harness.pcf.model;

public interface PcfConstants {
  String REPOSITORY_DIR_PATH = "./repository";
  String PCF_ARTIFACT_DOWNLOAD_DIR_PATH = "./repository/pcfartifacts";
  String PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX = "PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX: ";
  String PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION = "Pivotal Client Exception: ";
  String CF_HOME = "CF_HOME";
  String CF_PLUGIN_HOME = "CF_PLUGIN_HOME";
  String SYS_VAR_CF_PLUGIN_HOME = "harness.pcf.plugin.home";
  String CF_COMMAND_FOR_CHECKING_AUTOSCALAR = "cf plugins | grep autoscaling-apps";
  String CF_COMMAND_FOR_CHECKING_APP_AUTOSCALAR_BINDING = "cf autoscaling-apps | grep <APP_NAME>";
  String APP_TOKEN = "<APP_NAME>";
  String ENABLE_AUTOSCALING = "enable-autoscaling";
  String DISABLE_AUTOSCALING = "disable-autoscaling";
  String CONFIGURE_AUTOSCALING = "cf configure-autoscaling";
  String CF_APP_AUTOSCALAR_VALIDATION = "cf_appautoscalar";

  String MANIFEST_YML = "manifest.yml";
  String VARS_YML = "vars.yml";

  String APPLICATION_YML_ELEMENT = "applications";
  String NAME_MANIFEST_YML_ELEMENT = "name";
  String MEMORY_MANIFEST_YML_ELEMENT = "memory";
  String INSTANCE_MANIFEST_YML_ELEMENT = "instances";
  String CREATE_SERVICE_MANIFEST_ELEMENT = "create-services";
  String PATH_MANIFEST_YML_ELEMENT = "path";
  String ROUTES_MANIFEST_YML_ELEMENT = "routes";
  String ROUTE_MANIFEST_YML_ELEMENT = "route";
  String NO_ROUTE_MANIFEST_YML_ELEMENT = "no-route";
  String ROUTE_PLACEHOLDER_TOKEN_DEPRECATED = "${ROUTE_MAP}";
  String INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED = "${INSTANCE_COUNT}";
  String RANDOM_ROUTE_MANIFEST_YML_ELEMENT = "random-route";
  String HOST_MANIFEST_YML_ELEMENT = "host";

  String BUILDPACK_MANIFEST_YML_ELEMENT = "buildpack";
  String BUILDPACKS_MANIFEST_YML_ELEMENT = "buildpacks";
  String COMMAND_MANIFEST_YML_ELEMENT = "command";
  String DISK_QUOTA_MANIFEST_YML_ELEMENT = "disk_quota";
  String DOCKER_MANIFEST_YML_ELEMENT = "docker";
  String DOMAINS_MANIFEST_YML_ELEMENT = "domains";
  String ENV_MANIFEST_YML_ELEMENT = "env";
  String HEALTH_CHECK_HTTP_ENDPOINT_MANIFEST_YML_ELEMENT = "health-check-http-endpoint";
  String HEALTH_CHECK_TYPE_MANIFEST_YML_ELEMENT = "health-check-type";
  String HOSTS_MANIFEST_YML_ELEMENT = "hosts";
  String NO_HOSTNAME_MANIFEST_YML_ELEMENT = "no-hostname";
  String SERVICES_MANIFEST_YML_ELEMENT = "services";
  String STACK_MANIFEST_YML_ELEMENT = "stack";
  String TIMEOUT_MANIFEST_YML_ELEMENT = "timeout";
  String ROUTE_PATH_MANIFEST_YML_ELEMENT = "route-path";
  String INFRA_ROUTE = "${infra.route}";
  String PCF_INFRA_ROUTE = "${infra.pcf.route}";

  String LEGACY_NAME_PCF_MANIFEST = "${APPLICATION_NAME}";

  String CONTEXT_NEW_APP_NAME = "newAppName";
  String CONTEXT_NEW_APP_GUID = "newAppGuid";
  String CONTEXT_NEW_APP_ROUTES = "newAppRoutes";

  String CONTEXT_OLD_APP_NAME = "oldAppName";
  String CONTEXT_OLD_APP_GUID = "oldAppGuid";
  String CONTEXT_OLD_APP_ROUTES = "oldAppRoutes";

  String CONTEXT_APP_FINAL_ROUTES = "finalRoutes";
  String CONTEXT_APP_TEMP_ROUTES = "tempRoutes";
  String PCF_CONFIG_FILE_EXTENSION = ".yml";
  String PCF_ROUTE_PATH_SEPARATOR = "/";

  String FILE_START_REGEX = "\\$\\{service\\.manifest}";

  String PCF_AUTOSCALAR_MANIFEST_INSTANCE_LIMITS_ELE = "instance_limits";
  String PCF_AUTOSCALAR_MANIFEST_RULES_ELE = "rules";
}

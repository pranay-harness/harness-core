package io.harness.pcf.model;

public interface PcfConstants {
  String REPOSITORY_DIR_PATH = "./repository";
  String PCF_ARTIFACT_DOWNLOAD_DIR_PATH = "./repository/pcfartifacts";
  String PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX = "PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX: ";
  String PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION = "Pivotal Client Exception: ";
  String CF_HOME = "CF_HOME";

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
}

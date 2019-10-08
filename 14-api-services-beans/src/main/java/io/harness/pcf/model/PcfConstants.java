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
  String CREATE_SERVICE_MANIFEST_ELEMENT = "create-services:";
}

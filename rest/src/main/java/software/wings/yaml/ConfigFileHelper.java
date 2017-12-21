package software.wings.yaml;

import com.google.inject.Inject;

import software.wings.service.intfc.EnvironmentService;

/**
 * Helper class that handles to and from yaml for config file entity.
 * @author rktummala on 10/06/17
 */
public class ConfigFileHelper {
  @Inject EnvironmentService envService;

  //  public ConfigFile fromYaml(String accountId, String appId, String serviceId, String configFileId, ConfigFileYaml
  //  configFileYaml) {
  //    if (configFileId == null) {
  //      configFileId = UUIDGenerator.getUuid();
  //    }
  //    // Creating a final variable since lamdba expression only takes final variables.
  //    final String configFileIdFinal = configFileId;
  //    Map<String, EntityVersionYaml> entityVersionYamlMap = configFileYaml.getEnvNameVersionMap();
  //    Map<String, String> envNameIdMap = getEnvNameIdMap(appId);
  //    Map<String, EntityVersion> entityVersionMap = Maps.newHashMap();
  //    entityVersionYamlMap.entrySet().stream().forEach(entry -> entityVersionMap.put(envNameIdMap.get(entry.getKey()),
  //        createEntityVersion(appId, serviceId, entry.getValue(), configFileYaml, configFileIdFinal)));
  //
  //    ConfigFile configFile = new ConfigFile();
  //    configFile.setAccountId(accountId);
  //    configFile.setChecksum(configFileYaml.getChecksum());
  //    configFile.setChecksumType(configFileYaml.getChecksumType());
  //    configFile.setConfigOverrideExpression(configFileYaml.getConfigOverrideExpression());
  //    configFile.setConfigOverrideType(configFileYaml.getConfigOverrideType());
  //    configFile.setDefaultVersion(configFileYaml.getDefaultVersion());
  //    configFile.setDescription(configFileYaml.getDescription());
  //    configFile.setEnvId(envNameIdMap.get(configFileYaml.getOverrideEnvName()));
  //    configFile.setEnvIdVersionMap(entityVersionMap);
  //    configFile.setEnvIdVersionMapString(getEnvIdVersionMapString(entityVersionMap));
  //    configFile.setEncrypted(configFileYaml.isEncrypted());
  //    configFile.setEntityType(EntityType.SERVICE);
  //    configFile.setEntityId(serviceId);
  //    configFile.setRelativeFilePath(configFileYaml.getRelativeFilePath());
  //    configFile.setSize(configFileYaml.getSize());
  //    configFile.setSetAsDefault(configFileYaml.isSetAsDefault());
  //    configFile.setTemplateId(ServiceVariable.DEFAULT_TEMPLATE_ID);
  //    configFile.setTargetToAllEnv(configFileYaml.isTargetToAllEnv());
  //    configFile.setUuid(configFileId);
  //    return configFile;
  //  }
  //
  //  public ConfigFileYaml toYaml(ConfigFile configFile) {
  //    String appId = configFile.getAppId();
  //    Map<String, String> envIdNameMap = getEnvIdNameMap(appId);
  //
  //    Map<String, EntityVersionYaml> entityVersionYamlMap = new HashMap<>();
  //    configFile.getEnvIdVersionMap().entrySet().stream().forEach(entry ->
  //    entityVersionYamlMap.put(envIdNameMap.get(entry.getKey()),
  //        getEntityVersionYaml(entry.getValue())));
  //
  //    return ConfigFileYaml.Builder.aConfigFileYaml().withChecksum(configFile.getChecksum()).
  //        withChecksumType(configFile.getChecksumType()).withDefaultVersion(configFile.getDefaultVersion()).withDescription(configFile.getDescription()).
  //        withEncrypted(configFile.isEncrypted()).withEnvNameVersionMap(entityVersionYamlMap).
  //        withMimeType(configFile.getMimeType()).withNotes(configFile.getNotes()).
  //        withOverridePath(configFile.getOverridePath()).withParentConfigFileId(configFile.getParentConfigFileId()).
  //        withRelativeFilePath(configFile.getRelativeFilePath()).withSetAsDefault(configFile.isSetAsDefault()).withSize(configFile.getSize()).
  //        withTargetToAllEnv(configFile.isTargetToAllEnv()).build();
  //  }
  //
  //  private EntityVersionYaml getEntityVersionYaml(EntityVersion entityVersion) {
  //    if (entityVersion == null) {
  //      return null;
  //    }
  //    return EntityVersionYaml.Builder.anEntityVersionYaml().withVersion(entityVersion.getVersion()).build();
  //  }
  //
  //  private Map<String, String> getEnvNameIdMap(String appId) {
  //    List<Environment> environmentList = envService.getEnvByApp(appId);
  //    return environmentList.stream().collect(Collectors.toMap(env -> env.getName(), env -> env.getUuid()));
  //  }
  //
  //  private Map<String, String> getEnvIdNameMap(String appId) {
  //    List<Environment> environmentList = envService.getEnvByApp(appId);
  //    return environmentList.stream().collect(Collectors.toMap(env -> env.getUuid(), env -> env.getName()));
  //  }
  //
  //  private String getEnvIdVersionMapString(Map<String, EntityVersion> entityVersionMap) {
  //    return JsonUtils.asJson(entityVersionMap);
  //  }
  //
  //  private EntityVersion createEntityVersion(String appId, String serviceId, EntityVersionYaml entityVersionYaml,
  //  ConfigFileYaml configFileYaml,
  //      String configFileId) {
  //    return
  //    EntityVersion.Builder.anEntityVersion().withVersion(entityVersionYaml.getVersion()).withAppId(appId).withChangeType(ChangeType.CREATED).
  //        withEntityUuid(configFileId).withEntityType(EntityType.CONFIG).build();
  //  }
}

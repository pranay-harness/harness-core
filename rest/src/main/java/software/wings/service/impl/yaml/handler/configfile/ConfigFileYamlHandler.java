package software.wings.service.impl.yaml.handler.configfile;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ChecksumType;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.Yaml;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.ServiceVariable;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.JsonUtils;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * @author rktummala on 12/08/17
 */
public class ConfigFileYamlHandler extends BaseYamlHandler<Yaml, ConfigFile> {
  private static final Logger logger = LoggerFactory.getLogger(ConfigFileYamlHandler.class);

  @Inject private EnvironmentService environmentService;
  @Inject private ConfigService configService;
  @Inject private YamlHelper yamlHelper;
  @Inject private SecretManager secretManager;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Invalid Service for the yaml file:" + yamlFilePath, serviceId);
    Yaml yaml = changeContext.getYaml();
    String targetFilePath = yaml.getTargetFilePath();
    configService.delete(appId, serviceId, EntityType.SERVICE, targetFilePath);
  }

  @Override
  public Yaml toYaml(ConfigFile bean, String appId) {
    // target environments
    Map<String, EntityVersion> envIdVersionMap = bean.getEnvIdVersionMap();
    final List<String> envNameList = Lists.newArrayList();
    if (envIdVersionMap != null) {
      // Find all the envs that are configured to use the default version. If the env is configured to use default
      // version, the value is null.
      List<String> envIdList = envIdVersionMap.entrySet()
                                   .stream()
                                   .filter(entry -> entry.getValue() == null)
                                   .map(entry -> entry.getKey())
                                   .collect(Collectors.toList());
      if (!isEmpty(envIdList)) {
        envIdList.stream().forEach(envId -> {
          Environment environment = environmentService.get(appId, envId, false);
          if (environment != null) {
            envNameList.add(environment.getName());
          }
        });
      }
    }

    String fileName;
    if (bean.isEncrypted()) {
      //      try {
      //        fileName = secretManager.getEncryptedYamlRef(bean);
      //      } catch (IllegalAccessException e) {
      //        throw new WingsException(e);
      //      }
      fileName = bean.getEncryptedFileId();
    } else {
      fileName = bean.getFileName();
    }

    return ConfigFile.Yaml.builder()
        .description(bean.getDescription())
        .encrypted(bean.isEncrypted())
        .targetEnvs(envNameList)
        .targetToAllEnv(bean.isTargetToAllEnv())
        .targetFilePath(bean.getRelativeFilePath())
        .fileName(fileName)
        .checksum(bean.getChecksum())
        .checksumType(Util.getStringFromEnum(bean.getChecksumType()))
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  @Override
  public ConfigFile upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Invalid Service for the yaml file:" + yamlFilePath, serviceId);
    String configFileName = yamlHelper.getNameFromYamlFilePath(yamlFilePath);

    Yaml yaml = changeContext.getYaml();
    List<String> envNameList = yaml.getTargetEnvs();
    Map<String, EntityVersion> envIdMap = Maps.newHashMap();
    if (!isEmpty(envNameList)) {
      envNameList.stream().forEach(envName -> {
        Environment environment = environmentService.getEnvironmentByName(appId, envName);
        if (environment != null) {
          envIdMap.put(environment.getUuid(), null);
        }
      });
    }

    BoundedInputStream inputStream = null;
    ConfigFile previous = get(accountId, yamlFilePath);
    if (!yaml.isEncrypted()) {
      int index = yamlFilePath.lastIndexOf(PATH_DELIMITER);
      if (index != -1) {
        String configFileDirPath = yamlFilePath.substring(0, index);
        String configFilePath = configFileDirPath + PATH_DELIMITER + yaml.getTargetFilePath();

        Optional<ChangeContext> contentChangeContext = changeSetContext.stream()
                                                           .filter(changeContext1 -> {
                                                             String filePath = changeContext1.getChange().getFilePath();
                                                             return filePath.equals(configFilePath);
                                                           })
                                                           .findFirst();

        if (contentChangeContext.isPresent()) {
          ChangeContext fileContext = contentChangeContext.get();
          String fileContent = fileContext.getChange().getFileContent();
          inputStream = new BoundedInputStream(new ByteArrayInputStream(fileContent.getBytes()));
        }
      }
    }

    ConfigFile configFile = new ConfigFile();
    configFile.setAccountId(accountId);
    configFile.setAppId(appId);
    configFile.setName(configFileName);
    configFile.setFileName(configFileName);
    if (yaml.isEncrypted()) {
      configFile.setEncryptedFileId(yaml.getFileName());
    } else {
      configFile.setEncryptedFileId("");
      ChecksumType checksumType = Util.getEnumFromString(ChecksumType.class, yaml.getChecksumType());
      configFile.setFileName(yaml.getFileName());
      configFile.setChecksum(yaml.getChecksum());
      configFile.setChecksumType(checksumType);
    }

    configFile.setSetAsDefault(true);
    configFile.setDescription(yaml.getDescription());
    configFile.setEnvIdVersionMap(envIdMap);
    configFile.setEnvIdVersionMapString(getEnvIdVersionMapString(envIdMap));
    configFile.setEncrypted(yaml.isEncrypted());
    configFile.setEntityType(EntityType.SERVICE);
    configFile.setEntityId(serviceId);
    configFile.setRelativeFilePath(yaml.getTargetFilePath());

    configFile.setTemplateId(ServiceVariable.DEFAULT_TEMPLATE_ID);
    configFile.setTargetToAllEnv(yaml.isTargetToAllEnv());

    if (previous != null) {
      configFile.setUuid(previous.getUuid());
      configService.update(configFile, inputStream);
    } else {
      configService.save(configFile, inputStream);
    }

    return get(accountId, yamlFilePath);
  }

  private String getEnvIdVersionMapString(Map<String, EntityVersion> entityVersionMap) {
    return JsonUtils.asJson(entityVersionMap);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getTargetFilePath() == null || yaml.getFileName() == null);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ConfigFile get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("Invalid Service for the yaml file:" + yamlFilePath, serviceId);
    String relativeFilePath = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return configService.get(appId, serviceId, EntityType.SERVICE, relativeFilePath);
  }
}

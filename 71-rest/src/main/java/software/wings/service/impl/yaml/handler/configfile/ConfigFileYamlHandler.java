package software.wings.service.impl.yaml.handler.configfile;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ChecksumType;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.Yaml;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.JsonUtils;
import software.wings.utils.Util;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * @author rktummala on 12/08/17
 */
@Singleton
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
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Application application = optionalApplication.get();
    Optional<Service> serviceOptional = yamlHelper.getServiceIfPresent(application.getUuid(), yamlFilePath);
    if (!serviceOptional.isPresent()) {
      return;
    }

    Yaml yaml = changeContext.getYaml();
    String targetFilePath = yaml.getTargetFilePath();
    configService.delete(
        optionalApplication.get().getUuid(), serviceOptional.get().getUuid(), EntityType.SERVICE, targetFilePath);
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
                                   .collect(toList());
      if (isNotEmpty(envIdList)) {
        envIdList.forEach(envId -> {
          Environment environment = environmentService.get(appId, envId, false);
          if (environment != null) {
            envNameList.add(environment.getName());
          }
        });
      }
    }

    String fileName;
    if (bean.isEncrypted()) {
      try {
        fileName = secretManager.getEncryptedYamlRef(bean);
      } catch (IllegalAccessException e) {
        throw new WingsException(e);
      }
    } else {
      fileName = Util.normalize(bean.getRelativeFilePath());
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

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  @Override
  public ConfigFile upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Invalid Service for the yaml file:" + yamlFilePath, serviceId, USER);
    String configFileName = yamlHelper.getNameFromYamlFilePath(yamlFilePath);

    Yaml yaml = changeContext.getYaml();
    List<String> envNameList = yaml.getTargetEnvs();
    Map<String, EntityVersion> envIdMap = Maps.newHashMap();
    if (isNotEmpty(envNameList)) {
      envNameList.forEach(envName -> {
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
        String configFilePath = configFileDirPath + PATH_DELIMITER + yaml.getFileName();

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
        } else {
          logger.error("Could not locate file: " + yaml.getFileName());
          throw new WingsException("Could not locate file: " + yaml.getFileName());
        }
      }
    }

    ConfigFile configFile = new ConfigFile();
    configFile.setAccountId(accountId);
    configFile.setAppId(appId);
    configFile.setName(configFileName);
    configFile.setFileName(configFileName);
    if (yaml.isEncrypted()) {
      try {
        EncryptedData encryptedDataFromYamlRef = secretManager.getEncryptedDataFromYamlRef(yaml.getFileName());
        configFile.setEncryptedFileId(encryptedDataFromYamlRef.getUuid());
      } catch (IllegalAccessException e) {
        throw new WingsException("Error while decrypting config file url:" + yaml.getFileName(), e);
      }
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
    configFile.setSyncFromGit(changeContext.getChange().isSyncFromGit());

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
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ConfigFile get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Invalid Service for the yaml file:" + yamlFilePath, serviceId, USER);
    String relativeFilePath = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return configService.get(appId, serviceId, EntityType.SERVICE, relativeFilePath);
  }
}

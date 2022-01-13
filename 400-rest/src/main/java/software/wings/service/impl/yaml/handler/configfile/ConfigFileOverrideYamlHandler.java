/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.configfile;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.Service.GLOBAL_SERVICE_NAME_FOR_YAML;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import static com.google.common.base.Charsets.UTF_8;

import io.harness.beans.EncryptedData;
import io.harness.delegate.beans.ChecksumType;
import io.harness.exception.InvalidRequestException;
import io.harness.stream.BoundedInputStream;

import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.OverrideYaml;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
/**
 * @author rktummala on 12/08/17
 */
@Singleton
@Slf4j
public class ConfigFileOverrideYamlHandler extends BaseYamlHandler<OverrideYaml, ConfigFile> {
  @Inject private YamlHelper yamlHelper;
  @Inject private ConfigService configService;
  @Inject private SecretManager secretManager;

  @Override
  public void delete(ChangeContext<OverrideYaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Optional<Environment> optionalEnvironment =
        yamlHelper.getEnvIfPresent(optionalApplication.get().getUuid(), yamlFilePath);
    if (!optionalEnvironment.isPresent()) {
      return;
    }

    OverrideYaml yaml = changeContext.getYaml();
    String targetFilePath = yaml.getTargetFilePath();
    String serviceName = yamlHelper.getServiceNameForFileOverride(yamlFilePath);
    boolean syncFromGit = changeContext.getChange().isSyncFromGit();
    if (GLOBAL_SERVICE_NAME_FOR_YAML.equals(serviceName)) {
      configService.delete(optionalApplication.get().getUuid(), optionalEnvironment.get().getUuid(),
          EntityType.ENVIRONMENT, targetFilePath, syncFromGit);
    } else {
      Service service = yamlHelper.getServiceByName(optionalApplication.get().getAppId(), serviceName);
      notNullCheck("Service " + serviceName + " associated with file override might be deleted.", service);
      String serviceTemplateId = yamlHelper.getServiceTemplateId(
          optionalApplication.get().getUuid(), optionalEnvironment.get().getUuid(), service.getName());
      configService.delete(optionalApplication.get().getUuid(), serviceTemplateId, EntityType.SERVICE_TEMPLATE,
          targetFilePath, syncFromGit);
    }
  }

  @Override
  public OverrideYaml toYaml(ConfigFile bean, String appId) {
    if (bean.getEntityType() != EntityType.SERVICE_TEMPLATE && bean.getEntityType() != EntityType.ENVIRONMENT) {
      throw new InvalidRequestException("Unknown entity type: " + bean.getEntityType());
    }

    String fileName;
    if (bean.isEncrypted()) {
      fileName = secretManager.getEncryptedYamlRef(bean.getAccountId(), bean.getEncryptedFileId());
    } else {
      fileName = Utils.normalize(bean.getRelativeFilePath());
    }

    return ConfigFile.OverrideYaml.builder()
        .targetFilePath(bean.getRelativeFilePath())
        .fileName(fileName)
        .checksum(bean.getChecksum())
        .checksumType(Utils.getStringFromEnum(bean.getChecksumType()))
        .encrypted(bean.isEncrypted())
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  @Override
  public ConfigFile upsertFromYaml(ChangeContext<OverrideYaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Invalid Environment for the yaml file:" + yamlFilePath, envId, USER);
    String configFileName = yamlHelper.getNameFromYamlFilePath(yamlFilePath);

    String serviceName = yamlHelper.getServiceNameForFileOverride(yamlFilePath);

    OverrideYaml yaml = changeContext.getYaml();

    BoundedInputStream inputStream = null;
    ConfigFile previous = get(accountId, yamlFilePath, changeContext);
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
          inputStream = new BoundedInputStream(new ByteArrayInputStream(fileContent.getBytes(UTF_8)));
        } else {
          if (previous == null || previous.getFileUuid() == null) {
            log.error("Could not locate file: " + yaml.getFileName());
            throw new InvalidRequestException("Could not locate file: " + yaml.getFileName());
          }
        }
      }
    }

    ConfigFile configFile = new ConfigFile();
    configFile.setAccountId(accountId);
    configFile.setAppId(appId);
    configFile.setName(configFileName);
    configFile.setFileName(configFileName);
    if (yaml.isEncrypted()) {
      EncryptedData encryptedDataFromYamlRef = secretManager.getEncryptedDataFromYamlRef(yaml.getFileName(), accountId);
      configFile.setEncryptedFileId(encryptedDataFromYamlRef.getUuid());
    } else {
      configFile.setEncryptedFileId("");

      ChecksumType checksumType = Utils.getEnumFromString(ChecksumType.class, yaml.getChecksumType());
      configFile.setFileName(yaml.getFileName());
      configFile.setChecksum(yaml.getChecksum());
      configFile.setChecksumType(checksumType);
    }

    if (GLOBAL_SERVICE_NAME_FOR_YAML.equals(serviceName)) {
      configFile.setEntityType(EntityType.ENVIRONMENT);
      configFile.setEntityId(envId);
      configFile.setEnvId(GLOBAL_ENV_ID);
      configFile.setTemplateId(ConfigFile.DEFAULT_TEMPLATE_ID);
    } else {
      Service service = yamlHelper.getServiceByName(appId, serviceName);
      notNullCheck("Service " + serviceName + " associated with file override might be deleted.", service);
      String serviceTemplateId = yamlHelper.getServiceTemplateId(appId, envId, serviceName);

      configFile.setEntityId(serviceTemplateId);
      configFile.setTemplateId(serviceTemplateId);
      configFile.setEntityType(EntityType.SERVICE_TEMPLATE);
      configFile.setEnvId(envId);
    }

    configFile.setEncrypted(yaml.isEncrypted());
    configFile.setRelativeFilePath(yaml.getTargetFilePath());
    configFile.setTargetToAllEnv(false);
    configFile.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      configFile.setUuid(previous.getUuid());
      configService.update(configFile, inputStream);
    } else {
      configService.save(configFile, inputStream);
    }

    return get(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return OverrideYaml.class;
  }

  @Override
  public ConfigFile get(String accountId, String yamlFilePath, ChangeContext<OverrideYaml> changeContext) {
    ConfigFile.OverrideYaml yaml = changeContext.getYaml();
    String relativeFilePath = yaml.getTargetFilePath();
    return getConfigFile(accountId, yamlFilePath, relativeFilePath);
  }

  @Override
  public ConfigFile get(String accountId, String yamlFilePath) {
    String relativeFilePath = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return getConfigFile(accountId, yamlFilePath, relativeFilePath);
  }

  private ConfigFile getConfigFile(String accountId, String yamlFilePath, String relativeFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Invalid Environment for the yaml file:" + yamlFilePath, envId, USER);
    String serviceName = yamlHelper.getServiceNameForFileOverride(yamlFilePath);
    if (GLOBAL_SERVICE_NAME_FOR_YAML.equals(serviceName)) {
      return configService.get(appId, envId, EntityType.ENVIRONMENT, relativeFilePath);
    }
    Service service = yamlHelper.getServiceByName(appId, serviceName);
    notNullCheck("Service " + serviceName + " associated with file override might be deleted.", service);
    String serviceTemplateId = yamlHelper.getServiceTemplateId(appId, envId, service.getName());
    return configService.get(appId, serviceTemplateId, EntityType.SERVICE_TEMPLATE, relativeFilePath);
  }
}

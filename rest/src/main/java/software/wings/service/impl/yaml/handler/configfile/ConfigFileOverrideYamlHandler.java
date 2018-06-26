package software.wings.service.impl.yaml.handler.configfile;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.ChecksumType;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.ConfigFile.OverrideYaml;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Util;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
/**
 * @author rktummala on 12/08/17
 */
@Singleton
public class ConfigFileOverrideYamlHandler extends BaseYamlHandler<OverrideYaml, ConfigFile> {
  private static final Logger logger = LoggerFactory.getLogger(ConfigFileOverrideYamlHandler.class);
  @Inject private YamlHelper yamlHelper;
  @Inject private ConfigService configService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;

  @Override
  public void delete(ChangeContext<OverrideYaml> changeContext) throws HarnessException {
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
    configService.delete(optionalApplication.get().getUuid(), optionalEnvironment.get().getUuid(),
        EntityType.ENVIRONMENT, targetFilePath);
  }

  @Override
  public OverrideYaml toYaml(ConfigFile bean, String appId) {
    String fileName;
    if (bean.isEncrypted()) {
      //      try {
      //        fileName = secretManager.getEncryptedYamlRef(bean);
      //      } catch (IllegalAccessException e) {
      //        throw new WingsException(e);
      //      }
      fileName = bean.getEncryptedFileId();
    } else {
      fileName = Util.normalize(bean.getRelativeFilePath());
    }

    String serviceName = null;
    if (EntityType.SERVICE_TEMPLATE.equals(bean.getEntityType())) {
      ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, bean.getTemplateId());
      notNullCheck("Service template is null for the given id:" + bean.getTemplateId(), serviceTemplate, USER);
      String serviceId = serviceTemplate.getServiceId();
      Service service = serviceResourceService.get(appId, serviceId);
      notNullCheck("Service is null for the given id:" + serviceId, service, USER);
      serviceName = service.getName();
    } else {
      if (!EntityType.ENVIRONMENT.equals(bean.getEntityType())) {
        throw new WingsException("Unknown entity type: " + bean.getEntityType());
      }
    }

    return ConfigFile.OverrideYaml.builder()
        .targetFilePath(bean.getRelativeFilePath())
        .serviceName(serviceName)
        .fileName(fileName)
        .checksum(bean.getChecksum())
        .checksumType(Util.getStringFromEnum(bean.getChecksumType()))
        .encrypted(bean.isEncrypted())
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  @Override
  public ConfigFile upsertFromYaml(ChangeContext<OverrideYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Invalid Environment for the yaml file:" + yamlFilePath, envId, USER);
    String configFileName = yamlHelper.getNameFromYamlFilePath(yamlFilePath);

    OverrideYaml yaml = changeContext.getYaml();

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

    if (isNotEmpty(yaml.getServiceName())) {
      String serviceName = yaml.getServiceName();
      if (serviceName == null) {
        configFile.setEntityType(EntityType.ENVIRONMENT);
        configFile.setEntityId(envId);
        configFile.setEnvId(Base.GLOBAL_ENV_ID);
        configFile.setTemplateId(ConfigFile.DEFAULT_TEMPLATE_ID);
      } else {
        Service service = serviceResourceService.getServiceByName(appId, serviceName);
        if (service == null) {
          throw new HarnessException("Unable to locate a service with the given name: " + serviceName);
        }

        List<Key<ServiceTemplate>> templateRefKeysByService =
            serviceTemplateService.getTemplateRefKeysByService(appId, service.getUuid(), envId);
        if (isEmpty(templateRefKeysByService)) {
          throw new HarnessException("Unable to locate a service template for the given service: " + serviceName);
        }

        String serviceTemplateId = (String) templateRefKeysByService.get(0).getId();
        if (isEmpty(serviceTemplateId)) {
          throw new HarnessException(
              "Unable to locate a service template with the given service: " + serviceName + " and env: " + envId);
        }

        configFile.setEntityId(serviceTemplateId);
        configFile.setTemplateId(serviceTemplateId);
        configFile.setEntityType(EntityType.SERVICE_TEMPLATE);
        configFile.setConfigOverrideType(ConfigOverrideType.ALL);
        configFile.setEnvId(envId);
      }
    }

    configFile.setEncrypted(yaml.isEncrypted());
    configFile.setRelativeFilePath(yaml.getTargetFilePath());
    configFile.setTargetToAllEnv(false);

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
  public ConfigFile get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Invalid Application for the yaml file:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Invalid Environment for the yaml file:" + yamlFilePath, envId, USER);
    String relativeFilePath = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return configService.get(appId, envId, EntityType.ENVIRONMENT, relativeFilePath);
  }
}

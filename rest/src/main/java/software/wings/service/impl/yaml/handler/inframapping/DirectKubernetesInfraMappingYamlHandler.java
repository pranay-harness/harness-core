package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class DirectKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, DirectKubernetesInfrastructureMapping> {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesInfraMappingYamlHandler.class);
  @Inject SecretManager secretManager;

  @Override
  public Yaml toYaml(DirectKubernetesInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.DIRECT_KUBERNETES.name());
    yaml.setMasterUrl(bean.getMasterUrl());
    yaml.setUsername(bean.getUsername());
    yaml.setNamespace(bean.getNamespace());

    String fieldName = null;
    String encryptedYamlRef;
    try {
      if (bean.getEncryptedPassword() != null) {
        fieldName = "password";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setPassword(encryptedYamlRef);
      }

      if (bean.getEncryptedCaCert() != null) {
        fieldName = "caCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setCaCert(encryptedYamlRef);
      }

      if (bean.getEncryptedClientCert() != null) {
        fieldName = "clientCert";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setClientCert(encryptedYamlRef);
      }

      if (bean.getEncryptedClientKey() != null) {
        fieldName = "clientKey";
        encryptedYamlRef = secretManager.getEncryptedYamlRef(bean, fieldName);
        yaml.setClientKey(encryptedYamlRef);
      }

    } catch (IllegalAccessException e) {
      logger.warn("Invalid " + fieldName + ". Should be a valid url to a secret");
      throw new WingsException(e);
    }
    return yaml;
  }

  @Override
  public DirectKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = infraMappingYaml.getComputeProviderName().equals(SettingVariableTypes.DIRECT.name())
        ? SettingVariableTypes.DIRECT.name()
        : getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    DirectKubernetesInfrastructureMapping current = new DirectKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    DirectKubernetesInfrastructureMapping previous =
        (DirectKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(DirectKubernetesInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId,
      String envId, String computeProviderId, String serviceId) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, serviceId, null);
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setMasterUrl(infraMappingYaml.getMasterUrl());
    bean.setUsername(infraMappingYaml.getUsername());
    bean.setNamespace(infraMappingYaml.getNamespace());

    bean.setEncryptedPassword(infraMappingYaml.getPassword());
    bean.setEncryptedCaCert(infraMappingYaml.getCaCert());
    bean.setEncryptedClientCert(infraMappingYaml.getClientCert());
    bean.setEncryptedClientKey(infraMappingYaml.getClientKey());

    // Hardcoding it to some value since its a not null field in db. This field was used in name generation logic, but
    // no more.
    bean.setClusterName("clusterName");

    String encryptedRef;

    encryptedRef = infraMappingYaml.getPassword();
    if (encryptedRef != null) {
      bean.setPassword(null);
      bean.setEncryptedPassword(encryptedRef);
    }

    encryptedRef = infraMappingYaml.getCaCert();
    if (encryptedRef != null) {
      bean.setCaCert(null);
      bean.setEncryptedCaCert(encryptedRef);
    }

    encryptedRef = infraMappingYaml.getClientCert();
    if (encryptedRef != null) {
      bean.setClientCert(null);
      bean.setEncryptedClientCert(encryptedRef);
    }

    encryptedRef = infraMappingYaml.getClientKey();
    if (encryptedRef != null) {
      bean.setClientKey(null);
      bean.setEncryptedClientKey(encryptedRef);
    }
  }

  @Override
  public DirectKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (DirectKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}

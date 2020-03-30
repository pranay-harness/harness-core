package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.yaml.ChangeContext;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.sm.StateType;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration.DynaTraceCVConfigurationYaml;

import java.util.List;

public class DynatraceCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<DynaTraceCVConfigurationYaml, DynaTraceCVServiceConfiguration> {
  @Inject DynaTraceService dynaTraceService;
  @Override
  public DynaTraceCVConfigurationYaml toYaml(DynaTraceCVServiceConfiguration bean, String appId) {
    DynaTraceCVConfigurationYaml yaml = DynaTraceCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    List<DynaTraceApplication> serviceList = dynaTraceService.getServices(bean.getConnectorId());
    if (isEmpty(serviceList)) {
      throw new DataCollectionException("No dynatrace services found for the connector " + bean.getConnectorId());
    }
    for (DynaTraceApplication service : serviceList) {
      if (service.getEntityId().equals(bean.getServiceEntityId())) {
        yaml.setDynatraceServiceName(service.getDisplayName());
        break;
      }
    }
    if (isEmpty(yaml.getDynatraceServiceName())) {
      throw new DataCollectionException("No dynatrace service found for the serviceID " + bean.getServiceEntityId());
    }
    yaml.setType(StateType.DYNA_TRACE.name());
    return yaml;
  }

  @Override
  public DynaTraceCVServiceConfiguration upsertFromYaml(
      ChangeContext<DynaTraceCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    DynaTraceCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    DynaTraceCVServiceConfiguration bean = DynaTraceCVServiceConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setStateType(StateType.DYNA_TRACE);
    List<DynaTraceApplication> serviceList = dynaTraceService.getServices(bean.getConnectorId());
    if (isEmpty(serviceList)) {
      throw new DataCollectionException("No dynatrace services found for the connector " + bean.getConnectorName());
    }

    for (DynaTraceApplication service : serviceList) {
      if (service.getDisplayName().equals(yaml.getDynatraceServiceName())) {
        bean.setServiceEntityId(service.getEntityId());
        break;
      }
    }

    if (isEmpty(bean.getServiceEntityId())) {
      throw new DataCollectionException(
          "No dynatrace service found for the service name " + yaml.getDynatraceServiceName());
    }

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return DynaTraceCVConfigurationYaml.class;
  }

  @Override
  public DynaTraceCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (DynaTraceCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
}

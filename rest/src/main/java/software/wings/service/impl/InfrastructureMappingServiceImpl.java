package software.wings.service.impl;

import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ECS;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ErrorCodes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.JsonUtils;
import software.wings.utils.Validator;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;

/**
 * Created by anubhaw on 1/10/17.
 */
@Singleton
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Map<String, InfrastructureProvider> infrastructureProviders;

  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private SettingsService settingsService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String stencilsPath = "/templates/inframapping/";
  private static final String uiSchemaSuffix = "-InfraMappingUISchema.json";

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return wingsPersistence.query(InfrastructureMapping.class, pageRequest);
  }

  @Override
  public InfrastructureMapping save(@Valid InfrastructureMapping infraMapping) {
    if (infraMapping instanceof PhysicalInfrastructureMapping) {
      ServiceTemplate serviceTemplate =
          serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
      Validator.notNullCheck("ServiceTemplate", serviceTemplate);

      PhysicalInfrastructureMapping pyInfraMapping = (PhysicalInfrastructureMapping) infraMapping;

      List<String> distinctHostNames = pyInfraMapping.getHostnames()
                                           .stream()
                                           .map(String::trim)
                                           .filter(Objects::nonNull)
                                           .distinct()
                                           .collect(Collectors.toList());
      logger.info("Duplicate hosts {} ", pyInfraMapping.getHostnames().size() - distinctHostNames.size());

      List<String> existingHosts = ((PhysicalInfrastructureMapping) infraMapping).getHostnames();
      List<String> removedHosts =
          existingHosts.stream().filter(hostName -> !distinctHostNames.contains(hostName)).collect(Collectors.toList());

      List<Host> savedHosts =
          distinctHostNames.stream()
              .map(hostName -> {
                Host host = aHost()
                                .withAppId(pyInfraMapping.getAppId())
                                .withEnvId(pyInfraMapping.getEnvId())
                                .withServiceTemplateId(pyInfraMapping.getServiceTemplateId())
                                .withComputeProviderId(pyInfraMapping.getComputeProviderSettingId())
                                .withHostConnAttr(pyInfraMapping.getHostConnectionAttrs())
                                .withHostName(hostName)
                                .build();
                return getInfrastructureProviderByComputeProviderType(PHYSICAL_DATA_CENTER.name()).saveHost(host);
              })
              .collect(Collectors.toList());

      ((PhysicalInfrastructureMapping) infraMapping).setHostnames(distinctHostNames);
      InfrastructureMapping savedInfraMapping =
          wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping); // TODO:: simplify

      serviceInstanceService.updateInstanceMappings(serviceTemplate, savedInfraMapping, savedHosts, removedHosts);
      return savedInfraMapping;
    }
    return wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping);
  }

  @Override
  public InfrastructureMapping get(String appId, String envId, String infraMappingId) {
    return wingsPersistence.get(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping) {
    return save(infrastructureMapping);
  }

  @Override
  public void delete(String appId, String envId, String infraMappingId) {
    wingsPersistence.delete(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public Map<String, Map<String, Object>> getInfraMappingStencils(String appId) {
    // TODO:: dynamically read
    Map<String, Map<String, Object>> infraStencils = new HashMap<>();

    // TODO:: InfraMapping remove this hack and use stencils model

    JsonNode physicalJsonSchema = JsonUtils.jsonSchema(PhysicalInfrastructureMapping.class);
    List<SettingAttribute> settingAttributes =
        settingsService.getSettingAttributesByType(appId, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name());

    Map<String, String> data =
        settingAttributes.stream().collect(Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getName));

    ObjectNode jsonSchemaField = ((ObjectNode) physicalJsonSchema.get("properties").get("hostConnectionAttrs"));
    jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
    jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));

    infraStencils.put(PHYSICAL_DATA_CENTER.name(),
        ImmutableMap.of("jsonSchema", physicalJsonSchema, "uiSchema", readUiSchema("PHYSICAL_DATA_CENTER")));

    JsonNode awsJsonSchema = JsonUtils.jsonSchema(AwsInfrastructureMapping.class);
    jsonSchemaField = ((ObjectNode) awsJsonSchema.get("properties").get("hostConnectionAttrs"));
    jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
    jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));

    infraStencils.put(AWS.name(), ImmutableMap.of("jsonSchema", awsJsonSchema, "uiSchema", readUiSchema("AWS")));
    infraStencils.put(ECS.name(),
        ImmutableMap.of(
            "jsonSchema", JsonUtils.jsonSchema(EcsInfrastructureMapping.class), "uiSchema", readUiSchema("ECS")));
    infraStencils.put(KUBERNETES.name(),
        ImmutableMap.of("jsonSchema", JsonUtils.jsonSchema(KubernetesInfrastructureMapping.class), "uiSchema",
            readUiSchema("KUBERNETES")));
    return infraStencils;
  }

  @Override
  public List<ServiceInstance> selectServiceInstances(
      String appId, String serviceId, String envId, String computeProviderId, Map<String, Object> selectionParams) {
    String serviceTemplateId =
        (String) serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0).getId();

    InfrastructureMapping infrastructureMapping = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .field("envId")
                                                      .equal(envId)
                                                      .field("serviceTemplateId")
                                                      .equal(serviceTemplateId)
                                                      .field("computeProviderSettingId")
                                                      .equal(computeProviderId)
                                                      .get();
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      syncAwsHostsAndUpdateInstances(appId, envId, serviceTemplateId, infrastructureMapping,
          computeProviderSetting); // TODO:: instead of on-demand do it periodically?
    }
    return selectServiceInstancesByInfraMapping(
        appId, serviceTemplateId, infrastructureMapping.getUuid(), selectionParams);
  }

  private void syncAwsHostsAndUpdateInstances(String appId, String envId, String serviceTemplateId,
      InfrastructureMapping infrastructureMapping, SettingAttribute computeProviderSetting) {
    AwsInfrastructureProvider awsInfrastructureProvider =
        (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
    List<Host> hosts = awsInfrastructureProvider.listHosts(computeProviderSetting, new PageRequest<>()).getResponse();
    saveHostsAndUpdateServiceInstances(
        appId, envId, serviceTemplateId, infrastructureMapping, awsInfrastructureProvider, hosts, Arrays.asList());
  }

  private void saveHostsAndUpdateServiceInstances(String appId, String envId, String serviceTemplateId,
      InfrastructureMapping infrastructureMapping, AwsInfrastructureProvider awsInfrastructureProvider,
      List<Host> newHosts, List<String> deletedHostNames) {
    List<Host> savedHosts = newHosts.stream()
                                .map(host -> {
                                  host.setAppId(appId);
                                  host.setEnvId(envId);
                                  host.setHostConnAttr(infrastructureMapping.getHostConnectionAttrs());
                                  host.setInfraMappingId(infrastructureMapping.getUuid());
                                  host.setServiceTemplateId(serviceTemplateId);
                                  return awsInfrastructureProvider.saveHost(host);
                                })
                                .collect(Collectors.toList());

    ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, serviceTemplateId);
    serviceInstanceService.updateInstanceMappings(serviceTemplate, infrastructureMapping, savedHosts, deletedHostNames);
  }

  private List<ServiceInstance> selectServiceInstancesByInfraMapping(
      String appId, Object serviceTemplateId, String infraMappingId, Map<String, Object> selectionParams) {
    Builder requestBuilder = Builder.aPageRequest()
                                 .addFilter("appId", Operator.EQ, appId)
                                 .addFilter("serviceTemplateId", Operator.EQ, serviceTemplateId)
                                 .addFilter("infraMappingId", Operator.EQ, infraMappingId);

    boolean specificHosts =
        selectionParams.containsKey("specificHosts") && (boolean) selectionParams.get("specificHosts");
    String instanceCount = selectionParams.containsKey("instanceCount")
        ? Integer.toString((int) selectionParams.get("instanceCount"))
        : PageRequest.UNLIMITED;

    if (specificHosts) {
      List<String> hostNames = (List<String>) selectionParams.get("hostNames");
      requestBuilder.addFilter("hostName", Operator.IN, hostNames.toArray());
    } else {
      requestBuilder.withLimit(instanceCount);
    }

    return serviceInstanceService.list(requestBuilder.build()).getResponse();
  }

  @Override
  public List<String> listComputeProviderHosts(String appId, String envId, String serviceId, String computeProviderId) {
    Object serviceTemplateId =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0).getId();
    InfrastructureMapping infrastructureMapping = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .field("envId")
                                                      .equal(envId)
                                                      .field("serviceTemplateId")
                                                      .equal(serviceTemplateId)
                                                      .field("computeProviderSettingId")
                                                      .equal(computeProviderId)
                                                      .get();
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      return ((PhysicalInfrastructureMapping) infrastructureMapping).getHostnames();
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
      Validator.notNullCheck("ComputeProvider", computeProviderSetting);
      return infrastructureProvider.listHosts(computeProviderSetting, new PageRequest<>())
          .getResponse()
          .stream()
          .map(Host::getHostName)
          .collect(Collectors.toList());
    } else {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message",
          "Only PhysicalInfrastructureMapping type is supported for instance selection");
    }
  }

  private InfrastructureProvider getInfrastructureProviderByComputeProviderType(String computeProviderType) {
    return infrastructureProviders.get(computeProviderType);
  }

  @Override
  public InfrastructureMapping getInfraMappingByComputeProviderAndServiceId(
      String appId, String envId, String serviceId, String computeProviderId) {
    Object serviceTemplateId =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0).getId();
    InfrastructureMapping infrastructureMapping = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .field("envId")
                                                      .equal(envId)
                                                      .field("serviceTemplateId")
                                                      .equal(serviceTemplateId)
                                                      .field("computeProviderSettingId")
                                                      .equal(computeProviderId)
                                                      .get();
    Validator.notNullCheck("InfraMapping", infrastructureMapping);
    return infrastructureMapping;
  }

  @Override
  public List<ServiceInstance> provisionNodes(String appId, String serviceId, String envId, String computeProviderId,
      String launcherConfigName, int instanceCount) {
    String serviceTemplateId =
        (String) serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0).getId();

    InfrastructureMapping infrastructureMapping = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .field("envId")
                                                      .equal(envId)
                                                      .field("serviceTemplateId")
                                                      .equal(serviceTemplateId)
                                                      .field("computeProviderSettingId")
                                                      .equal(computeProviderId)
                                                      .get();
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
      Validator.notNullCheck("ComputeProvider", computeProviderSetting);

      AwsInfrastructureProvider awsInfrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(
              infrastructureMapping.getComputeProviderType());

      List<Host> hosts =
          awsInfrastructureProvider.provisionHosts(computeProviderSetting, launcherConfigName, instanceCount);
      saveHostsAndUpdateServiceInstances(
          appId, envId, serviceTemplateId, infrastructureMapping, awsInfrastructureProvider, hosts, Arrays.asList());

      return selectServiceInstancesByInfraMapping(appId, serviceTemplateId, infrastructureMapping.getUuid(),
          ImmutableMap.of(
              "specificHosts", true, "hostNames", hosts.stream().map(Host::getHostName).collect(Collectors.toList())));
    } else {
      throw new WingsException(
          ErrorCodes.INVALID_REQUEST, "message", "Node Provisioning is only supported for AWS infra mapping");
    }
  }

  private Object readUiSchema(String type) {
    try {
      return readResource(stencilsPath + type + uiSchemaSuffix);
    } catch (Exception e) {
      return new HashMap<String, Object>();
    }
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in reasing ui schema - " + file, exception);
    }
  }
}

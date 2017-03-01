package software.wings.service.impl;

import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMapping.InfrastructureMappingType.AWS_ECS;
import static software.wings.beans.InfrastructureMapping.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMapping.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.ArtifactType;
import software.wings.utils.JsonUtils;
import software.wings.utils.Validator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 1/10/17.
 */
@Singleton
@ValidateOnExecution
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Map<String, InfrastructureProvider> infrastructureProviders;

  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private SettingsService settingsService;
  @Inject private HostService hostService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String stencilsPath = "/templates/inframapping/";
  private static final String uiSchemaSuffix = "-InfraMappingUISchema.json";

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return wingsPersistence.query(InfrastructureMapping.class, pageRequest);
  }

  @Override
  public InfrastructureMapping save(@Valid InfrastructureMapping infraMapping) {
    SettingAttribute computeProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
    Validator.notNullCheck("ServiceTemplate", serviceTemplate);

    infraMapping.setServiceId(serviceTemplate.getServiceId());

    String computeProviderType =
        SettingVariableTypes.PHYSICAL_DATA_CENTER.name().equals(infraMapping.getComputeProviderType())
        ? "Data Center"
        : infraMapping.getComputeProviderType();
    infraMapping.setDisplayName(String.format("%s (Cloud provider: %s, Deployment type: %s)",
        computeProviderSetting.getName(), computeProviderType, infraMapping.getDeploymentType()));

    InfrastructureMapping savedInfraMapping = wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping);

    if (savedInfraMapping instanceof PhysicalInfrastructureMapping) {
      savedInfraMapping = syncPhysicalHostsAndServiceInstances(savedInfraMapping, serviceTemplate, new ArrayList<>());
    }
    return savedInfraMapping;
  }

  public InfrastructureMapping syncPhysicalHostsAndServiceInstances(
      InfrastructureMapping infrastructureMapping, ServiceTemplate serviceTemplate, List<String> existingHostNames) {
    PhysicalInfrastructureMapping pyInfraMapping = (PhysicalInfrastructureMapping) infrastructureMapping;

    List<String> distinctHostNames = pyInfraMapping.getHostNames()
                                         .stream()
                                         .map(String::trim)
                                         .filter(Objects::nonNull)
                                         .distinct()
                                         .collect(Collectors.toList());

    if (distinctHostNames.size() < ((PhysicalInfrastructureMapping) infrastructureMapping).getHostNames().size()) {
      logger.info("Duplicate hosts {} ", pyInfraMapping.getHostNames().size() - distinctHostNames.size());
      ((PhysicalInfrastructureMapping) infrastructureMapping).setHostNames(distinctHostNames);
    }

    wingsPersistence.updateField(
        InfrastructureMapping.class, infrastructureMapping.getUuid(), "hostNames", distinctHostNames);

    List<String> removedHosts = existingHostNames.stream()
                                    .filter(hostName -> !distinctHostNames.contains(hostName))
                                    .collect(Collectors.toList());

    List<Host> savedHosts =
        distinctHostNames.stream()
            .map(hostName -> {
              Host host = aHost()
                              .withAppId(pyInfraMapping.getAppId())
                              .withEnvId(pyInfraMapping.getEnvId())
                              .withInfraMappingId(infrastructureMapping.getUuid())
                              .withServiceTemplateId(pyInfraMapping.getServiceTemplateId())
                              .withComputeProviderId(pyInfraMapping.getComputeProviderSettingId())
                              .withHostConnAttr(pyInfraMapping.getHostConnectionAttrs())
                              .withHostName(hostName)
                              .build();
              return getInfrastructureProviderByComputeProviderType(PHYSICAL_DATA_CENTER.name()).saveHost(host);
            })
            .collect(Collectors.toList());

    removedHosts.forEach(hostName
        -> getInfrastructureProviderByComputeProviderType(PHYSICAL_DATA_CENTER.name())
               .deleteHost(infrastructureMapping.getAppId(), infrastructureMapping.getUuid(), hostName));
    serviceInstanceService.updateInstanceMappings(serviceTemplate, infrastructureMapping, savedHosts, removedHosts);
    return infrastructureMapping;
  }

  @Override
  public InfrastructureMapping get(String appId, String infraMappingId) {
    return wingsPersistence.get(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping) {
    InfrastructureMapping savedInfraMapping = get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    UpdateOperations<InfrastructureMapping> updateOperations =
        wingsPersistence.createUpdateOperations(InfrastructureMapping.class);

    if (savedInfraMapping.getHostConnectionAttrs() != null
        && !savedInfraMapping.getHostConnectionAttrs().equals(infrastructureMapping.getHostConnectionAttrs())) {
      getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType())
          .updateHostConnAttrs(infrastructureMapping, infrastructureMapping.getHostConnectionAttrs());
      updateOperations.set("hostConnectionAttrs", infrastructureMapping.getHostConnectionAttrs());
    }

    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      updateOperations.set("clusterName", ((EcsInfrastructureMapping) infrastructureMapping).getClusterName());
    } else if (infrastructureMapping instanceof KubernetesInfrastructureMapping) {
      updateOperations.set("clusterName", ((KubernetesInfrastructureMapping) infrastructureMapping).getClusterName());
    }

    wingsPersistence.update(savedInfraMapping, updateOperations);

    if (savedInfraMapping instanceof PhysicalInfrastructureMapping) {
      ServiceTemplate serviceTemplate =
          serviceTemplateService.get(savedInfraMapping.getAppId(), savedInfraMapping.getServiceTemplateId());
      Validator.notNullCheck("ServiceTemplate", serviceTemplate);
      syncPhysicalHostsAndServiceInstances(
          infrastructureMapping, serviceTemplate, ((PhysicalInfrastructureMapping) savedInfraMapping).getHostNames());
    }
    return get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
  }

  @Override
  public void delete(String appId, String envId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    if (infrastructureMapping != null) {
      boolean deleted = wingsPersistence.delete(infrastructureMapping);
      if (deleted) {
        InfrastructureProvider infrastructureProvider =
            getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType());
        executorService.submit(() -> infrastructureProvider.deleteHostByInfraMappingId(appId, infraMappingId));
        executorService.submit(() -> serviceInstanceService.deleteByInfraMappingId(appId, infraMappingId));
      }
    }
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

    infraStencils.put(PHYSICAL_DATA_CENTER_SSH.name(),
        ImmutableMap.of("jsonSchema", physicalJsonSchema, "uiSchema", readUiSchema(PHYSICAL_DATA_CENTER_SSH.name())));

    JsonNode awsJsonSchema = JsonUtils.jsonSchema(AwsInfrastructureMapping.class);
    jsonSchemaField = ((ObjectNode) awsJsonSchema.get("properties").get("hostConnectionAttrs"));
    jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
    jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));

    infraStencils.put(
        AWS_SSH.name(), ImmutableMap.of("jsonSchema", awsJsonSchema, "uiSchema", readUiSchema(AWS_SSH.name())));
    infraStencils.put(AWS_ECS.name(),
        ImmutableMap.of("jsonSchema", JsonUtils.jsonSchema(EcsInfrastructureMapping.class), "uiSchema",
            readUiSchema(AWS_ECS.name())));
    return infraStencils;
  }

  @Override
  public List<ServiceInstance> selectServiceInstances(
      String appId, String envId, String infraMappingId, Map<String, Object> selectionParams) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      syncAwsHostsAndUpdateInstances(
          infrastructureMapping, computeProviderSetting); // TODO:: instead of on-demand do it periodically?
    }
    return selectServiceInstancesByInfraMapping(
        appId, infrastructureMapping.getServiceTemplateId(), infrastructureMapping.getUuid(), selectionParams);
  }

  @Override
  public List<String> listClusters(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listClusterNames(computeProviderSetting);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> listImages(String appId, String deploymentType, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listAMIs(computeProviderSetting, region);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> listRegions(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listRegions(computeProviderSetting);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> listInstanceTypes(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listInstanceTypes(computeProviderSetting);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> listRoles(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listIAMRoles(computeProviderSetting);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> listNetworks(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listVPCs(computeProviderSetting);
    }
    return new ArrayList<>();
  }

  @Override
  public List<HostValidationResponse> validateHost(@Valid HostValidationRequest validationRequest) {
    SettingAttribute computeProviderSetting = settingsService.get(validationRequest.getComputeProviderSettingId());
    Validator.notNullCheck("ComputeProvider", computeProviderSetting);

    if (!PHYSICAL_DATA_CENTER.name().equals(computeProviderSetting.getValue().getType())) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure provider");
    }
    StaticInfrastructureProvider infrastructureProvider =
        (StaticInfrastructureProvider) getInfrastructureProviderByComputeProviderType(PHYSICAL_DATA_CENTER.name());

    SettingAttribute hostConnectionSetting = settingsService.get(validationRequest.getHostConnectionAttrs());

    return Arrays.asList(infrastructureProvider.validateHost(
        validationRequest.getHostNames().get(0), hostConnectionSetting, validationRequest.getExecutionCredential()));
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
      syncAwsHostsAndUpdateInstances(
          infrastructureMapping, computeProviderSetting); // TODO:: instead of on-demand do it periodically?
    }
    return selectServiceInstancesByInfraMapping(
        appId, serviceTemplateId, infrastructureMapping.getUuid(), selectionParams);
  }

  private void syncAwsHostsAndUpdateInstances(
      InfrastructureMapping infrastructureMapping, SettingAttribute computeProviderSetting) {
    AwsInfrastructureProvider awsInfrastructureProvider =
        (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
    List<Host> hosts = awsInfrastructureProvider.listHosts(computeProviderSetting, new PageRequest<>()).getResponse();
    PageRequest<Host> pageRequest = aPageRequest()
                                        .addFilter("appId", Operator.EQ, infrastructureMapping.getAppId())
                                        .addFilter("infraMappingId", Operator.EQ, infrastructureMapping.getUuid())
                                        .build();
    List<String> existingHostNames =
        hostService.list(pageRequest).getResponse().stream().map(Host::getHostName).collect(Collectors.toList());

    ListIterator<Host> hostListIterator = hosts.listIterator();

    while (hostListIterator.hasNext()) {
      Host host = hostListIterator.next();
      if (existingHostNames.contains(host.getHostName())) {
        hostListIterator.remove();
        existingHostNames.remove(host.getHostName());
      }
    }
    updateHostsAndServiceInstances(infrastructureMapping, hosts, existingHostNames);
  }

  private void updateHostsAndServiceInstances(
      InfrastructureMapping infraMapping, List<Host> newHosts, List<String> deletedHostNames) {
    InfrastructureProvider awsInfrastructureProvider =
        getInfrastructureProviderByComputeProviderType(infraMapping.getComputeProviderType());

    List<Host> savedHosts = newHosts.stream()
                                .map(host -> {
                                  host.setAppId(infraMapping.getAppId());
                                  host.setEnvId(infraMapping.getEnvId());
                                  host.setHostConnAttr(infraMapping.getHostConnectionAttrs());
                                  host.setInfraMappingId(infraMapping.getUuid());
                                  host.setServiceTemplateId(infraMapping.getServiceTemplateId());
                                  return awsInfrastructureProvider.saveHost(host);
                                })
                                .collect(Collectors.toList());

    deletedHostNames.forEach(deletedHostName -> {
      awsInfrastructureProvider.deleteHost(infraMapping.getAppId(), infraMapping.getUuid(), deletedHostName);
    });

    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
    serviceInstanceService.updateInstanceMappings(serviceTemplate, infraMapping, savedHosts, deletedHostNames);
  }

  private List<ServiceInstance> selectServiceInstancesByInfraMapping(
      String appId, Object serviceTemplateId, String infraMappingId, Map<String, Object> selectionParams) {
    Builder requestBuilder = aPageRequest()
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
      return ((PhysicalInfrastructureMapping) infrastructureMapping).getHostNames();
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
    }
    return new ArrayList<>();
  }

  @Override
  public List<LaunchConfiguration> listLaunchConfigs(
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

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
      Validator.notNullCheck("ComputeProvider", computeProviderSetting);
      return infrastructureProvider.listLaunchConfigurations(computeProviderSetting);
    }
    return new ArrayList<>();
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
  public List<ServiceInstance> provisionNodes(
      String appId, String envId, String infraMappingId, String launcherConfigName, int instanceCount) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      SettingAttribute computeProviderSetting =
          settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      Validator.notNullCheck("ComputeProvider", computeProviderSetting);

      AwsInfrastructureProvider awsInfrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(
              infrastructureMapping.getComputeProviderType());

      List<Host> hosts =
          awsInfrastructureProvider.provisionHosts(computeProviderSetting, launcherConfigName, instanceCount);
      updateHostsAndServiceInstances(infrastructureMapping, hosts, Arrays.asList());

      return selectServiceInstancesByInfraMapping(appId, infrastructureMapping.getServiceTemplateId(),
          infrastructureMapping.getUuid(),
          ImmutableMap.of(
              "specificHosts", true, "hostNames", hosts.stream().map(Host::getHostName).collect(Collectors.toList())));
    } else {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "Node Provisioning is only supported for AWS infra mapping");
    }
  }

  @Override
  public void deProvisionNodes(
      String appId, String serviceId, String envId, String computeProviderId, List<String> hostNames) {
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

      awsInfrastructureProvider.deProvisionHosts(
          appId, infrastructureMapping.getUuid(), computeProviderSetting, hostNames);
      updateHostsAndServiceInstances(infrastructureMapping, Arrays.asList(), hostNames);
    } else {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "Node deprovisioning is only supported for AWS infra mapping");
    }
  }

  @Override
  public Map<String, Map<String, String>> listInfraTypes(String appId, String envId, String serviceId) {
    // TODO:: use serviceId and envId to narrow down list ??

    Service service = serviceResourceService.get(appId, serviceId);
    ArtifactType artifactType = service.getArtifactType();
    Map<String, Map<String, String>> infraTypes = new HashMap<>();

    if (artifactType.equals(ArtifactType.DOCKER)) {
      infraTypes.put(AWS.name(), ImmutableMap.of(ECS.name(), AWS_ECS.name()));
    } else {
      infraTypes.put(PHYSICAL_DATA_CENTER.name(), ImmutableMap.of(SSH.name(), PHYSICAL_DATA_CENTER_SSH.name()));
      infraTypes.put(AWS.name(), ImmutableMap.of(SSH.name(), AWS_SSH.name()));
    }
    return infraTypes;
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

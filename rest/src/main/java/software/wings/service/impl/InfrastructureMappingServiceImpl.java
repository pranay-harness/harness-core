package software.wings.service.impl;

import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ECS;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.Valid;

/**
 * Created by anubhaw on 1/10/17.
 */
@Singleton
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject private WingsPersistence wingsPersistence;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String stencilsPath = "/templates/inframapping/";
  private static final String uiSchemaSuffix = "-InfraMappingUISchema.json";

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return wingsPersistence.query(InfrastructureMapping.class, pageRequest);
  }

  @Override
  public InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping) {
    return wingsPersistence.saveAndGet(InfrastructureMapping.class, infrastructureMapping);
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
    infraStencils.put(PHYSICAL_DATA_CENTER.name(),
        ImmutableMap.of("jsonSchema", JsonUtils.jsonSchema(PhysicalInfrastructureMapping.class), "uiSchema",
            readUiSchema("PHYSICAL_DATA_CENTER")));
    infraStencils.put(AWS.name(),
        ImmutableMap.of(
            "jsonSchema", JsonUtils.jsonSchema(AwsInfrastructureMapping.class), "uiSchema", readUiSchema("AWS")));
    infraStencils.put(ECS.name(),
        ImmutableMap.of(
            "jsonSchema", JsonUtils.jsonSchema(EcsInfrastructureMapping.class), "uiSchema", readUiSchema("ECS")));
    infraStencils.put(KUBERNETES.name(),
        ImmutableMap.of("jsonSchema", JsonUtils.jsonSchema(KubernetesInfrastructureMapping.class), "uiSchema",
            readUiSchema("KUBERNETES")));
    return infraStencils;
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

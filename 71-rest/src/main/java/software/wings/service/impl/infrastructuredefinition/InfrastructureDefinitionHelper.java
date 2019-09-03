package software.wings.service.impl.infrastructuredefinition;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.dl.WingsPersistence;
import software.wings.infra.FieldKeyValMapProvider;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;

import java.util.List;
import java.util.Map;

@Slf4j
class InfrastructureDefinitionHelper {
  @Inject private WingsPersistence wingsPersistence;

  String getNameFromInfraDefinition(InfrastructureDefinition infrastructureDefinition, String serviceId) {
    StringBuilder stringBuilder = new StringBuilder()
                                      .append(infrastructureDefinition.getAppId())
                                      .append(infrastructureDefinition.getEnvId())
                                      .append(serviceId)
                                      .append(infrastructureDefinition.getUuid());
    InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
    Map<String, Object> queryMap = ((FieldKeyValMapProvider) infrastructure).getFieldMapForClass();
    queryMap.forEach((k, v) -> stringBuilder.append(v.toString()));
    return DigestUtils.sha1Hex(stringBuilder.toString());
  }

  InfrastructureMapping existingInfraMapping(InfrastructureDefinition infraDefinition, String serviceId) {
    Query baseQuery = getQuery(infraDefinition, serviceId);
    List<InfrastructureMapping> infrastructureMappings = baseQuery.asList();
    if (EmptyPredicate.isEmpty(infrastructureMappings)) {
      return null;
    }
    return infrastructureMappings.get(0);
  }

  @NotNull
  private Query getQuery(InfrastructureDefinition infraDefinition, String serviceId) {
    InfraMappingInfrastructureProvider infrastructure = infraDefinition.getInfrastructure();
    Class<? extends InfrastructureMapping> mappingClass = infrastructure.getMappingClass();
    Map<String, Object> queryMap = ((FieldKeyValMapProvider) infrastructure).getFieldMapForClass();
    Query baseQuery = wingsPersistence.createQuery(mappingClass)
                          .filter(InfrastructureMapping.APP_ID_KEY, infraDefinition.getAppId())
                          .filter(InfrastructureMapping.ENV_ID_KEY, infraDefinition.getEnvId())
                          .filter(InfrastructureMappingKeys.serviceId, serviceId)
                          .filter(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinition.getUuid());
    queryMap.forEach(baseQuery::filter);
    return baseQuery;
  }
}

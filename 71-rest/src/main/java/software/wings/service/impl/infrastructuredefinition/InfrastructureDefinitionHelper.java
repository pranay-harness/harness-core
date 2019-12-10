package software.wings.service.impl.infrastructuredefinition;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.reflection.ReflectionUtils;
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
import java.util.Set;

@Slf4j
class InfrastructureDefinitionHelper {
  @Inject private WingsPersistence wingsPersistence;

  String getNameFromInfraDefinition(InfrastructureDefinition infrastructureDefinition, String serviceId) {
    StringBuilder stringBuilder = new StringBuilder()
                                      .append(infrastructureDefinition.getAppId())
                                      .append(infrastructureDefinition.getEnvId())
                                      .append(serviceId)
                                      .append(infrastructureDefinition.getUuid());
    Map<String, Object> queryMap = getQueryMap(infrastructureDefinition.getInfrastructure());

    if (isNotEmpty(queryMap)) {
      queryMap.entrySet()
          .stream()
          .filter(entry -> entry.getValue() != null)
          .map(entry -> entry.getValue().toString())
          .forEach(stringBuilder::append);
    }
    return DigestUtils.sha1Hex(stringBuilder.toString());
  }

  @NotNull
  Map<String, Object> getQueryMap(InfraMappingInfrastructureProvider infrastructureProvider) {
    Map<String, Object> queryMap = ((FieldKeyValMapProvider) infrastructureProvider).getFieldMapForClass();
    Set<String> uniqueInfraFields = infrastructureProvider.getUserDefinedUniqueInfraFields();
    Map<String, Object> fieldValues = ReflectionUtils.getFieldValues(infrastructureProvider, uniqueInfraFields);
    queryMap.putAll(fieldValues);
    return queryMap;
  }

  InfrastructureMapping existingInfraMapping(InfrastructureDefinition infraDefinition, String serviceId) {
    Query baseQuery = getQuery(infraDefinition, serviceId);
    List<InfrastructureMapping> infrastructureMappings = baseQuery.asList();
    return isEmpty(infrastructureMappings) ? null : infrastructureMappings.get(0);
  }

  @NotNull
  private Query getQuery(InfrastructureDefinition infraDefinition, String serviceId) {
    Map<String, Object> queryMap = getQueryMap(infraDefinition.getInfrastructure());

    Class<? extends InfrastructureMapping> mappingClass = infraDefinition.getInfrastructure().getMappingClass();
    Query baseQuery = wingsPersistence.createQuery(mappingClass)
                          .filter(InfrastructureMapping.APP_ID_KEY, infraDefinition.getAppId())
                          .filter(InfrastructureMapping.ENV_ID_KEY, infraDefinition.getEnvId())
                          .filter(InfrastructureMappingKeys.serviceId, serviceId)
                          .filter(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinition.getUuid());
    queryMap.forEach(baseQuery::filter);
    return baseQuery;
  }
}

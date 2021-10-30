package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DatadogLogCVConfig;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.exception.InvalidRequestException;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatadogLogHealthSourceSpec extends HealthSourceSpec {

    @NotNull String feature;
    @NotNull @NotEmpty
    @Valid List<DatadogLogHealthSourceSpec.QueryDTO> queries;

    @Data
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryDTO {
        @NotNull
        String name;
        @NotNull String query;
        @NotNull List<String> indexes;
        @NotNull String serviceInstanceIdentifier;
    }

    @Value
    @Builder
    private static class Key {
        String serviceIdentifier;
        String envIdentifier;
        String queryName;
    }

    @Override
    public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier, String projectIdentifier,
                                                                     String environmentRef, String serviceRef, String identifier, String name, List<CVConfig> existingCVConfigs,
                                                                     MetricPackService metricPackService) {
        Map<Key, DatadogLogCVConfig> existingConfigMap = getExistingCVConfigMap(existingCVConfigs);
        Map<Key, DatadogLogCVConfig> currentConfigMap = getCurrentCVConfigMap(
                accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name);

        Set<Key> deleted = Sets.difference(existingConfigMap.keySet(), currentConfigMap.keySet());
        Set<Key> added = Sets.difference(currentConfigMap.keySet(), existingConfigMap.keySet());
        Set<Key> updated = Sets.intersection(existingConfigMap.keySet(), currentConfigMap.keySet());
        List<CVConfig> updatedConfigs = updated.stream().map(currentConfigMap::get).collect(Collectors.toList());
        List<CVConfig> updatedConfigWithUuid = updated.stream().map(existingConfigMap::get).collect(Collectors.toList());
        for (int i = 0; i < updatedConfigs.size(); i++) {
            updatedConfigs.get(i).setUuid(updatedConfigWithUuid.get(i).getUuid());
        }
        return HealthSource.CVConfigUpdateResult.builder()
                .deleted(deleted.stream().map(existingConfigMap::get).collect(Collectors.toList()))
                .updated(updatedConfigs)
                .added(added.stream().map(currentConfigMap::get).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void validate() {
        Set<String> uniqueQueryNames = new HashSet<>();
        queries.forEach(query -> {
            if (uniqueQueryNames.contains(query.getName())) {
                throw new InvalidRequestException(String.format("Duplicate query name present %s", query.getName()));
            }
            uniqueQueryNames.add(query.getName());
        });
    }

    private Map<Key, DatadogLogCVConfig> getExistingCVConfigMap(List<CVConfig> existingCVConfigs) {
        Map<Key, DatadogLogCVConfig> existingConfigMap = new HashMap<>();
        List<DatadogLogCVConfig> existingCVConfig = (List<DatadogLogCVConfig>) (List<?>) existingCVConfigs;
        for (DatadogLogCVConfig datadogLogCVConfig : existingCVConfig) {
            existingConfigMap.put(getKeyFromCVConfig(datadogLogCVConfig), datadogLogCVConfig);
        }
        return existingConfigMap;
    }

    private Map<Key, DatadogLogCVConfig> getCurrentCVConfigMap(String accountId, String orgIdentifier,
                                                               String projectIdentifier, String environmentRef,
                                                               String serviceRef, String identifier, String name) {
        List<DatadogLogCVConfig> cvConfigsFromThisObj =
                toCVConfigs(accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef, identifier, name);
        Map<Key, DatadogLogCVConfig> currentCVConfigsMap = new HashMap<>();
        for (DatadogLogCVConfig datadogLogCVConfig : cvConfigsFromThisObj) {
            currentCVConfigsMap.put(getKeyFromCVConfig(datadogLogCVConfig), datadogLogCVConfig);
        }
        return currentCVConfigsMap;
    }

    private Key getKeyFromCVConfig(DatadogLogCVConfig datadogLogCVConfig) {
        return Key.builder()
                .serviceIdentifier(datadogLogCVConfig.getServiceIdentifier())
                .envIdentifier(datadogLogCVConfig.getEnvIdentifier())
                .queryName(datadogLogCVConfig.getQueryName())
                .build();
    }

    private List<DatadogLogCVConfig> toCVConfigs(String accountId, String orgIdentifier, String projectIdentifier,
                                                     String environmentRef, String serviceRef, String identifier, String name) {
        List<DatadogLogCVConfig> cvConfigs = new ArrayList<>();
        queries.forEach(queryDTO -> {
            DatadogLogCVConfig datadogLogCVConfig =
                    DatadogLogCVConfig.builder()
                            .accountId(accountId)
                            .orgIdentifier(orgIdentifier)
                            .projectIdentifier(projectIdentifier)
                            .identifier(identifier)
                            .connectorIdentifier(getConnectorRef())
                            .monitoringSourceName(name)
                            .productName(feature)
                            .envIdentifier(environmentRef)
                            .serviceIdentifier(serviceRef)
                            .queryName(queryDTO.getName())
                            .query(queryDTO.getQuery())
                            .serviceInstanceIdentifier(queryDTO.getServiceInstanceIdentifier())
                            .category(CVMonitoringCategory.ERRORS)
                            .build();
            cvConfigs.add(datadogLogCVConfig);
        });
        return cvConfigs;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATADOG_LOG;
    }
}

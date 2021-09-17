package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import static io.harness.cvng.beans.MonitoredServiceDataSourceType.dataSourceTypeMonitoredServiceDataSourceTypeMap;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.utils.monitoredService.CVConfigToHealthSourceTransformer;
import io.harness.cvng.models.VerificationType;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Data
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthSourceDTO {
  String identifier;
  String name;
  DataSourceType type;
  VerificationType verificationType;

  public static HealthSourceDTO toHealthSourceDTO(HealthSource healthSource) {
    return HealthSourceDTO.builder()
        .name(healthSource.getName())
        .identifier(healthSource.getIdentifier())
        .type(healthSource.getSpec().getType())
        .verificationType(healthSource.getSpec().getType().getVerificationType())
        .build();
  }

  public static HealthSource toHealthSource(List<CVConfig> cvConfigs, Injector injector) {
    Preconditions.checkState(isNotEmpty(cvConfigs), "Cannot convert to HealthSource if cvConfig list is empty");

    CVConfigToHealthSourceTransformer<CVConfig, HealthSourceSpec> cvConfigToHealthSourceTransformer =
        injector.getInstance(
            Key.get(CVConfigToHealthSourceTransformer.class, Names.named(cvConfigs.get(0).getType().name())));

    return HealthSource.builder()
        .name(cvConfigs.get(0).getMonitoringSourceName())
        .type(dataSourceTypeMonitoredServiceDataSourceTypeMap.get(cvConfigs.get(0).getType()))
        .identifier(cvConfigs.get(0).getIdentifier())
        .spec(cvConfigToHealthSourceTransformer.transform(cvConfigs))
        .build();
  }
}

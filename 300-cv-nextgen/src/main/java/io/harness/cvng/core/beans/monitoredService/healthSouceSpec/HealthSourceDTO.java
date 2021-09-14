/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import static io.harness.cvng.beans.MonitoredServiceDataSourceType.dataSourceTypeMonitoredServiceDataSourceTypeMap;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.utils.monitoredService.CVConfigToHealthSourceTransformer;
import io.harness.cvng.models.VerificationType;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.Arrays;
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

  public static HealthSource toHealthSource(CVConfig cvConfig, Injector injector) {
    CVConfigToHealthSourceTransformer<CVConfig, HealthSourceSpec> cvConfigToHealthSourceTransformer =
        injector.getInstance(Key.get(CVConfigToHealthSourceTransformer.class, Names.named(cvConfig.getType().name())));

    return HealthSource.builder()
        .name(cvConfig.getMonitoringSourceName())
        .type(dataSourceTypeMonitoredServiceDataSourceTypeMap.get(cvConfig.getType()))
        .identifier(cvConfig.getIdentifier())
        .spec(cvConfigToHealthSourceTransformer.transform(Arrays.asList(cvConfig)))
        .build();
  }
}

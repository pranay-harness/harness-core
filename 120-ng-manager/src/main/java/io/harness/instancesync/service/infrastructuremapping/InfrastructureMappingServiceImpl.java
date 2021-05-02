package io.harness.instancesync.service.infrastructuremapping;

import io.harness.instancesync.dto.infrastructureMapping.InfrastructureMapping;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;

public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public InfrastructureMapping get(String infrastructureMappingId) {
    return wingsPersistence.get(InfrastructureMapping.class, infrastructureMappingId);
  }
}

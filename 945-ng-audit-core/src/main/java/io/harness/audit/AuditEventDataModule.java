package io.harness.audit;

import io.harness.audit.mapper.AuditEventDataDTOToEntityMapper;
import io.harness.audit.mapper.AuditEventDataEntityToDTOMapper;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class AuditEventDataModule extends AbstractModule {
  @Override
  protected void configure() {
    registerAuditEventDataDTOToEntityMapper();
    registerAuditEventDataEntityToDTOMapper();
  }

  private void registerAuditEventDataEntityToDTOMapper() {
    MapBinder<String, AuditEventDataEntityToDTOMapper> auditEventDataEntityToDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, AuditEventDataEntityToDTOMapper.class);
  }

  private void registerAuditEventDataDTOToEntityMapper() {
    MapBinder<String, AuditEventDataDTOToEntityMapper> auditEventDataDTOtoEntityMapper =
        MapBinder.newMapBinder(binder(), String.class, AuditEventDataDTOToEntityMapper.class);
  }
}

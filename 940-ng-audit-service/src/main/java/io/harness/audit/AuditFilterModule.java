package io.harness.audit;

import io.harness.audit.mapper.AuditFilterPropertiesMapper;
import io.harness.filter.FilterType;
import io.harness.filter.FiltersModule;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class AuditFilterModule extends AbstractModule {
  @Override
  protected void configure() {
    registerRequiredBindings();
    install(FiltersModule.getInstance());

    MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
        MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
    filterPropertiesMapper.addBinding(FilterType.AUDIT.toString()).to(AuditFilterPropertiesMapper.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}

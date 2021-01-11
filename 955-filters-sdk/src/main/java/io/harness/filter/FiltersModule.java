package io.harness.filter;

import io.harness.filter.impl.FilterServiceImpl;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.filter.service.FilterService;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.atomic.AtomicReference;

public class FiltersModule extends AbstractModule {
  private static final AtomicReference<FiltersModule> instanceRef = new AtomicReference();
  public FiltersModule() {}

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(FilterService.class).to(FilterServiceImpl.class);
    MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
        MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
  }

  public static FiltersModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet((FiltersModule) null, new FiltersModule());
    }

    return (FiltersModule) instanceRef.get();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
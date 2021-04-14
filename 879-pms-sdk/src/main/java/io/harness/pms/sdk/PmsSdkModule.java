package io.harness.pms.sdk;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.PmsSdkCoreModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.exceptionmanager.ExceptionHandler;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.sdk.registries.PmsSdkRegistryModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@SuppressWarnings("ALL")
public class PmsSdkModule extends AbstractModule {
  private static PmsSdkModule instance;
  private final PmsSdkConfiguration config;

  public static PmsSdkModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkModule(config);
    }
    return instance;
  }

  private PmsSdkModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    List<Module> modules = getModules();
    for (Module module : modules) {
      install(module);
    }
    MapBinder<Class<? extends Exception>, ExceptionHandler> exceptionHandlerMapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class<? extends Exception>>() {}, new TypeLiteral<ExceptionHandler>() {});
    requireBinding(ExceptionManager.class);
  }

  @NotNull
  private List<Module> getModules() {
    List<Module> modules = new ArrayList<>();
    modules.add(PmsSdkCoreModule.getInstance());
    modules.add(PmsSdkRegistryModule.getInstance(config));
    modules.add(PmsSdkProviderModule.getInstance(config));
    modules.add(PmsSdkQueueModule.getInstance(config));
    if (config.getDeploymentMode().isNonLocal()) {
      modules.add(PmsSdkGrpcModule.getInstance(config));
    } else {
      modules.add(PmsSdkDummyGrpcModule.getInstance());
    }
    return modules;
  }
}

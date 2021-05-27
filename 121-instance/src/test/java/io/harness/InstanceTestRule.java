package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.mockito.Mockito.mock;

import io.harness.annotations.dev.OwnedBy;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ServersModule;
import io.harness.repositories.instance.InstanceRepository;
import io.harness.repositories.instance.InstanceRepositoryCustom;
import io.harness.repositories.instance.InstanceRepositoryCustomImpl;
import io.harness.rule.InjectorRuleMixin;
import io.harness.service.instancedashboardservice.InstanceDashboardService;
import io.harness.service.instancedashboardservice.InstanceDashboardServiceImpl;
import io.harness.testlib.module.MongoRuleMixin;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@Slf4j
@OwnedBy(DX)

public class InstanceTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  ClosingFactory closingFactory;

  public InstanceTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(InstanceDashboardService.class).to(InstanceDashboardServiceImpl.class);
        bind(InstanceRepository.class).toInstance(mock(InstanceRepository.class));
        bind(InstanceRepositoryCustom.class).toInstance(mock(InstanceRepositoryCustomImpl.class));
      }
    });
    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(log, base, method, target);
  }
}

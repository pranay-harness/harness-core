package io.harness.runners;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class GuiceRunner extends BlockJUnit4ClassRunner {
  protected final transient Injector injector;

  public GuiceRunner(final Class<?> klass, Injector injector) throws Exception {
    super(klass);
    this.injector = injector;
  }

  public GuiceRunner(final Class<?> klass) throws Exception {
    super(klass);
    this.injector = Guice.createInjector(this.getModulesFor(klass));
  }

  @Override
  public final Object createTest() throws Exception {
    final Object clazz = super.createTest();
    this.injector.injectMembers(clazz);
    return clazz;
  }

  protected List<Module> getModulesFor(Class<?> klass)
      throws InitializationError, IllegalAccessException, InstantiationException {
    final ModuleProvider annotation = klass.getAnnotation(ModuleProvider.class);
    if (annotation == null) {
      final String message = String.format("Missing @GuiceModules annotation for unit test '%s'", klass.getName());
      throw new InitializationError(message);
    }
    ModuleListProvider moduleListProvider = annotation.value().newInstance();
    return moduleListProvider.modules();
  }
}
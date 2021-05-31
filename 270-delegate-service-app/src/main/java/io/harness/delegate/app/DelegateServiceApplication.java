package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;

import static com.google.common.base.Charsets.UTF_8;

import io.harness.annotations.dev.TargetModule;
import io.harness.serializer.YamlUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
public class DelegateServiceApplication {
  private static DelegateServiceConfig configuration;

  public static DelegateServiceConfig getConfiguration() {
    return configuration;
  }

  public static void main(String... args) throws IOException {
    try {
      File configFile = new File(args[0]);
      configuration = new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), DelegateServiceConfig.class);

      log.info("Starting Delegate Service Application");
      DelegateServiceApplication delegateServiceApplication = new DelegateServiceApplication();
      delegateServiceApplication.run(configuration);
    } catch (RuntimeException | IOException exception) {
      log.error("Delegate Service process initialization failed", exception);
      throw exception;
    }
  }

  private void run(DelegateServiceConfig configuration) {
    List<Module> modules = new ArrayList<>();
    modules.add(new DelegateServiceModule(configuration));
    Injector injector = Guice.createInjector(modules);
  }
}

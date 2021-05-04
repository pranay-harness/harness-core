package io.harness.delegate.app;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.TargetModule;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
@BreakDependencyOn("io.harness.serializer.ManagerRegistrars")
@BreakDependencyOn("io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar")
public class DelegateTaskManagerApplication {
  private static DelegateTaskMgrConfig configuration;

  public static DelegateTaskMgrConfig getConfiguration() {
    return configuration;
  }

  public static void main(String... args) throws IOException {
    try {
      File configFile = new File(args[0]);
      configuration = new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), DelegateTaskMgrConfig.class);

      log.info("Starting Delegate Task Manager");
      DelegateTaskManagerApplication delegateTaskManagerApplication = new DelegateTaskManagerApplication();
      delegateTaskManagerApplication.run(configuration);
    } catch (RuntimeException | IOException exception) {
      log.error("Delegate Task Manager process initialization failed", exception);
      throw exception;
    }
  }

  private void run(DelegateTaskMgrConfig configuration) {
    List<Module> modules = new ArrayList<>();
    modules.add(new DelegateTaskManagerModule());
    Injector injector = Guice.createInjector(modules);
  }
}

package software.wings.app;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.SSOServiceImpl;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSOModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SSOSettingService.class).to(SSOSettingServiceImpl.class);
    bind(SSOService.class).to(SSOServiceImpl.class);
  }
}

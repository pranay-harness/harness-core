package software.wings.app;

import software.wings.service.impl.SSOServiceImpl;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import com.google.inject.AbstractModule;

public class SSOModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SSOSettingService.class).to(SSOSettingServiceImpl.class);
    bind(SSOService.class).to(SSOServiceImpl.class);
  }
}

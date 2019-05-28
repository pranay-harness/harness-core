package software.wings.app;

import com.google.inject.AbstractModule;

import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.intfc.AuthService;

public class AuthModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AuthService.class).to(AuthServiceImpl.class);
  }
}

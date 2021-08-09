package software.wings.app;

import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.intfc.AuthService;

import com.google.inject.AbstractModule;

public class AuthModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AuthService.class).to(AuthServiceImpl.class);
  }
}

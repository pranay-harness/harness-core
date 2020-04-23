package software.wings.delegatetasks.citasks;

import com.google.inject.AbstractModule;

import software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandler;

public class CITaskFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CIBuildTaskHandler.class).to(CIK8BuildTaskHandler.class);
  }
}

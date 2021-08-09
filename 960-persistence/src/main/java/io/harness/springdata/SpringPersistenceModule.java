package io.harness.springdata;

public class SpringPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class};
  }
}

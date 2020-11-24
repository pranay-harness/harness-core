package io.harness.springdata;

public class SpringPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class};
  }
}

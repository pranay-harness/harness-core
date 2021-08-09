package io.harness.springdata;

public class SpringPersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistenceTestConfig.class};
  }
}

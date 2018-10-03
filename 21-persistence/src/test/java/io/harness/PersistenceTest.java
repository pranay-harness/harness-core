package io.harness;

import io.harness.rule.PersistenceRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongodb.morphia.AdvancedDatastore;

public class PersistenceTest extends CategoryTest implements MockableTestMixin {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public PersistenceRule persistenceRule = new PersistenceRule();

  protected AdvancedDatastore getDatastore() {
    return persistenceRule.getDatastore();
  }
}
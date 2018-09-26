package io.harness;

import io.harness.rule.PersistenceRule;
import org.junit.Rule;
import org.mongodb.morphia.AdvancedDatastore;

public class PersistenceTest extends CategoryTest {
  @Rule public PersistenceRule rule = new PersistenceRule();

  protected AdvancedDatastore getDatastore() {
    return rule.getDatastore();
  }
}
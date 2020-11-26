package io.harness.serializer.spring;

import io.harness.beans.EmbeddedUser;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */

public class PersistenceAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("embeddedUser", EmbeddedUser.class);
  }
}

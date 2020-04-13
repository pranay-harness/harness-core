package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class CommandLibraryServerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    logger.info("Registering Command Library Server Entities in morphia");
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    //   add command library service specific classes here
  }
}

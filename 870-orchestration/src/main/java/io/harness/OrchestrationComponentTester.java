/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.serializer.KryoSerializer;
import io.harness.waiter.NotifyCallback;

import com.esotericsoftware.kryo.KryoException;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
public class OrchestrationComponentTester {
  public static void testKryoRegistration(Provider<KryoSerializer> kryoSerializerProvider) {
    KryoSerializer kryoSerializer = kryoSerializerProvider.get();
    Reflections reflections = new Reflections("io.harness", "software.wings");
    Set<Class<? extends NotifyCallback>> callbacks = reflections.getSubTypesOf(NotifyCallback.class)
                                                         .stream()
                                                         .filter(clazz -> !clazz.isInterface())
                                                         .collect(Collectors.toSet());
    List<Class<? extends NotifyCallback>> unregisteredClasses = new ArrayList<>();
    for (Class<? extends NotifyCallback> callback : callbacks) {
      if (!kryoSerializer.isRegistered(callback)) {
        log.error("Class should be registered with kryo : {}", callback.getName());
        unregisteredClasses.add(callback);
      }
    }

    if (!isEmpty(unregisteredClasses)) {
      throw new KryoException("Some Classes missing Kryo Registration");
    }
  }
}

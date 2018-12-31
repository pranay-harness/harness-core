package io.harness.serializer;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HKryo extends Kryo {
  private static final Logger logger = LoggerFactory.getLogger(HKryo.class);

  public HKryo(ClassResolver classResolver) {
    super(classResolver, new MapReferenceResolver(), new DefaultStreamFactory());
  }

  private Registration check(Registration registration, int id) {
    if (registration.getId() != id) {
      throw new IllegalStateException(
          String.format("The class %s was already registered with id %d, do not double register it with %d",
              registration.getType().getCanonicalName(), registration.getId(), id));
    }
    return registration;
  }

  @Override
  public Registration register(Class type) {
    throw new IllegalStateException("Do not use this method");
  }

  @Override
  public Registration register(Class type, int id) {
    return check(super.register(type, id), id);
  }

  private static IntMap<Class> allowed;
  static {
    allowed = new IntMap<>();
    allowed.put(0, int.class);
    allowed.put(1, String.class);
    allowed.put(2, float.class);
    allowed.put(3, boolean.class);
    allowed.put(4, byte.class);
    allowed.put(5, char.class);
    allowed.put(6, short.class);
    allowed.put(7, long.class);
    allowed.put(8, double.class);
    allowed.put(9, void.class);
  }

  private static IntMap<String> allowedNames;
  static {
    allowedNames = new IntMap<>();
    allowedNames.put(24, "java.util.Collections.UnmodifiableCollection");
    allowedNames.put(60, "java.util.Collections.UnmodifiableRandomAccessList");
    allowedNames.put(79, "java.util.Collections.UnmodifiableList");
    allowedNames.put(80, "java.util.Collections.UnmodifiableSet");
    allowedNames.put(81, "java.util.Collections.UnmodifiableSortedSet");
    allowedNames.put(82, "java.util.Collections.UnmodifiableMap");
    allowedNames.put(83, "java.util.Collections.UnmodifiableSortedMap");
    allowedNames.put(84, "java.util.Collections.SynchronizedCollection");
    allowedNames.put(85, "java.util.Collections.SynchronizedRandomAccessList");
    allowedNames.put(86, "java.util.Collections.SynchronizedList");
    allowedNames.put(87, "java.util.Collections.SynchronizedSet");
    allowedNames.put(88, "java.util.Collections.SynchronizedSortedSet");
    allowedNames.put(89, "java.util.Collections.SynchronizedMap");
    allowedNames.put(90, "java.util.Collections.SynchronizedSortedMap");
    allowedNames.put(91, "com.google.common.collect.ImmutableList");
    allowedNames.put(92, "com.google.common.collect.RegularImmutableList");
    allowedNames.put(93, "com.google.common.collect.SingletonImmutableList");
    allowedNames.put(94, "com.google.common.collect.Lists.StringAsImmutableList");
    allowedNames.put(95, "com.google.common.collect.RegularImmutableTable.Values");
    allowedNames.put(96, "com.google.common.collect.ImmutableSet");
    allowedNames.put(97, "com.google.common.collect.RegularImmutableSet");
    allowedNames.put(98, "com.google.common.collect.SingletonImmutableSet");
    allowedNames.put(97, "com.google.common.collect.RegularImmutableSet");
    allowedNames.put(99, "com.google.common.collect.ImmutableEnumSet");
    allowedNames.put(100, "com.google.common.collect.ImmutableMap");
    allowedNames.put(101, "com.google.common.collect.RegularImmutableBiMap");
    allowedNames.put(102, "com.google.common.collect.SingletonImmutableBiMap");
    allowedNames.put(103, "com.google.common.collect.RegularImmutableMap");
    allowedNames.put(104, "com.google.common.collect.ImmutableEnumMap");
    allowedNames.put(105, "com.google.common.collect.ImmutableMultimap");
    allowedNames.put(106, "com.google.common.collect.EmptyImmutableListMultimap");
    allowedNames.put(107, "com.google.common.collect.ImmutableListMultimap");
    allowedNames.put(108, "com.google.common.collect.EmptyImmutableSetMultimap");
    allowedNames.put(109, "com.google.common.collect.ImmutableSetMultimap");
  }

  @Override
  public Registration register(Class type, Serializer serializer) {
    final Registration registration = super.register(type, serializer);
    if (allowed.get(registration.getId()) == registration.getType()) {
      return registration;
    }
    if (registration.getType().getCanonicalName().equals(allowedNames.get(registration.getId()))) {
      return registration;
    }
    throw new IllegalStateException("Do not use this method");
  }

  @Override
  public Registration register(Class type, Serializer serializer, int id) {
    return check(super.register(type, serializer, id), id);
  }

  @Override
  public Registration register(Registration registration) {
    return check(super.register(registration), registration.getId());
  }
}
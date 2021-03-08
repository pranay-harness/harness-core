package io.harness.core;

import io.harness.beans.CastedField;
import io.harness.exceptions.RecasterException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;

@Slf4j
public class RecastObjectCreator implements RecastObjectFactory {
  private static final Objenesis objenesis = new ObjenesisStd(true);
  private final Map<Class<?>, InstanceConstructor<?>> instanceConstructors = new ConcurrentHashMap<>();

  interface InstanceConstructor<T> {
    T construct();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createInstance(final Class<T> clazz) {
    InstanceConstructor<?> instanceConstructor =
        instanceConstructors.computeIfAbsent(clazz, c -> makeInstanceConstructor(clazz));
    try {
      return (T) instanceConstructor.construct();
    } catch (Exception e) {
      throw new RecasterException("Failed to instantiate " + clazz.getName(), e);
    }
  }

  private <T> InstanceConstructor<T> makeInstanceConstructor(Class<T> clazz) {
    final Constructor<T> constructor = noArgsConstructorOrNull(clazz);
    if (constructor != null) {
      return () -> newInstance(constructor);
    }
    if (!Collection.class.isAssignableFrom(clazz)) {
      return () -> objenesis.newInstance(clazz);
    }

    return () -> createInstanceInternal(clazz);
  }

  private static <T> Constructor<T> noArgsConstructorOrNull(final Class<T> type) {
    if (type == null) {
      return null;
    }
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T createInstanceInternal(final Class<T> clazz) {
    try {
      return getNoArgsConstructorOrException(clazz).newInstance();
    } catch (Exception e) {
      if (Collection.class.isAssignableFrom(clazz)) {
        if (Set.class.isAssignableFrom(clazz)) {
          return (T) createSet(null);
        } else if (Collection.class.isAssignableFrom(clazz)) {
          return (T) createList(null);
        } else if (Map.class.isAssignableFrom(clazz)) {
          return (T) createMap(null);
        } else if (Set.class.isAssignableFrom(clazz)) {
          return (T) createSet(null);
        }
      }
      throw new RecasterException("No usable constructor for " + clazz.getName(), e);
    }
  }

  private static <T> Constructor<T> getNoArgsConstructorOrException(final Class<T> type) {
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException e) {
      throw new RecasterException("No usable constructor for " + type.getName(), e);
    }
  }

  @Override
  public <T> T createInstance(final Class<T> clazz, final Document document) {
    Class<T> c = getClass(document);
    if (c == null) {
      c = clazz;
    }
    return createInstance(c);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object createInstance(final Recaster mapper, final CastedField cf, final Document document) {
    Class c = getClass(document);
    if (c == null) {
      c = cf.isSingleValue() ? cf.getType() : cf.getSubClass();
      if (c.equals(Object.class)) {
        c = cf.getType();
      }
    }
    return createInstance(c, document);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> createList(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, ArrayList.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Object> createMap(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, HashMap.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<Object> createSet(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, HashSet.class);
  }

  protected ClassLoader getClassLoaderForClass() {
    return Thread.currentThread().getContextClassLoader();
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> getClass(final Document document) {
    // see if there is a className value
    Class<?> c = null;
    if (document.containsKey(Recaster.RECAST_CLASS_KEY)) {
      final String className = document.getString(Recaster.RECAST_CLASS_KEY);
      // TODO : add caching here
      try {
        c = Class.forName(className, true, getClassLoaderForClass());
      } catch (ClassNotFoundException e) {
        log.warn("Class not found defined in dbObj: ", e);
      }
    }
    return (Class<T>) c;
  }

  /**
   * creates an instance of testType (if it isn't Object.class or null) or fallbackType
   */
  private <T> T newInstance(final Constructor<T> tryMe, final Class<T> fallbackType) {
    if (tryMe != null) {
      tryMe.setAccessible(true);
      try {
        return tryMe.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return createInstance(fallbackType);
  }

  private static <T> T newInstance(Constructor<T> constructor) {
    try {
      return constructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException exception) {
      throw new RecasterException("The class constructor fail", exception);
    }
  }

  private boolean isHarnessClass(String className) {
    return className.startsWith("software.wings") || className.startsWith("io.harness");
  }
}

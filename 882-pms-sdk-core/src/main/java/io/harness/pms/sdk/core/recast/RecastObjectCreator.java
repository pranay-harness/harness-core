package io.harness.pms.sdk.core.recast;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class RecastObjectCreator implements RecastObjectFactory {
  public RecastObjectCreator() {}

  private static <T> Constructor<T> getNoArgsConstructor(final Class<T> type) {
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException e) {
      throw new RecasterException("No usable constructor for " + type.getName(), e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createInstance(final Class<T> clazz) {
    try {
      return getNoArgsConstructor(clazz).newInstance();
    } catch (Exception e) {
      if (Collection.class.isAssignableFrom(clazz)) {
        return (T) createList(null);
      } else if (Map.class.isAssignableFrom(clazz)) {
        return (T) createMap(null);
      } else if (Set.class.isAssignableFrom(clazz)) {
        return (T) createSet(null);
      }
      throw new RecasterException("No usable constructor for " + clazz.getName(), e);
    }
  }

  @Override
  public <T> T createInstance(final Class<T> clazz, final Document dbObj) {
    Class<T> c = getClass(dbObj);
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
  public List createList(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, ArrayList.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map createMap(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, HashMap.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set createSet(final CastedField cf) {
    return newInstance(cf != null ? cf.getConstructor() : null, HashSet.class);
  }

  protected ClassLoader getClassLoaderForClass() {
    return Thread.currentThread().getContextClassLoader();
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> getClass(final Document document) {
    // see if there is a className value
    Class c = null;
    if (document.containsKey(Recaster.RECAST_CLASS_KEY)) {
      final String className = document.getString(Recaster.RECAST_CLASS_KEY);
      // TODO : add caching here
      try {
        c = Class.forName(className, true, getClassLoaderForClass());
      } catch (ClassNotFoundException e) {
        log.warn("Class not found defined in dbObj: ", e);
      }
    }
    return c;
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
}

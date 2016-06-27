package software.wings.utils;

import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappingException;
import software.wings.exception.WingsException;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Created by peeyushaggarwal on 6/23/16.
 */
public class NoDefaultConstructorMorphiaObjectFactory extends DefaultCreator {
  @Override
  public Object createInstance(Class clazz) {
    try {
      final Constructor constructor = getNoArgsConstructor(clazz);
      if (constructor != null) {
        return constructor.newInstance();
      }
      try {
        return getUnsafe().allocateInstance(clazz);
      } catch (Exception e) {
        throw new MappingException("Failed to instantiate " + clazz.getName(), e);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Constructor getNoArgsConstructor(final Class ctorType) {
    try {
      Constructor ctor = ctorType.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @SuppressWarnings("restriction")
  private static Unsafe getUnsafe() {
    try {
      Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
      singleoneInstanceField.setAccessible(true);
      return (Unsafe) singleoneInstanceField.get(null);

    } catch (IllegalArgumentException e) {
      throw createExceptionForObtainingUnsafe(e);
    } catch (SecurityException e) {
      throw createExceptionForObtainingUnsafe(e);
    } catch (NoSuchFieldException e) {
      throw createExceptionForObtainingUnsafe(e);
    } catch (IllegalAccessException e) {
      throw createExceptionForObtainingUnsafe(e);
    }
  }

  private static WingsException createExceptionForObtainingUnsafe(Exception e) {
    throw new WingsException(e);
  }
}

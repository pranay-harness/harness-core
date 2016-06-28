package software.wings.utils;

import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappingException;

import java.lang.reflect.Constructor;

/**
 * Created by peeyushaggarwal on 6/23/16.
 */
public class NoDefaultConstructorMorphiaObjectFactory extends DefaultCreator {
  private static final Objenesis objenesis = new ObjenesisStd(true);

  @Override
  public Object createInstance(Class clazz) {
    try {
      final Constructor constructor = getNoArgsConstructor(clazz);
      if (constructor != null) {
        return constructor.newInstance();
      }
      try {
        return objenesis.newInstance(clazz);
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
}

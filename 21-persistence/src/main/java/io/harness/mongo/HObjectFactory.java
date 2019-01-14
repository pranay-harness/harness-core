package io.harness.mongo;

import io.harness.annotation.MorphiaMove;
import io.harness.exception.WingsException;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappingException;

import java.lang.reflect.Constructor;

public class HObjectFactory extends DefaultCreator {
  private static final Objenesis objenesis = new ObjenesisStd(true);

  @Override
  public Object createInstance(Class clazz) {
    boolean harnessClass = clazz.getName().startsWith("software.wings") || clazz.getName().startsWith("io.harness");
    if (harnessClass) {
      MorphiaMove move = (MorphiaMove) clazz.getAnnotation(MorphiaMove.class);
      if (move != null) {
        try {
          clazz = clazz.getClassLoader().loadClass(move.canonicalName());
        } catch (ClassNotFoundException exception) {
          throw new WingsException(exception);
        }
      }
    }

    try {
      final Constructor constructor = getNoArgsConstructor(clazz);
      if (constructor != null) {
        return constructor.newInstance();
      }
      try {
        if (harnessClass) {
          return objenesis.newInstance(clazz);
        } else {
          return super.createInstance(clazz);
        }
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

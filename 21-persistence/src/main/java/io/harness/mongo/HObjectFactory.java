package io.harness.mongo;

import io.harness.annotation.MorphiaMove;
import io.harness.exception.WingsException;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

      Object object = buildObject(clazz);
      if (object != null) {
        return object;
      }

      try {
        return harnessClass ? objenesis.newInstance(clazz) : super.createInstance(clazz);
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

  private Object buildObject(final Class clazz) {
    if (!Modifier.isPublic(clazz.getModifiers())) {
      return null;
    }
    try {
      final Method builderMethod = clazz.getMethod("builder");
      final Object builder = builderMethod.invoke(null);
      final Class<?> aClass = builder.getClass();
      final Method build = aClass.getMethod("build");
      return build.invoke(builder);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      return null;
    }
  }
}

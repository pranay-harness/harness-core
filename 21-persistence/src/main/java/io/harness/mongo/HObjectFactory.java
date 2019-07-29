package io.harness.mongo;

import com.mongodb.DBObject;
import io.harness.mongo.MorphiaMove.MorphiaMoveKeys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.annotations.ConstructorArgs;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HObjectFactory extends DefaultCreator {
  @Setter private AdvancedDatastore datastore;

  private static synchronized Map<String, Class> collectMorphiaInterfaceImplementers() {
    Map<String, Class> morphiaInterfaceImplementers = new ConcurrentHashMap<>();

    try {
      Reflections reflections = new Reflections("io.harness.serializer.morphia");
      for (Class clazz : reflections.getSubTypesOf(MorphiaRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        morphiaRegistrar.register(morphiaInterfaceImplementers);
      }
    } catch (Exception e) {
      logger.error("Failed to initialize morphia object factory", e);
      System.exit(1);
    }

    return morphiaInterfaceImplementers;
  }

  @Getter private Map<String, Class> morphiaInterfaceImplementers = collectMorphiaInterfaceImplementers();

  private static final Objenesis objenesis = new ObjenesisStd(true);

  private boolean isHarnessClass(String className) {
    return className.startsWith("software.wings") || className.startsWith("io.harness");
  }

  @SuppressWarnings("unchecked")
  private Class getClass(final DBObject dbObj) {
    // see if there is a className value
    Class clazz = null;
    if (dbObj.containsField(Mapper.CLASS_NAME_FIELDNAME)) {
      final String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
      clazz = morphiaInterfaceImplementers.computeIfAbsent(className, name -> {
        if (isHarnessClass(name)) {
          if (datastore != null) {
            final MorphiaMove morphiaMove =
                datastore.createQuery(MorphiaMove.class).filter(MorphiaMoveKeys.target, name).get();
            if (morphiaMove != null) {
              for (String source : morphiaMove.getSources()) {
                try {
                  return Class.forName(source, true, getClassLoaderForClass());
                } catch (ClassNotFoundException ignore) {
                  // do nothing
                }
              }
            }
          }

          logger.error("Class {} is not prerecorded in the known morphia classes", name);
        }

        try {
          return Class.forName(name, true, getClassLoaderForClass());
        } catch (ClassNotFoundException e) {
          logger.warn("Class not found defined in dbObj: ", e);
        }
        return null;
      });
    }
    return clazz;
  }

  interface InstanceConstructor {
    Object construct() throws Exception;
  }

  private Map<Class, InstanceConstructor> instanceConstructors = new ConcurrentHashMap<>();

  @Override
  // This is a copy/paste from the parent DefaultCreator to allow for overriding the getClass method
  public Object createInstance(final Mapper mapper, final MappedField mf, final DBObject dbObj) {
    Class c = getClass(dbObj);
    if (c == null) {
      c = mf.isSingleValue() ? mf.getConcreteType() : mf.getSubClass();
      if (c.equals(Object.class)) {
        c = mf.getConcreteType();
      }
    }
    try {
      return createInstance(c);
    } catch (RuntimeException e) {
      final ConstructorArgs argAnn = mf.getAnnotation(ConstructorArgs.class);
      if (argAnn == null) {
        throw e;
      }
      // TODO: now that we have a mapper, get the arg types that way by getting the fields by name. + Validate names
      final Object[] args = new Object[argAnn.value().length];
      final Class[] argTypes = new Class[argAnn.value().length];
      for (int i = 0; i < argAnn.value().length; i++) {
        // TODO: run converters and stuff against these. Kinda like the List of List stuff,
        // using a fake MappedField to hold the value
        final Object val = dbObj.get(argAnn.value()[i]);
        args[i] = val;
        argTypes[i] = val.getClass();
      }
      try {
        final Constructor constructor = c.getDeclaredConstructor(argTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public Object createInstance(Class clazz) {
    InstanceConstructor instanceConstructor = instanceConstructors.computeIfAbsent(clazz, c -> {
      final Constructor constructor = getNoArgsConstructor(clazz);
      if (constructor != null) {
        return () -> {
          return constructor.newInstance();
        };
      }
      if (isHarnessClass(clazz.getName())) {
        return () -> {
          return objenesis.newInstance(clazz);
        };
      } else {
        return () -> {
          return super.createInstance(clazz);
        };
      }
    });

    try {
      return instanceConstructor.construct();
    } catch (Exception e) {
      throw new MappingException("Failed to instantiate " + clazz.getName(), e);
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

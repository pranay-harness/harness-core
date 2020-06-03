package io.harness.mongo;

import static io.harness.morphia.MorphiaRegistrar.PKG_HARNESS;
import static io.harness.morphia.MorphiaRegistrar.PKG_WINGS;

import com.mongodb.DBObject;
import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.logging.AutoLogRemoveContext;
import io.harness.mongo.MorphiaMove.MorphiaMoveKeys;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar.HelperPut;
import io.harness.morphia.MorphiaRegistrar.NotFoundClass;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.annotations.ConstructorArgs;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.reflections.Reflections;
import org.slf4j.MDC;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class HObjectFactory extends DefaultCreator {
  @Setter private AdvancedDatastore datastore;

  private static synchronized Map<String, Class> collectMorphiaInterfaceImplementers() {
    Map<String, Class> morphiaInterfaceImplementers = new ConcurrentHashMap<>();

    HelperPut h = (name, clazz) -> morphiaInterfaceImplementers.merge(PKG_HARNESS + name, clazz, (x, y) -> {
      throw new GeneralException("Duplicated");
    });

    HelperPut w = (name, clazz) -> morphiaInterfaceImplementers.merge(PKG_WINGS + name, clazz, (x, y) -> {
      throw new GeneralException("Duplicated");
    });

    try {
      Reflections reflections = new Reflections("io.harness.serializer.morphia");
      for (Class clazz : reflections.getSubTypesOf(MorphiaRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

        morphiaRegistrar.registerImplementationClasses(h, w);
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

  private Map<String, Set<String>> alerted = new ConcurrentHashMap<>();

  private Class checkForRollbackClass(String name) {
    if (!isHarnessClass(name)) {
      return null;
    }
    if (datastore == null) {
      return null;
    }
    try (AutoLogRemoveContext ignore1 = new AutoLogRemoveContext(CollectionLogContext.ID)) {
      final MorphiaMove morphiaMove =
          datastore.createQuery(MorphiaMove.class).filter(MorphiaMoveKeys.target, name).get();
      if (morphiaMove != null) {
        for (String source : morphiaMove.getSources()) {
          try {
            return Class.forName(source, true, getClassLoaderForClass());
          } catch (ClassNotFoundException ignore2) {
            // do nothing
          }
        }
      }
    }
    logger.error("Class {} is not prerecorded in the known morphia classes", name);
    return null;
  }

  @SuppressWarnings("unchecked")
  private Class fetchClass(final DBObject dbObj) {
    // see if there is a className value
    if (!dbObj.containsField(Mapper.CLASS_NAME_FIELDNAME)) {
      return null;
    }

    final String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
    Class clazz = morphiaInterfaceImplementers.computeIfAbsent(className, name -> {
      final Class rollbackClass = checkForRollbackClass(name);
      if (rollbackClass != null) {
        return rollbackClass;
      }
      try {
        return Class.forName(name, true, getClassLoaderForClass());
      } catch (ClassNotFoundException e) {
        logger.warn("Class not found defined in dbObj: ", e);
      }
      return NotFoundClass.class;
    });

    if (clazz == NotFoundClass.class) {
      return null;
    }
    logIssues(clazz, className);
    return clazz;
  }

  private void logIssues(Class clazz, String className) {
    String actualClassName = clazz.getName();
    if (className.equals(actualClassName)) {
      return;
    }
    final String collectionName = MDC.get(CollectionLogContext.ID);
    if (collectionName == null) {
      logger.error("The collection was not initialized", new Exception());
      return;
    }

    final Set<String> collections = alerted.computeIfAbsent(className, cn -> new ConcurrentHashSet<>());
    if (!collections.contains(collectionName)) {
      collections.add(collectionName);
      logger.error("Need migration for class from {} to {}", className, actualClassName);
    }
  }

  @Override
  // This is a copy/paste from the parent DefaultCreator to allow for overriding the getClass method
  public Object createInstance(final Mapper mapper, final MappedField mf, final DBObject dbObj) {
    Class c = fetchClass(dbObj);
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
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | NoSuchMethodException exception) {
        throw new UnexpectedException("The class constructor fail", exception);
      }
    }
  }

  private static Object newInstance(Constructor constructor) {
    try {
      return constructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException exception) {
      throw new UnexpectedException("The class constructor fail", exception);
    }
  }

  private InstanceConstructor makeInstanceConstructor(Class clazz) {
    final Constructor constructor = noArgsConstructor(clazz);
    if (constructor != null) {
      return () -> newInstance(constructor);
    }
    if (isHarnessClass(clazz.getName())) {
      return () -> objenesis.newInstance(clazz);
    }

    return () -> super.createInstance(clazz);
  }

  interface InstanceConstructor {
    Object construct();
  }

  private Map<Class, InstanceConstructor> instanceConstructors = new ConcurrentHashMap<>();

  @Override
  public Object createInstance(Class clazz) {
    InstanceConstructor instanceConstructor =
        instanceConstructors.computeIfAbsent(clazz, c -> makeInstanceConstructor(clazz));

    try {
      return instanceConstructor.construct();
    } catch (Exception e) {
      throw new MappingException("Failed to instantiate " + clazz.getName(), e);
    }
  }

  private static Constructor noArgsConstructor(final Class ctorType) {
    try {
      Constructor ctor = ctorType.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  public static Set<Class> checkRegisteredClasses(Set<Class> baseClasses, Map<String, Class> classes) {
    classes.values()
        .stream()
        .filter(clazz -> baseClasses.stream().noneMatch(base -> base.isAssignableFrom(clazz)))
        .forEach(clazz -> logger.info("The class {} has no base registered", clazz.getName()));

    Reflections reflections = new Reflections("software.wings", "io.harness");

    Set<Class> result = new HashSet<>();

    for (Class base : baseClasses) {
      final Set<Class> types = reflections.<Class>getSubTypesOf(base);
      types.stream()
          .filter(clazz -> !classes.containsKey(clazz.getName()))
          .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
          .collect(Collectors.toCollection(() -> result));
    }

    return result;
  }
}

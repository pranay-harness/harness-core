package io.harness.morphia;

import io.harness.exception.GeneralException;
import io.harness.reflection.CodeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface MorphiaRegistrar {
  interface NotFoundClass {}

  interface HelperPut {
    void put(String path, Class clazz);
  }

  // This method needs to list every class referred as an entity class or it is mentioned directly
  // as a field or field of field recursively.
  // This list of classes should overlap with the result of morphia.mapPackage
  void registerClasses(Set<Class> set);

  // This method should register every class that is serialized/deserialized from morphia referring by className field.
  // This could be implementation classes of a field interface or a base abstract or any non final class.
  void registerImplementationClasses(HelperPut h, HelperPut w);

  default void testClassesModule() {
    final Set<Class> classes = new HashSet<>();
    registerClasses(classes);
    CodeUtils.checkHarnessClassesBelongToModule(CodeUtils.location(this.getClass()), classes);
  }

  default void testImplementationClassesModule() {
    final Map<String, Class> map = new HashMap<>();

    HelperPut g = (name, clazz) -> map.merge(name, clazz, (v1, v2) -> {
      if (v1.equals(v2)) {
        throw new GeneralException("Do not register the same value twice");
      } else {
        throw new GeneralException("Registering different class for the same name is very dangerous");
      }
    });

    HelperPut h = (name, clazz) -> g.put("io.harness." + name, clazz);
    HelperPut w = (name, clazz) -> g.put("software.wings." + name, clazz);

    registerImplementationClasses(h, w);

    Set<Class> classes = new HashSet<>(map.values());
    classes.remove(NotFoundClass.class);

    CodeUtils.checkHarnessClassesBelongToModule(CodeUtils.location(this.getClass()), classes);
  }
}

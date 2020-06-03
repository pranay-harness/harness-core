package io.harness.morphia;

import io.harness.exception.GeneralException;
import io.harness.reflection.CodeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface MorphiaRegistrar {
  interface NotFoundClass {}

  String PKG_WINGS = "software.wings.";
  String PKG_HARNESS = "io.harness.";

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

    HelperPut h =
        (name, clazz) -> map.merge(PKG_HARNESS + name, clazz, (x, y) -> { throw new GeneralException("Duplicated"); });

    HelperPut w =
        (name, clazz) -> map.merge(PKG_WINGS + name, clazz, (x, y) -> { throw new GeneralException("Duplicated"); });

    registerImplementationClasses(h, w);

    Set<Class> classes = new HashSet<>(map.values());
    classes.remove(NotFoundClass.class);

    CodeUtils.checkHarnessClassesBelongToModule(CodeUtils.location(this.getClass()), classes);
  }
}

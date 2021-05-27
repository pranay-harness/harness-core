package io.harness;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.packages.HarnessPackages;
import io.harness.reflection.ReflectionUtils;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.ManagerKryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.google.common.hash.Hashing;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;

class MicroserviceInterfaceTool {
  private static void log(String message) {
    System.out.println(message);
  }

  public static void main(String[] args) {
    try {
      Kryo kryo = new Kryo();
      log("Loading all implementers of Kryo Registrars");
      // Set<Class<? extends KryoRegistrar>> registrars = getManagerRegistrar();
      Set<Class<? extends KryoRegistrar>> registrars = getAllImplementingClasses();
      log("Found: " + registrars.size() + " registrars");
      for (Class<? extends KryoRegistrar> registrar : registrars) {
        // log("Registering: " + registrar.getCanonicalName());
        registrar.newInstance().register(kryo);
      }

      DefaultClassResolver classResolver = (DefaultClassResolver) kryo.getClassResolver();
      Field field = classResolver.getClass().getDeclaredField("classToRegistration");
      field.setAccessible(true);
      ObjectMap<Class, Registration> map = (ObjectMap<Class, Registration>) field.get(classResolver);

      // log("Registered Classes:- ");
      ObjectMap.Keys<Class> keys = map.keys();
      int numClasses = 0;
      Map<String, String> classToHash = new HashMap<>();
      while (keys.hasNext) {
        numClasses++;
        Class next = keys.next();
        String canonicalName = next.getCanonicalName();
        classToHash.put(canonicalName, calculateStringHash(next));
      }
      log("Total Classes: " + numClasses);
      List<String> sortedClasses = classToHash.keySet().stream().sorted(String::compareTo).collect(Collectors.toList());
      List<String> sortedHashes = sortedClasses.stream().map(classToHash::get).collect(Collectors.toList());
      String concatenatedHashes = HarnessStringUtils.join(",", sortedHashes);
      String codebaseHash = Hashing.sha256().hashString(concatenatedHashes, StandardCharsets.UTF_8).toString();
      String message = String.format("Codebase Hash: %s", codebaseHash);
      log(message);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // This method computes the hash of a class by generating a String representation of the
  // fields of that class. We can continue improving it further.
  private static String calculateStringHash(Class specialClass) throws Exception {
    List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(specialClass);
    List<String> collect = fields.stream()
                               .map(field -> field.getType().getCanonicalName() + ":" + field.getName())
                               .sorted(String::compareTo)
                               .collect(Collectors.toList());
    String result = HarnessStringUtils.join(";", collect);
    String hash = Hashing.sha256().hashString(result, StandardCharsets.UTF_8).toString();
    // log(specialClass.getCanonicalName() + ":" + hash);
    return hash;
  }

  private static Set<Class<? extends KryoRegistrar>> getManagerRegistrar() {
    return Collections.singleton(ManagerKryoRegistrar.class);
  }

  private static Set<Class<? extends KryoRegistrar>> getAllImplementingClasses() {
    Set<Class<? extends KryoRegistrar>> retval = new HashSet<>();
    Reflections reflections =
        new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS, "io.serializer");
    retval.addAll(reflections.getSubTypesOf(KryoRegistrar.class));
    return retval;
  }
}

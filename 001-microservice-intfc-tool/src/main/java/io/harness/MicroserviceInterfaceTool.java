package io.harness;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.packages.HarnessPackages;
import io.harness.reflection.ReflectionUtils;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.google.common.hash.Hashing;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.ProtocolMessageEnum;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
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

  private static Map<String, String> computeProtoHashes() throws Exception {
    Set<Class> protoClasses = new HashSet<>();
    Reflections reflections =
        new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS, "io.serializer");
    protoClasses.addAll(reflections.getSubTypesOf(GeneratedMessageV3.class));
    protoClasses.addAll(reflections.getSubTypesOf(ProtocolMessageEnum.class));
    Map<String, String> classToHash = new HashMap<>();
    for (Class protoClass : protoClasses) {
      classToHash.put(protoClass.getCanonicalName(), calculateStringHash(protoClass));
    }

    return classToHash;
  }

  private static Map<String, String> computeKryoHashes() throws Exception {
    Kryo kryo = new Kryo();
    log("Loading all implementers of Kryo Registrars");
    Set<Class<? extends KryoRegistrar>> registrars = getAllImplementingClasses();
    log("Found: " + registrars.size() + " registrars");
    for (Class<? extends KryoRegistrar> registrar : registrars) {
      registrar.newInstance().register(kryo);
    }

    DefaultClassResolver classResolver = (DefaultClassResolver) kryo.getClassResolver();
    Field field = classResolver.getClass().getDeclaredField("classToRegistration");
    field.setAccessible(true);
    ObjectMap<Class, Registration> map = (ObjectMap<Class, Registration>) field.get(classResolver);

    ObjectMap.Keys<Class> keys = map.keys();
    Map<String, String> classToHash = new HashMap<>();
    while (keys.hasNext) {
      Class next = keys.next();
      String canonicalName = next.getCanonicalName();
      classToHash.put(canonicalName, calculateStringHash(next));
    }
    return classToHash;
  }

  public static void main(String[] args) {
    try {
      Map<String, String> classToHash = computeKryoHashes();
      classToHash.putAll(computeProtoHashes());
      List<String> sortedClasses = classToHash.keySet().stream().sorted(String::compareTo).collect(Collectors.toList());
      List<String> sortedHashes = sortedClasses.stream().map(classToHash::get).collect(Collectors.toList());
      String concatenatedHashes = HarnessStringUtils.join(",", sortedHashes);
      String codebaseHash = Hashing.sha256().hashString(concatenatedHashes, StandardCharsets.UTF_8).toString();
      String message = String.format("Codebase Hash:%s", codebaseHash);
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
    return hash;
  }

  private static Set<Class<? extends KryoRegistrar>> getAllImplementingClasses() {
    Set<Class<? extends KryoRegistrar>> retval = new HashSet<>();
    Reflections reflections =
        new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS, "io.serializer");
    retval.addAll(reflections.getSubTypesOf(KryoRegistrar.class));
    return retval;
  }
}

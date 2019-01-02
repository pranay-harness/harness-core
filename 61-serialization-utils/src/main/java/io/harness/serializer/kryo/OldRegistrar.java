package io.harness.serializer.kryo;

import static io.harness.serializer.kryo.SerializationClasses.serializationClasses;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

public class OldRegistrar implements KryoRegistrar {
  private static final Logger logger = LoggerFactory.getLogger(OldRegistrar.class);
  @Override
  public void register(Kryo kryo) throws Exception {
    // Harness classes
    Map<String, Integer> classIds = serializationClasses();
    if (classIds != null) {
      for (Entry<String, Integer> entry : classIds.entrySet()) {
        kryo.register(Class.forName(entry.getKey()), entry.getValue());
      }
    }
  }
}

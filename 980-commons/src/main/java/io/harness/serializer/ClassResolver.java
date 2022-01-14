package io.harness.serializer;

import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.IntMap;

public class ClassResolver extends DefaultClassResolver {
  public IntMap<Registration> getRegistrations() {
    return idToRegistration;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.reflection.CodeUtils;
import io.harness.serializer.ClassResolver;
import io.harness.serializer.HKryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.KryoSerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.util.IntMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.redisson.codec.KryoCodec;
import org.reflections.Reflections;

@OwnedBy(PL)
public class RedissonKryoCodec extends KryoCodec {
  private boolean useFieldSerializer;

  public RedissonKryoCodec() {
    this.useFieldSerializer = true;
  }

  public RedissonKryoCodec(boolean useFieldSerializer) {
    this.useFieldSerializer = useFieldSerializer;
  }

  @Override
  protected Kryo createInstance(List<Class<?>> classes, ClassLoader classLoader) {
    return kryo(useFieldSerializer);
  }

  public static synchronized Kryo kryo(boolean useFieldSerializer) {
    final ClassResolver classResolver = new ClassResolver();
    HKryo kryo = new HKryo(classResolver);
    if (useFieldSerializer) {
      kryo.setDefaultSerializer(FieldSerializer.class);
    }

    try {
      Reflections reflections = new Reflections("io.harness.serializer.kryo");
      for (Class clazz : reflections.getSubTypesOf(KryoRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final KryoRegistrar kryoRegistrar = (KryoRegistrar) constructor.newInstance();

        final IntMap<Registration> previousState = new IntMap<>(classResolver.getRegistrations());
        kryo.setCurrentLocation(CodeUtils.location(kryoRegistrar.getClass()));
        kryoRegistrar.register(kryo);

        try {
          KryoSerializer.check(previousState, classResolver.getRegistrations());
        } catch (Exception exception) {
          throw new IllegalStateException(
              format("Check for registration of %s failed", clazz.getCanonicalName()), exception);
        }
      }

    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing kryo", e);
    }

    return kryo;
  }
}

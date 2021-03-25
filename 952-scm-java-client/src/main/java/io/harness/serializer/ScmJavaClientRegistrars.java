package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.ScmJavaClientKryoRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class ScmJavaClientRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(ScmJavaClientKryoRegistrar.class).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().build();
}

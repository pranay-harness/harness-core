/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.WaitEngineKryoRegister;
import io.harness.serializer.morphia.WaitEngineMorphiaRegistrar;
import io.harness.serializer.spring.NotifyCallbackReadConverter;
import io.harness.serializer.spring.NotifyCallbackWriteConverter;
import io.harness.serializer.spring.ProgressCallbackReadConverter;
import io.harness.serializer.spring.ProgressCallbackWriteConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class WaitEngineRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(TimeoutEngineRegistrars.kryoRegistrars)
          .add(WaitEngineKryoRegister.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(TimeoutEngineRegistrars.morphiaRegistrars)
          .add(WaitEngineMorphiaRegistrar.class)
          .build();

  public static Iterable<? extends Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.of(NotifyCallbackReadConverter.class, NotifyCallbackWriteConverter.class,
          ProgressCallbackReadConverter.class, ProgressCallbackWriteConverter.class);
}

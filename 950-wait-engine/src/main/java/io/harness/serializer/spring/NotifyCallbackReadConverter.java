/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class NotifyCallbackReadConverter implements Converter<Binary, NotifyCallback> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public NotifyCallback convert(Binary data) {
    if (data == null) {
      return null;
    }
    return (NotifyCallback) kryoSerializer.asInflatedObject(data.getData());
  }
}

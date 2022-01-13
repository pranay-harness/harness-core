/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

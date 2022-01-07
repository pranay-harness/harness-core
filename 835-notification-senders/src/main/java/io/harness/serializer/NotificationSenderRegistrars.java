/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.NotificationSenderKryoRegistrar;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.serializer.morphia.NotificationSenderMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;

public class NotificationSenderRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(NotificationSenderKryoRegistrar.class)
          .add(DelegateTasksBeansKryoRegister.class)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(NotificationSenderMorphiaRegistrar.class)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .build();
}

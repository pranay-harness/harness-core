/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.UsageRestrictions;
import software.wings.security.WorkflowFilter;

import com.esotericsoftware.kryo.Kryo;

public class RbacCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(UsageRestrictions.class, 5247);
    kryo.register(UsageRestrictions.AppEnvRestriction.class, 5248);
    kryo.register(GenericEntityFilter.class, 5249);
    kryo.register(EnvFilter.class, 5250);
    kryo.register(WorkflowFilter.class, 5251);
    kryo.register(PermissionAttribute.Action.class, 5354);
    kryo.register(PermissionAttribute.PermissionType.class, 5353);
    kryo.register(PermissionAttribute.class, 5352);
  }
}

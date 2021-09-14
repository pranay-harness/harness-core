/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.Encryptable;
import io.harness.context.MdcGlobalContextData;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.security.SimpleEncryption;
import io.harness.tasks.Task;

import java.io.Serializable;
import java.util.Set;

@OwnedBy(PL)
public class CommonMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Task.class);
    set.add(Serializable.class);
    set.add(Encryptable.class);
    set.add(DecryptableEntity.class);
    set.add(NestedAnnotationResolver.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("context.MdcGlobalContextData", MdcGlobalContextData.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);
    h.put("limits.impl.model.StaticLimit", StaticLimit.class);
    h.put("security.SimpleEncryption", SimpleEncryption.class);

    w.put("security.encryption.SimpleEncryption", SimpleEncryption.class);
  }
}

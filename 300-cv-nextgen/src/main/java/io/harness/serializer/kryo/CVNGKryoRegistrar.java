/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.services.impl.CVNGStep;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CV)
public class CVNGKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // TODO: should we move CVNGStep and it's logic to a separate module.
    kryo.register(CVNGStep.CVNGResponseData.class, 30000);
    kryo.register(CVNGStepParameter.class, 30001);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.serializer.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class NGHarnessJacksonModule extends Module {
  @Override
  public String getModuleName() {
    return "NGHarnessJacksonModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addDeserializers(new NGHarnessDeserializers());
    context.addSerializers(new NGHarnessSerializers());
    context.addTypeModifier(new NGHarnessJacksonTypeModifier());
  }
}

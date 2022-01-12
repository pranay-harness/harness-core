/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

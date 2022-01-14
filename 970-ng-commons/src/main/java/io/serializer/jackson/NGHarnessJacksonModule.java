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

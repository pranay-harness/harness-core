package io.harness.serializer.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class HarnessJacksonModule extends Module {
  @Override
  public String getModuleName() {
    return "HarnessJacksonModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addDeserializers(new HarnessDeserializers());
    context.addTypeModifier(new HarnessJacksonTypeModifier());
  }
}

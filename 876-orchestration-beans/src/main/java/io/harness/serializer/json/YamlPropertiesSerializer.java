package io.harness.serializer.json;

import io.harness.pms.contracts.plan.YamlProperties;

public class YamlPropertiesSerializer extends ProtoJsonSerializer<YamlProperties> {
  public YamlPropertiesSerializer() {
    super(YamlProperties.class);
  }
}

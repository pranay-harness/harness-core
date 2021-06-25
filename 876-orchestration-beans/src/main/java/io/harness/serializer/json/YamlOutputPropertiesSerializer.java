package io.harness.serializer.json;

import io.harness.pms.contracts.plan.YamlOutputProperties;

public class YamlOutputPropertiesSerializer extends ProtoJsonSerializer<YamlOutputProperties> {
  public YamlOutputPropertiesSerializer() {
    super(YamlOutputProperties.class);
  }
}

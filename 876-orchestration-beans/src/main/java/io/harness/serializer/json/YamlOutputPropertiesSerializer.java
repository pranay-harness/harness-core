/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.json;

import io.harness.pms.contracts.plan.YamlOutputProperties;

public class YamlOutputPropertiesSerializer extends ProtoJsonSerializer<YamlOutputProperties> {
  public YamlOutputPropertiesSerializer() {
    super(YamlOutputProperties.class);
  }
}

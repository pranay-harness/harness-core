/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class RiskDeserializer extends StdDeserializer<Integer> {
  public RiskDeserializer() {
    this(null);
  }

  protected RiskDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Integer deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    String riskText = jsonParser.getText();
    if (isNumber(riskText)) {
      return Integer.parseInt(riskText);
    } else {
      return Risk.valueOf(jsonParser.getText()).getValue();
    }
  }

  private boolean isNumber(String text) {
    try {
      Integer.parseInt(text);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}

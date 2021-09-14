/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.ParsedQuery;

import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(HarnessTeam.PIPELINE)
@ReadingConverter
public class ParsedQueryReadConverter implements Converter<String, ParsedQuery> {
  @Override
  public ParsedQuery convert(String jsonString) {
    if (jsonString == null) {
      return null;
    }
    Map<String, Object> objectMap = JsonUtils.asMap(jsonString);
    return new ParsedQuery(objectMap);
  }
}

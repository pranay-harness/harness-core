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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(HarnessTeam.PIPELINE)
@WritingConverter
public class ParsedQueryWriteConverter implements Converter<ParsedQuery, String> {
  @Override
  public String convert(ParsedQuery parsedQuery) {
    if (parsedQuery == null) {
      return null;
    }
    return JsonUtils.asJson(parsedQuery);
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

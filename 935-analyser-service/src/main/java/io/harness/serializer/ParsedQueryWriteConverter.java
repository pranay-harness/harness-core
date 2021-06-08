package io.harness.serializer;

import io.harness.event.ParsedQuery;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

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

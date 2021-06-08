package io.harness.serializer;

import io.harness.event.ParsedQuery;

import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

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

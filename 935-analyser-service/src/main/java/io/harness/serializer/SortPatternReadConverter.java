package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.query.SortPattern;

import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(HarnessTeam.PIPELINE)
@ReadingConverter
public class SortPatternReadConverter implements Converter<String, SortPattern> {
  @Override
  public SortPattern convert(String jsonString) {
    if (jsonString == null) {
      return null;
    }
    Map<String, Object> objectMap = JsonUtils.asMap(jsonString);
    return new SortPattern(objectMap);
  }
}

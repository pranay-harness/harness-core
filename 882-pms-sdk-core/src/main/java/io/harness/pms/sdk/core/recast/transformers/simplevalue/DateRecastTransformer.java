package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public DateRecastTransformer() {
    super(ImmutableList.of(Date.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    if (fromObject instanceof Date) {
      return fromObject;
    }

    if (fromObject instanceof Number) {
      return new Date(((Number) fromObject).longValue());
    }

    if (fromObject instanceof String) {
      try {
        return new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.US).parse((String) fromObject);
      } catch (ParseException e) {
        log.error("Can't parse Date from: " + fromObject);
      }
    }

    throw new IllegalArgumentException("Can't convert to Date from " + fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }
}

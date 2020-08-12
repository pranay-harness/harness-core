package io.harness.ng.core.utils;

import io.harness.exception.InvalidRequestException;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
public class NGUtils {
  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList) {
    for (Pair<?, ?> pair : valuesList) {
      if (!pair.getKey().equals(pair.getValue())) {
        throw new InvalidRequestException(
            "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue());
      }
    }
  }
}

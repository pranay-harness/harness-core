package io.harness.queryconverter;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CE)
public class QueryHelper {
  public static void throwInvalidIfNull(Object object, String msg) {
    if (object == null) {
      throw new InvalidRequestException(msg);
    }
  }
}

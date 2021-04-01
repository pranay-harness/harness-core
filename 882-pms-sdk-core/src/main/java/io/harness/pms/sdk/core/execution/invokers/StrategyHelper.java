package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;

import java.util.Map;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class StrategyHelper {
  public static Supplier buildResponseDataSupplier(Map<String, ResponseData> responseDataMap) {
    return () -> {
      if (isEmpty(responseDataMap)) {
        return null;
      }
      ResponseData data = responseDataMap.values().iterator().next();
      if (data instanceof ErrorResponseData) {
        throw new ErrorDataException((ErrorResponseData) data);
      }
      return data;
    };
  }
}

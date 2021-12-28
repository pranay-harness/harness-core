package io.harness.cdng;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.common.NGTimeConversionHelper;
import io.harness.executions.steps.StepConstants;
import io.harness.plancreator.steps.common.StepElementParameters;

public class TimeOutHelper {
  public static int getTimeoutInMin(StepElementParameters stepParameters) {
    String timeout = getTimeoutValue(stepParameters);
    return NGTimeConversionHelper.convertTimeStringToMinutes(timeout);
  }

  public static long getTimeoutInMillis(StepElementParameters stepParameters) {
    String timeout = getTimeoutValue(stepParameters);
    return NGTimeConversionHelper.convertTimeStringToMilliseconds(timeout);
  }

  public static String getTimeoutValue(StepElementParameters stepParameters) {
    return stepParameters.getTimeout() == null || isEmpty(stepParameters.getTimeout().getValue())
        ? StepConstants.defaultTimeout
        : stepParameters.getTimeout().getValue();
  }
}

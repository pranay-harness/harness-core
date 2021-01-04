package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.UnitStep;

public interface ProtobufStepSerializer<T extends CIStepInfo> {
  UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId);
}

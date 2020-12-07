package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.StepElement;

public interface ProtobufStepSerializer<T extends CIStepInfo> {
  UnitStep serializeStep(StepElement step);
  String serializeToBase64(StepElement step);
}

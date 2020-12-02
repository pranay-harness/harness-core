package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.product.ci.engine.proto.UnitStep;

public interface ProtobufStepSerializer<T extends CIStepInfo> {
  UnitStep serializeStep(T step);
  String serializeToBase64(T step);
}

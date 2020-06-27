package io.harness.cdng.infra.yaml;

import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.data.Outcome;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StepParameters;

//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//@JsonSubTypes({ @JsonSubTypes.Type(value = K8SDirectInfrastructureSpecYaml.class, name = "kubernetesDirect") })
public interface Infrastructure extends Outcome, StepParameters, PassThroughData {
  InfraMapping getInfraMapping();
  InfrastructureKind getKind();
}

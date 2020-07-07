package io.harness.cdng.infra.yaml;

import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.data.Outcome;
import io.harness.facilitator.PassThroughData;
import io.harness.yaml.core.intfc.OverridesApplier;

//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//@JsonSubTypes({ @JsonSubTypes.Type(value = K8SDirectInfrastructureSpecYaml.class, name = "kubernetesDirect") })
public interface Infrastructure extends Outcome, PassThroughData, OverridesApplier<Infrastructure> {
  InfraMapping getInfraMapping();
  InfrastructureKind getKind();
}

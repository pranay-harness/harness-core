package software.wings.sm.states.k8s;

import io.harness.beans.SweepingOutputInstance;
import io.harness.serializer.KryoSerializer;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;

import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@FieldNameConstants(innerTypeName = "K8DelegateSpawnStateKeys")
@Attributes
@Slf4j
public class K8DelegateProvisionState extends State implements SweepingOutputStateMixin {
  @Getter @Setter private String kubernetesConnectorId;
  @Getter @Setter private String delegateName;
  @Getter @Setter private String cpu;
  @Getter @Setter private String memory;
  @Getter @Setter private String artifactVersion;
  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;
  @Transient @Inject @Getter KryoSerializer kryoSerializer;

  public K8DelegateProvisionState(String name) {
    super(name, StateType.DELEGATE_PROVISION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

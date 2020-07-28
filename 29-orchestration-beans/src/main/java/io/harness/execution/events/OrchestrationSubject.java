package io.harness.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;
import io.harness.registries.RegistrableEntity;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationSubject extends Subject<OrchestrationEventHandlerProxy> implements RegistrableEntity {}

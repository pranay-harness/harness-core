package io.harness.execution.events;

import io.harness.observer.Subject;
import io.harness.registries.RegistrableEntity;

public class OrchestrationSubject extends Subject<OrchestrationEventHandlerAsyncWrapper> implements RegistrableEntity {}

package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@OwnedBy(PL)
public interface Resource {
  String getType();

  Set<ScopeLevel> getValidScopeLevels();

  Optional<String> getEventFrameworkEntityType();

  ResourceInfo getResourceInfoFromEvent(Message message);

  List<Boolean> validate(List<String> resourceIds, Scope scope);

  // Enhance it to have {Scope,List<ValidatorType>} with validations
  EnumSet<ValidatorType> getSelectorKind();
}

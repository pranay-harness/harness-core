package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@OwnedBy(PL)
public interface ResourceValidator {
  String getResourceType();

  Set<Scope> getScopes();

  Optional<String> getEventFrameworkEntityType();

  ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message);

  List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  EnumSet<ValidatorType> getValidatorTypes();
}

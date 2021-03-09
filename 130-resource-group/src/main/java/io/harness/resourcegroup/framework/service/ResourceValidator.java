package io.harness.resourcegroup.framework.service;

import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.model.Scope;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResourceValidator {
  String getResourceType();

  Set<Scope> getScopes();

  Optional<String> getEventFrameworkEntityType();

  ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message);

  List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  EnumSet<ValidatorType> getValidatorTypes();
}

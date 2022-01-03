package io.harness.audit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public enum ResourceType {
  ORGANIZATION(ResourceTypeConstants.ORGANIZATION),
  PROJECT(ResourceTypeConstants.PROJECT),
  USER_GROUP(ResourceTypeConstants.USER_GROUP),
  SECRET(ResourceTypeConstants.SECRET),
  RESOURCE_GROUP(ResourceTypeConstants.RESOURCE_GROUP),
  USER(ResourceTypeConstants.USER),
  ROLE(ResourceTypeConstants.ROLE),
  ROLE_ASSIGNMENT(ResourceTypeConstants.ROLE_ASSIGNMENT),
  PIPELINE(ResourceTypeConstants.PIPELINE),
  TRIGGER(ResourceTypeConstants.TRIGGER),
  TEMPLATE(ResourceTypeConstants.TEMPLATE),
  INPUT_SET(ResourceTypeConstants.INPUT_SET),
  DELEGATE_CONFIGURATION(ResourceTypeConstants.DELEGATE_CONFIGURATION),
  SERVICE(ResourceTypeConstants.SERVICE),
  ENVIRONMENT(ResourceTypeConstants.ENVIRONMENT),
  DELEGATE(ResourceTypeConstants.DELEGATE),
  SERVICE_ACCOUNT(ResourceTypeConstants.SERVICE_ACCOUNT),
  CONNECTOR(ResourceTypeConstants.CONNECTOR),
  API_KEY(ResourceTypeConstants.API_KEY),
  TOKEN(ResourceTypeConstants.TOKEN),
  DELEGATE_TOKEN(ResourceTypeConstants.DELEGATE_TOKEN);

  ResourceType(String resourceType) {
    if (!this.name().equals(resourceType)) {
      throw new IllegalArgumentException(
          String.format("ResourceType enum: %s doesn't match constant: %s", this.name(), resourceType));
    }
  }
}

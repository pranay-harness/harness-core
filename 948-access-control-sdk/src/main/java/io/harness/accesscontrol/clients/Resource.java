package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class Resource {
  String resourceType;
  String resourceIdentifier;

  public static final Resource NONE = null;

  public static Resource of(String resourceType, String resourceIdentifier) {
    return Resource.builder().resourceType(resourceType).resourceIdentifier(resourceIdentifier).build();
  }
}

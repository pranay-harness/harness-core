package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class Resource {
  String resourceType;
  String resourceIdentifier;

  public static Resource of(@NotEmpty String resourceType, @Nullable String resourceIdentifier) {
    return Resource.builder().resourceType(resourceType).resourceIdentifier(resourceIdentifier).build();
  }
}

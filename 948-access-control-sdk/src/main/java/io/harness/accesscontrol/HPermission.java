package io.harness.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HPermission implements Permission {
  @NotEmpty String action;
  @NotEmpty String resourceType;
  String resourceId;

  @Override
  public String getPermissionAsString() {
    if (Optional.ofNullable(resourceId).isPresent()) {
      return String.format("%s###%s###%s", resourceType, action, resourceId);
    }
    return String.format("%s###%s", resourceType, action);
  }

  public static Optional<HPermission> getFromString(String fqn) {
    String[] splitted = fqn.split("###");
    HPermissionBuilder permissionBuilder = HPermission.builder();
    if (splitted.length == 2) {
      permissionBuilder.resourceType(splitted[0]).action(splitted[1]);
    } else {
      permissionBuilder.resourceType(splitted[0]).action(splitted[1]).resourceId(splitted[2]);
    }
    HPermission hPermission = permissionBuilder.build();
    return Optional.ofNullable(hPermission.getResourceType()).isPresent() ? Optional.of(hPermission) : Optional.empty();
  }
}

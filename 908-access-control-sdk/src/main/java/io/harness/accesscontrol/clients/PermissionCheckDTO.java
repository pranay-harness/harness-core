package io.harness.accesscontrol.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "PermissionCheck")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionCheckDTO {
  @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String resourceType;
  String resourceIdentifier;
  @NotEmpty String permission;
}

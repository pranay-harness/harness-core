package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "AccessCheckRequest")
@Schema(name = "AccessCheckRequest")
@OwnedBy(HarnessTeam.PL)
public class AccessCheckRequestDTO {
  @Schema(description = "List of permission checks to perform", required = true)
  @Size(max = 1000)
  @Valid
  List<PermissionCheckDTO> permissions;
  @Schema(description = "Principal (user/service account) to check the access for", required = true)
  @Valid
  @Nullable
  Principal principal;
}

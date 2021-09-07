package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
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
@OwnedBy(HarnessTeam.PL)
public class AccessCheckRequestDTO {
  @Size(max = 1000) @Valid List<PermissionCheckDTO> permissions;
  @Valid @Nullable Principal principal;
}

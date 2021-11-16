package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@ApiModel(value = "AccessCheckResponse")
@Schema(name = "AccessCheckResponse")
public class AccessCheckResponseDTO {
  Principal principal;
  private List<AccessControlDTO> accessControlList;
}

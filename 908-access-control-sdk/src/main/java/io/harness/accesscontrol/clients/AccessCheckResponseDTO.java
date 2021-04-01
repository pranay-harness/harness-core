package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "AccessCheckResponse")
public class AccessCheckResponseDTO {
  Principal principal;
  private List<AccessControlDTO> accessControlList;
}

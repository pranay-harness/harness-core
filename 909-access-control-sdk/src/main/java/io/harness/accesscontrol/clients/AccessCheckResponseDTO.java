package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessCheckResponseDTO {
  Principal principal;
  private List<AccessControlDTO> accessControlList;
}

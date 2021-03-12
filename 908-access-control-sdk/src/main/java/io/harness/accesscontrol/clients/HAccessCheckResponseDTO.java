package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.HPrincipal;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "AccessCheckResponse")
public class HAccessCheckResponseDTO implements AccessCheckResponseDTO {
  HPrincipal principal;
  private List<HAccessControlDTO> accessControlList;
}

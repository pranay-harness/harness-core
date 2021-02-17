package io.harness.accesscontrol.acl.services;

import io.harness.accesscontrol.acl.dtos.AccessCheckRequestDTO;
import io.harness.accesscontrol.acl.dtos.AccessCheckResponseDTO;

public interface ACLService {
  AccessCheckResponseDTO get(AccessCheckRequestDTO dto);
}

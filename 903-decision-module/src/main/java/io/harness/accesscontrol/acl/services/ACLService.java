package io.harness.accesscontrol.acl.services;

import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;

public interface ACLService {
  AccessCheckResponseDTO get(AccessCheckRequestDTO dto);
}

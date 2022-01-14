package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.HostRecordDTO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface HostRecordService {
  void save(HostRecordDTO hostRecordDTO);
  void save(List<HostRecordDTO> hostRecordDTOs);
  Set<String> get(String verificationTaskId, Instant startTime, Instant endTime);
  Set<String> get(Set<String> verificationTaskIds, Instant startTime, Instant endTime);
}

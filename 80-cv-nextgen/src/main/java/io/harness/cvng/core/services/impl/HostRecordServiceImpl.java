package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.core.entities.HostRecord;
import io.harness.cvng.core.entities.HostRecord.HostRecordKeys;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.persistence.HPersistence;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HostRecordServiceImpl implements HostRecordService {
  @Inject private HPersistence hPersistence;

  @Override
  public void save(HostRecordDTO hostRecordDTO) {
    hPersistence.save(toHostRecord(hostRecordDTO));
  }

  @Override
  public void save(List<HostRecordDTO> hostRecordDTOs) {
    saveRecords(hostRecordDTOs.stream().map(this ::toHostRecord).collect(Collectors.toList()));
  }

  @Override
  public Set<String> get(String verificationTaskId, Instant startTime, Instant endTime) {
    List<HostRecord> hostRecords = hPersistence.createQuery(HostRecord.class)
                                       .filter(HostRecordKeys.verificationTaskId, verificationTaskId)
                                       .field(HostRecordKeys.startTime)
                                       .greaterThanOrEq(startTime)
                                       .field(HostRecordKeys.endTime)
                                       .lessThanOrEq(endTime)
                                       .asList();
    return hostRecords.stream()
        .map(hostRecord -> hostRecord.getHosts())
        .flatMap(hosts -> hosts.stream())
        .collect(Collectors.toSet());
  }

  private void saveRecords(List<HostRecord> hostRecords) {
    hPersistence.save(hostRecords);
  }
  private HostRecord toHostRecord(HostRecordDTO hostRecordDTOs) {
    return HostRecord.builder()
        .accountId(hostRecordDTOs.getAccountId())
        .verificationTaskId(hostRecordDTOs.getVerificationTaskId())
        .hosts(hostRecordDTOs.getHosts())
        .startTime(hostRecordDTOs.getStartTime())
        .endTime(hostRecordDTOs.getEndTime())
        .build();
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.yamlChangeSet;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlChangeSetRepository
    extends PagingAndSortingRepository<YamlChangeSet, String>, YamlChangeSetRepositoryCustom {
  int countByAccountIdAndStatusIn(String accountId, List<String> status);

  int countByAccountIdAndStatusInAndQueueKey(String accountId, List<String> status, String queueKey);

  Optional<YamlChangeSet> findFirstByAccountIdAndQueueKeyAndStatusOrderByQueuedOn(
      String accountId, String queueKey, String status);

  List<YamlChangeSet> findByAccountIdInAndStatusInAndCutOffTimeLessThan(
      List<String> accountIds, List<String> status, Long cutOffTime);

  List<YamlChangeSet> findByAccountIdAndQueueKeyAndStatus(String accountId, String queueKey, String status);
}

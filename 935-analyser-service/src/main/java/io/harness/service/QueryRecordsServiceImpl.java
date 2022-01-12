/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;
import io.harness.repositories.QueryRecordsRepository;
import io.harness.service.beans.QueryRecordKey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class QueryRecordsServiceImpl implements QueryRecordsService {
  private final QueryRecordsRepository queryRecordsRepository;
  private static final int PAGE_SIZE = 1000;

  @Override
  public Map<QueryRecordKey, List<QueryRecordEntity>> findAllEntries() {
    int page = 0;
    List<QueryRecordEntity> queryRecordEntityList;
    Map<QueryRecordKey, List<QueryRecordEntity>> result = new HashMap<>();
    do {
      queryRecordEntityList = queryRecordsRepository.findAllHashes(page, PAGE_SIZE);
      for (QueryRecordEntity queryRecordEntity : queryRecordEntityList) {
        QueryRecordKey queryRecordKey = QueryRecordKey.builder()
                                            .hash(queryRecordEntity.getHash())
                                            .serviceName(queryRecordEntity.getServiceName())
                                            .version(queryRecordEntity.getMajorVersion())
                                            .build();
        if (result.containsKey(queryRecordKey)) {
          result.get(queryRecordKey).add(queryRecordEntity);
        } else {
          LinkedList<QueryRecordEntity> valuesList = new LinkedList<>();
          valuesList.add(queryRecordEntity);
          result.put(queryRecordKey, valuesList);
        }
      }
      page++;
    } while (!queryRecordEntityList.isEmpty());
    return result;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.migration.NGMigration;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.entities.SourceCodeManager;
import io.harness.ng.userprofile.entities.SourceCodeManager.SourceCodeManagerMapper;
import io.harness.repositories.ng.userprofile.spring.SourceCodeManagerRepository;

import com.google.inject.Inject;
import com.mongodb.MongoException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PL)
@Slf4j
public class SourceCodeManagerMigrationService implements NGMigration {
  private final MongoTemplate mongoTemplate;
  private final SourceCodeManagerRepository sourceCodeManagerRepository;
  private final NgUserService ngUserService;
  private Map<SCMType, SourceCodeManagerMapper> scmMapBinder;

  @Override
  public void migrate() {
    int pageIdx = 0;
    int pageSize = 20;
    int maxUsers = 10000;
    int maxPages = maxUsers / pageSize;
    int numNewSourceCodeManagerCreated = 0;

    try {
      mongoTemplate.getCollection("sourceCodeManagers").dropIndex("query_idx");
    } catch (MongoException e) {
      if (e.getCode() == 27) {
        log.info("query_idx is already deleted", e);
      } else {
        throw new UnexpectedException("Migration failed. Could not delete query_idx", e);
      }
    }

    while (pageIdx < maxPages) {
      Pageable pageable = PageRequest.of(pageIdx, pageSize);
      Query query = new Query().with(pageable);
      List<SourceCodeManager> oldSourceCodeManagerList = mongoTemplate.find(query, SourceCodeManager.class);
      if (oldSourceCodeManagerList.isEmpty()) {
        break;
      }

      Set<SourceCodeManager> newSourceCodeManagerList = new HashSet<>();
      for (SourceCodeManager oldSourceCodeManager : oldSourceCodeManagerList) {
        newSourceCodeManagerList.addAll(getNewSourceCodeManagerList(oldSourceCodeManager));
      }

      try {
        sourceCodeManagerRepository.saveAll(newSourceCodeManagerList);
        numNewSourceCodeManagerCreated += newSourceCodeManagerList.size();
      } catch (DuplicateKeyException e) {
        // this would happen when migration is run for the second time
      } catch (Exception e) {
        log.error("Couldn't save SourceCodeManager", e);
      }

      pageIdx++;
      if (pageIdx % (maxPages / 5) == 0) {
        log.info("SourceCodeManager migration in process...");
      }
    }
    log.info("SourceCodeManager migration completed");
    log.info("Total {} SourceCodeManager created", numNewSourceCodeManagerCreated);
  }

  private Set<SourceCodeManager> getNewSourceCodeManagerList(SourceCodeManager oldSourceCodeManager) {
    SourceCodeManagerDTO dto = scmMapBinder.get(oldSourceCodeManager.getType()).toSCMDTO(oldSourceCodeManager);
    List<String> accountIds = ngUserService.listUserAccountIds(oldSourceCodeManager.getUserIdentifier());
    Set<SourceCodeManager> sourceCodeManagerList = new HashSet<>();
    for (String accountId : accountIds) {
      dto.setAccountIdentifier(accountId);
      sourceCodeManagerList.add(scmMapBinder.get(dto.getType()).toSCMEntity(dto));
    }
    return sourceCodeManagerList;
  }
}

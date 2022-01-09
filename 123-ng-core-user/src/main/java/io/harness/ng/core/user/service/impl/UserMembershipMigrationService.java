/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.migration.NGMigration;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembershipOld;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.repositories.user.spring.UserMetadataRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PL)
@Slf4j
public class UserMembershipMigrationService implements NGMigration {
  private final MongoTemplate mongoTemplate;
  private final UserMetadataRepository userMetadataRepository;
  private final UserMembershipRepository userMembershipRepository;

  @Inject
  public UserMembershipMigrationService(MongoTemplate mongoTemplate, UserMetadataRepository userMetadataRepository,
      UserMembershipRepository userMembershipRepository) {
    this.mongoTemplate = mongoTemplate;
    this.userMetadataRepository = userMetadataRepository;
    this.userMembershipRepository = userMembershipRepository;
  }

  public void migrate() {
    int pageIdx = 0;
    int pageSize = 20;
    int maxUsers = 10000;
    int maxPages = maxUsers / pageSize;
    int numUserMembershipCreated = 0;
    int numUserMetadataCreated = 0;

    while (pageIdx < maxPages) {
      Pageable pageable = PageRequest.of(pageIdx, pageSize);
      Query query = new Query().with(pageable);
      List<UserMembershipOld> oldUserMemberships = mongoTemplate.find(query, UserMembershipOld.class);
      if (oldUserMemberships.isEmpty()) {
        break;
      }

      List<UserMetadata> userMetadataList = oldUserMemberships.stream()
                                                .map(userMembership
                                                    -> UserMetadata.builder()
                                                           .userId(userMembership.getUserId())
                                                           .email(userMembership.getEmailId())
                                                           .name(userMembership.getName())
                                                           .build())
                                                .collect(Collectors.toList());
      try {
        numUserMetadataCreated += userMetadataRepository.insertAllIgnoringDuplicates(userMetadataList);
      } catch (DuplicateKeyException e) {
        // this would happen when migration is run for the second time
      } catch (Exception e) {
        log.error("Couldn't save UserMetadata", e);
      }

      List<UserMembership> userMembershipList = new ArrayList<>();
      for (UserMembershipOld oldUserMembership : oldUserMemberships) {
        for (Scope scope : oldUserMembership.getScopes()) {
          userMembershipList.add(UserMembership.builder()
                                     .userId(oldUserMembership.getUserId())
                                     .scope(Scope.builder()
                                                .accountIdentifier(scope.getAccountIdentifier())
                                                .orgIdentifier(scope.getOrgIdentifier())
                                                .projectIdentifier(scope.getProjectIdentifier())
                                                .build())
                                     .build());
        }
      }

      try {
        if (!userMembershipList.isEmpty()) {
          numUserMembershipCreated += userMembershipRepository.insertAllIgnoringDuplicates(userMembershipList);
        }
      } catch (DuplicateKeyException e) {
        log.error("DuplicateKeyException...{}", e);
        // this would happen when migration is run for the second time
      } catch (Exception e) {
        log.error("Couldn't save UserMembership", e);
      }
      pageIdx++;
    }
    log.info("UserMembership and UserMetadata migration completed");
    log.info("Total {} UserMemberships created", numUserMembershipCreated);
    log.info("Total {} UserMetadata created", numUserMetadataCreated);
  }
}

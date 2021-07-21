package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.repositories.user.spring.UserMetadataRepository;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PL)
@Slf4j
public class UserMetadataMigrationService implements NGMigration {
  private final MongoTemplate mongoTemplate;
  private final UserMetadataRepository userMetadataRepository;
  private final NgUserService ngUserService;

  @Inject
  public UserMetadataMigrationService(
      MongoTemplate mongoTemplate, UserMetadataRepository userMetadataRepository, NgUserService ngUserService) {
    this.mongoTemplate = mongoTemplate;
    this.userMetadataRepository = userMetadataRepository;
    this.ngUserService = ngUserService;
  }

  public void migrate() {
    int pageIdx = 0;
    int pageSize = 20;
    int maxUsers = 10000;
    int maxPages = maxUsers / pageSize;
    int numNewUserMetadataCreated = 0;

    while (pageIdx < maxPages) {
      Pageable pageable = PageRequest.of(pageIdx, pageSize);
      Query query = new Query().with(pageable);
      List<UserMetadata> oldUserMetadataList = mongoTemplate.find(query, UserMetadata.class);
      if (oldUserMetadataList.isEmpty()) {
        break;
      }

      List<UserMetadata> newUserMetadataList = oldUserMetadataList.stream()
                                                   .map(oldUserMetadata
                                                       -> UserMetadata.builder()
                                                              .userId(oldUserMetadata.getUserId())
                                                              .email(oldUserMetadata.getEmail())
                                                              .name(oldUserMetadata.getName())
                                                              .locked(getLockedStatus(oldUserMetadata))
                                                              .build())
                                                   .collect(Collectors.toList());
      try {
        numNewUserMetadataCreated += userMetadataRepository.insertAllIgnoringDuplicates(newUserMetadataList);
      } catch (DuplicateKeyException e) {
        // this would happen when migration is run for the second time
      } catch (Exception e) {
        log.error("Couldn't save UserMetadata", e);
      }

      pageIdx++;
    }
    log.info("UserMetadata migration completed");
    log.info("Total {} UserMetadata created", numNewUserMetadataCreated);
  }

  private boolean getLockedStatus(UserMetadata userMetadata) {
    String userId = userMetadata.getUserId();
    Optional<UserInfo> userInfoOptional = ngUserService.getUserById(userId);
    if (userInfoOptional.isPresent()) {
      UserInfo userInfo = userInfoOptional.get();
      return userInfo.isLocked();
    }
    return false;
  }
}

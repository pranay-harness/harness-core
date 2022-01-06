/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.migration.NGMigration;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.repositories.entitysetupusage.EntitySetupUsageRepository;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PL)
public class YamlGitConfigMigration implements NGMigration {
  private final MongoTemplate mongoTemplate;
  private final IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  private final EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  private final EntitySetupUsageRepository entitySetupUsageRepository;

  @Override
  public void migrate() {
    int pageIdx = 0;
    int pageSize = 20;
    int maxGitConfigs = 10000;
    int maxPages = maxGitConfigs / pageSize;
    int numSetupUsageCreated = 0;

    while (pageIdx < maxPages) {
      Pageable pageable = PageRequest.of(pageIdx, pageSize);
      Query query = new Query().with(pageable);
      List<YamlGitConfig> yamlGitConfigs = mongoTemplate.find(query, YamlGitConfig.class);
      if (yamlGitConfigs.isEmpty()) {
        break;
      }

      Set<EntitySetupUsage> entitySetupUsages = getEntitySetupUsageList(yamlGitConfigs);
      try {
        entitySetupUsageRepository.saveAll(entitySetupUsages);
        numSetupUsageCreated += entitySetupUsages.size();
      } catch (DuplicateKeyException ex) {
        // this would happen when migration is run for the second time
      } catch (Exception ex) {
        log.error("Couldn't create setup usage", ex);
      }
      pageIdx++;
      if (pageIdx % (maxPages / 5) == 0) {
        log.info("yamlGitConfig migration in process...");
      }
    }

    log.info("yamlGitConfig Migration Completed");
    log.info("Total {} entitySetupUsages created", numSetupUsageCreated);
  }

  private Set<EntitySetupUsage> getEntitySetupUsageList(List<YamlGitConfig> yamlGitConfigs) {
    Set<EntitySetupUsage> entitySetupUsages = new HashSet<>();
    for (YamlGitConfig gitSyncConfig : yamlGitConfigs) {
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(gitSyncConfig.getGitConnectorRef(),
          gitSyncConfig.getAccountId(), gitSyncConfig.getOrgIdentifier(), gitSyncConfig.getProjectIdentifier());
      IdentifierRefProtoDTO yamlGitConfigReference =
          identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(gitSyncConfig.getAccountId(),
              gitSyncConfig.getOrgIdentifier(), gitSyncConfig.getProjectIdentifier(), gitSyncConfig.getIdentifier());
      IdentifierRefProtoDTO connectorReference =
          identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
              identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
      EntityDetailProtoDTO yamlGitConfigDetails = EntityDetailProtoDTO.newBuilder()
                                                      .setIdentifierRef(yamlGitConfigReference)
                                                      .setType(EntityTypeProtoEnum.GIT_REPOSITORIES)
                                                      .setName(gitSyncConfig.getName())
                                                      .build();
      EntityDetailProtoDTO connectorDetails = EntityDetailProtoDTO.newBuilder()
                                                  .setIdentifierRef(connectorReference)
                                                  .setType(EntityTypeProtoEnum.CONNECTORS)
                                                  .build();
      final EntityDetail referredByEntity = entityDetailProtoToRestMapper.createEntityDetailDTO(yamlGitConfigDetails);
      final EntityDetail referredEntity = entityDetailProtoToRestMapper.createEntityDetailDTO(connectorDetails);
      entitySetupUsages.add(EntitySetupUsage.builder()
                                .accountIdentifier(gitSyncConfig.getAccountId())
                                .referredByEntity(referredByEntity)
                                .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                                .referredByEntityType(referredByEntity.getType().toString())
                                .referredEntityFQN(referredEntity.getEntityRef().getFullyQualifiedName())
                                .referredEntityType(referredEntity.getType().toString())
                                .referredEntity(referredEntity)
                                .build());
    }
    return entitySetupUsages;
  }
}

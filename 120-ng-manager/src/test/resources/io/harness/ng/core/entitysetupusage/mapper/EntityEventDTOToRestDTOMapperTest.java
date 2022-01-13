/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage.mapper;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.SECRETS;
import static io.harness.encryption.Scope.PROJECT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class EntityEventDTOToRestDTOMapperTest extends CategoryTest {
  @InjectMocks EntitySetupUsageEventDTOMapper entitySetupUsageEventDTOToRestDTOMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toRestDTO() {
    String accountIdentifier = "accountIdentifier";
    String projectIdentifier = "projectIdentifier";
    String orgIdentifier = "orgIdentifier";
    String referredByEntityIdentifier = "referredByEntityIdentifier";
    String referredByEntityName = "Pipeline 1";
    String referredEntityIdentifier = "referredEntityIdentifier";
    String referredEntityName = "Connector 1";
    IdentifierRefProtoDTO referredEntityRef = IdentifierRefProtoDTO.newBuilder()
                                                  .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                  .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                  .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                  .setIdentifier(StringValue.of(referredEntityIdentifier))
                                                  .setScope(ScopeProtoEnum.PROJECT)
                                                  .build();
    IdentifierRefProtoDTO referredByEntityRef = IdentifierRefProtoDTO.newBuilder()
                                                    .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                    .setOrgIdentifier(StringValue.of(orgIdentifier))
                                                    .setProjectIdentifier(StringValue.of(projectIdentifier))
                                                    .setIdentifier(StringValue.of(referredByEntityIdentifier))
                                                    .setScope(ScopeProtoEnum.PROJECT)
                                                    .build();
    EntityDetailProtoDTO referredByEntity = EntityDetailProtoDTO.newBuilder()
                                                .setIdentifierRef(referredByEntityRef)
                                                .setType(EntityTypeProtoEnum.SECRETS)
                                                .setName(referredByEntityName)
                                                .build();
    EntityDetailProtoDTO referredEntity = EntityDetailProtoDTO.newBuilder()
                                              .setIdentifierRef(referredEntityRef)
                                              .setType(EntityTypeProtoEnum.CONNECTORS)
                                              .setName(referredEntityName)
                                              .build();
    long time = System.currentTimeMillis();
    EntitySetupUsageCreateDTO entitySetupUsageDTO = EntitySetupUsageCreateDTO.newBuilder()
                                                        .setAccountIdentifier(accountIdentifier)
                                                        .setCreatedAt(time)
                                                        .setReferredEntity(referredEntity)
                                                        .setReferredByEntity(referredByEntity)
                                                        .build();
    EntitySetupUsageDTO entitySetupUsage = entitySetupUsageEventDTOToRestDTOMapper.toRestDTO(entitySetupUsageDTO);
    assertThat(entitySetupUsage).isNotNull();
    assertThat(entitySetupUsage.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(entitySetupUsage.getReferredEntity().getName()).isEqualTo(referredEntityName);
    assertThat(entitySetupUsage.getReferredEntity().getType()).isEqualTo(CONNECTORS);

    IdentifierRef referredIdentifierRef = IdentifierRef.builder()
                                              .accountIdentifier(accountIdentifier)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .identifier(referredEntityIdentifier)
                                              .scope(PROJECT)
                                              .build();
    assertThat(entitySetupUsage.getReferredEntity().getEntityRef()).isEqualTo(referredIdentifierRef);
    assertThat(entitySetupUsage.getReferredByEntity().getName()).isEqualTo(referredByEntityName);
    assertThat(entitySetupUsage.getReferredByEntity().getType()).isEqualTo(SECRETS);

    IdentifierRef referredByIdentifierRef = IdentifierRef.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .identifier(referredByEntityIdentifier)
                                                .scope(PROJECT)
                                                .build();
    assertThat(entitySetupUsage.getReferredByEntity().getEntityRef()).isEqualTo(referredByIdentifierRef);
    assertThat(entitySetupUsage.getAccountIdentifier()).isEqualTo(accountIdentifier);
  }
}

package io.harness.ng.core.activityhistory.mapper;

import static io.harness.EntityType.CONNECTORS;
import static io.harness.EntityType.PIPELINES;
import static io.harness.encryption.Scope.PROJECT;
import static io.harness.ng.core.activityhistory.NGActivityStatus.SUCCESS;
import static io.harness.ng.core.activityhistory.NGActivityType.CONNECTIVITY_CHECK;
import static io.harness.ng.core.activityhistory.NGActivityType.ENTITY_USAGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.entity.ConnectivityCheckDetail;
import io.harness.ng.core.activityhistory.entity.EntityUsageActivityDetail;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class NGActivityEntityToDTOMapperTest extends CategoryTest {
  String referredEntityFQN = "referredEntityFQN";
  String referredEntityOrgIdentifier = "referredEntityOrgIdentifier";
  String referredEntityProjIdentifier = "referredEntityProjIdentifier";
  String referredEntityIdentifier = "referredEntityIdentifier";
  String errorMessage = "errorMessage";
  String activityDescription = "activityDescription";
  String accountIdentifier = "accountIdentifier";
  String referredByEntityFQN = "referredByEntityFQN";
  String referredByEntityIdentifier = "referredByEntityIdentifier";
  String referredByEntityOrgIdentifier = "referredByEntityOrgIdentifier";
  String referredByEntityProjectIdentifier = "referredByEntityProjectIdentifier";
  long activityTime = System.currentTimeMillis();

  @InjectMocks NGActivityEntityToDTOMapper activityEntityToDTOMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeDTOForConenctivityCheckActivity() {
    NGActivity activityEntity = ConnectivityCheckDetail.builder().build();
    setCommonFieldsOfActivityHistory(activityEntity, CONNECTIVITY_CHECK);
    NGActivityDTO activityHistoryDTO = activityEntityToDTOMapper.writeDTO(activityEntity);
    verifyTheActivityHistoryDetails(activityHistoryDTO, CONNECTIVITY_CHECK);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void writeDTOForEntityUsedActivity() {
    NGActivity activityEntity = EntityUsageActivityDetail.builder()
                                    .referredByEntityOrgIdentifier(referredByEntityOrgIdentifier)
                                    .referredByEntityProjectIdentifier(referredByEntityProjectIdentifier)
                                    .referredByEntityIdentifier(referredByEntityIdentifier)
                                    .referredByEntityScope(PROJECT.toString())
                                    .referredByEntityFQN(referredByEntityFQN)
                                    .referredByEntityType(PIPELINES.toString())
                                    .build();
    setCommonFieldsOfActivityHistory(activityEntity, ENTITY_USAGE);
    NGActivityDTO activityHistoryDTO = activityEntityToDTOMapper.writeDTO(activityEntity);
    verifyTheActivityHistoryDetails(activityHistoryDTO, ENTITY_USAGE);
    EntityUsageActivityDetailDTO activityDetailDTO = (EntityUsageActivityDetailDTO) activityHistoryDTO.getDetail();
    assertThat(activityDetailDTO.getReferredByEntityOrgIdentifier()).isEqualTo(referredByEntityOrgIdentifier);
    assertThat(activityDetailDTO.getReferredByEntityProjectIdentifier()).isEqualTo(referredByEntityProjectIdentifier);
    assertThat(activityDetailDTO.getReferredByEntityIdentifier()).isEqualTo(referredByEntityIdentifier);
    assertThat(activityDetailDTO.getReferredByEntityType()).isEqualTo(PIPELINES);
  }

  private void setCommonFieldsOfActivityHistory(NGActivity activityEntity, NGActivityType ngActivityType) {
    activityEntity.setReferredEntityType(CONNECTORS.toString());
    activityEntity.setReferredEntityFQN(referredEntityFQN);
    activityEntity.setType(ngActivityType.toString());
    activityEntity.setErrorMessage(errorMessage);
    activityEntity.setDescription(activityDescription);
    activityEntity.setActivityTime(activityTime);
    activityEntity.setAccountIdentifier(accountIdentifier);
    activityEntity.setActivityStatus(SUCCESS.toString());
    activityEntity.setReferredEntityScope(PROJECT.toString());
    activityEntity.setReferredEntityOrgIdentifier(referredEntityOrgIdentifier);
    activityEntity.setReferredEntityProjectIdentifier(referredEntityProjIdentifier);
    activityEntity.setReferredEntityIdentifier(referredEntityIdentifier);
  }

  private void verifyTheActivityHistoryDetails(NGActivityDTO activityHistoryDTO, NGActivityType ngActivityType) {
    assertThat(activityHistoryDTO).isNotNull();
    assertThat(activityHistoryDTO.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(activityHistoryDTO.getType()).isEqualTo(ngActivityType);
    assertThat(activityHistoryDTO.getActivityTime()).isEqualTo(activityTime);
    assertThat(activityHistoryDTO.getDescription()).isEqualTo(activityDescription);
    assertThat(activityHistoryDTO.getErrorMessage()).isEqualTo(errorMessage);
    assertThat(activityHistoryDTO.getReferredEntityType()).isEqualTo(CONNECTORS);
    assertThat(activityHistoryDTO.getActivityStatus()).isEqualTo(SUCCESS);
    assertThat(activityHistoryDTO.getReferredEntityOrgIdentifier()).isEqualTo(referredEntityOrgIdentifier);
    assertThat(activityHistoryDTO.getReferredEntityIdentifier()).isEqualTo(referredEntityIdentifier);
    assertThat(activityHistoryDTO.getReferredEntityProjectIdentifier()).isEqualTo(referredEntityProjIdentifier);
    assertThat(activityHistoryDTO.getReferredEntityScope()).isEqualTo(PROJECT);
  }
}
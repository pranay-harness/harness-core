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
import io.harness.ng.core.activityhistory.dto.ActivityDetail;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.entity.EntityUsageActivityDetail;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class NGActivityDTOToEntityMapperTest extends CategoryTest {
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

  @InjectMocks NGActivityDTOToEntityMapper activityDTOToEntityMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toActivityHistoryEntityForConnectivityCheckActivity() {
    ConnectivityCheckActivityDetailDTO connectivityCheckActivity = ConnectivityCheckActivityDetailDTO.builder().build();
    NGActivityDTO activityHistoryDTO = createActivityHistoryDTO(connectivityCheckActivity, CONNECTIVITY_CHECK);
    NGActivity activityEntity = activityDTOToEntityMapper.toActivityHistoryEntity(activityHistoryDTO);
    verifyActivityHistoryEntityValues(activityEntity, CONNECTIVITY_CHECK);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void toActivityHistoryEntityForEntityUsageActivity() {
    EntityUsageActivityDetailDTO entityUsageActivityDetailDTO =
        EntityUsageActivityDetailDTO.builder()
            .referredByEntityType(PIPELINES)
            .referredByEntityScope(PROJECT)
            .referredByEntityOrgIdentifier(referredByEntityOrgIdentifier)
            .referredByEntityProjectIdentifier(referredByEntityProjectIdentifier)
            .referredByEntityIdentifier(referredByEntityIdentifier)
            .build();
    NGActivityDTO activityHistoryDTO = createActivityHistoryDTO(entityUsageActivityDetailDTO, ENTITY_USAGE);
    NGActivity activityEntity = activityDTOToEntityMapper.toActivityHistoryEntity(activityHistoryDTO);
    EntityUsageActivityDetail entityUsageActivityDetailEntity = (EntityUsageActivityDetail) activityEntity;
    verifyActivityHistoryEntityValues(activityEntity, ENTITY_USAGE);
    assertThat(entityUsageActivityDetailEntity.getReferredByEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
            referredByEntityOrgIdentifier, referredByEntityProjectIdentifier, referredByEntityIdentifier));
    assertThat(entityUsageActivityDetailEntity.getReferredByEntityType()).isEqualTo(PIPELINES.toString());
    assertThat(entityUsageActivityDetailEntity.getReferredByEntityOrgIdentifier())
        .isEqualTo(referredByEntityOrgIdentifier);
    assertThat(entityUsageActivityDetailEntity.getReferredByEntityProjectIdentifier())
        .isEqualTo(referredByEntityProjectIdentifier);
    assertThat(entityUsageActivityDetailEntity.getReferredByEntityIdentifier()).isEqualTo(referredByEntityIdentifier);
  }

  private NGActivityDTO createActivityHistoryDTO(ActivityDetail activityDetail, NGActivityType ngActivityType) {
    return NGActivityDTO.builder()
        .accountIdentifier(accountIdentifier)
        .referredEntityOrgIdentifier(referredEntityOrgIdentifier)
        .referredEntityProjectIdentifier(referredEntityProjIdentifier)
        .referredEntityIdentifier(referredEntityIdentifier)
        .referredEntityScope(PROJECT)
        .activityStatus(SUCCESS)
        .activityTime(activityTime)
        .description(activityDescription)
        .detail(activityDetail)
        .referredEntityType(CONNECTORS)
        .errorMessage(errorMessage)
        .referredEntityScope(PROJECT)
        .type(ngActivityType)
        .build();
  }

  private void verifyActivityHistoryEntityValues(NGActivity activityEntity, NGActivityType ngActivityType) {
    assertThat(activityEntity).isNotNull();
    assertThat(activityEntity.getAccountIdentifier()).isEqualTo(accountIdentifier);
    assertThat(activityEntity.getActivityTime()).isEqualTo(activityTime);
    assertThat(activityEntity.getDescription()).isEqualTo(activityDescription);
    assertThat(activityEntity.getErrorMessage()).isEqualTo(errorMessage);
    assertThat(activityEntity.getReferredEntityType()).isEqualTo(CONNECTORS.toString());
    assertThat(activityEntity.getType()).isEqualTo(ngActivityType.toString());
    assertThat(activityEntity.getActivityStatus()).isEqualTo(SUCCESS.toString());
    assertThat(activityEntity.getReferredEntityScope()).isEqualTo(PROJECT.toString());
    assertThat(activityEntity.getReferredEntityFQN())
        .isEqualTo(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, referredEntityOrgIdentifier, referredEntityProjIdentifier, referredEntityIdentifier));
    assertThat(activityEntity.getReferredEntityIdentifier()).isEqualTo(referredEntityIdentifier);
    assertThat(activityEntity.getReferredEntityOrgIdentifier()).isEqualTo(referredEntityOrgIdentifier);
    assertThat(activityEntity.getReferredEntityProjectIdentifier()).isEqualTo(referredEntityProjIdentifier);
  }
}
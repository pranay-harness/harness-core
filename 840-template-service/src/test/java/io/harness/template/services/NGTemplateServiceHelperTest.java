package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.rule.Owner;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.gitsync.TemplateGitSyncBranchContextGuard;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDC)
public class NGTemplateServiceHelperTest extends CategoryTest {
  NGTemplateServiceHelper templateServiceHelper;
  @Mock FilterService filterService;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String TEMPLATE_IDENTIFIER = "template1";
  private final String TEMPLATE_VERSION_LABEL = "version1";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    templateServiceHelper = new NGTemplateServiceHelper(filterService);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> NGTemplateServiceHelper.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetTemplateGitContext() {
    TemplateEntity entity = TemplateEntity.builder()
                                .accountId(ACCOUNT_ID)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJ_IDENTIFIER)
                                .identifier(TEMPLATE_IDENTIFIER)
                                .versionLabel(TEMPLATE_VERSION_LABEL)
                                .build();
    try (TemplateGitSyncBranchContextGuard ignored = templateServiceHelper.getTemplateGitContext(entity, null, "")) {
      GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
      assertThat(gitEntityInfo).isNotNull();
      assertThat(gitEntityInfo.getBranch()).isEqualTo("");
      assertThat(gitEntityInfo.getYamlGitConfigId()).isEqualTo("");
      assertThat(gitEntityInfo.getCommitMsg()).isEqualTo("");
      assertThat(gitEntityInfo.getFilePath()).isNull();
      assertThat(gitEntityInfo.getFolderPath()).isNull();
    }
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testFormCriteriaUsingFilterIdentifier() {
    String filterIdentifier = "filterIdentifier";
    FilterDTO filterDTO = FilterDTO.builder()
                              .filterProperties(TemplateFilterPropertiesDTO.builder()
                                                    .templateIdentifiers(Collections.singletonList(TEMPLATE_IDENTIFIER))
                                                    .templateNames(Collections.singletonList(TEMPLATE_IDENTIFIER))
                                                    .build())
                              .build();
    doReturn(filterDTO)
        .when(filterService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, filterIdentifier, FilterType.TEMPLATE);

    Criteria criteria = templateServiceHelper.formCriteria(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, filterIdentifier, null, false, "");
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.get(TemplateEntityKeys.accountId)).isEqualTo(ACCOUNT_ID);
    assertThat(criteriaObject.get(TemplateEntityKeys.orgIdentifier)).isEqualTo(ORG_IDENTIFIER);
    assertThat(criteriaObject.get(TemplateEntityKeys.projectIdentifier)).isEqualTo(PROJ_IDENTIFIER);
    assertThat(criteriaObject.get(TemplateEntityKeys.deleted)).isEqualTo(false);

    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(TemplateEntityKeys.identifier)).get("$in")).size())
        .isEqualTo(1);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(TemplateEntityKeys.identifier)).get("$in"))
                   .contains(TEMPLATE_IDENTIFIER));
    assertThat(
        ((Pattern) ((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(0)).get("$or")).get(0))
                .get("name"))
            .pattern())
        .isEqualTo("template1");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testFormCriteriaUsingFilterDto() {
    TemplateFilterPropertiesDTO propertiesDTO = TemplateFilterPropertiesDTO.builder()
                                                    .templateNames(Collections.singletonList(TEMPLATE_IDENTIFIER))
                                                    .description("random")
                                                    .build();
    Criteria criteria = templateServiceHelper.formCriteria(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "", propertiesDTO, false, TEMPLATE_IDENTIFIER);
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.get(TemplateEntityKeys.accountId)).isEqualTo(ACCOUNT_ID);
    assertThat(criteriaObject.get(TemplateEntityKeys.orgIdentifier)).isEqualTo(ORG_IDENTIFIER);
    assertThat(criteriaObject.get(TemplateEntityKeys.projectIdentifier)).isEqualTo(PROJ_IDENTIFIER);
    assertThat(criteriaObject.get(TemplateEntityKeys.deleted)).isEqualTo(false);

    assertThat(
        ((Pattern) ((Document) ((List<?>) criteriaObject.get("$and")).get(1)).get(TemplateEntityKeys.description))
            .pattern())
        .isEqualTo("random");
    assertThat(((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).size()).isEqualTo(6);
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(0))
                   .get(TemplateEntityKeys.identifier))
        .isNotNull();
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(1))
                   .get(TemplateEntityKeys.name))
        .isNotNull();
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(2))
                   .get(TemplateEntityKeys.versionLabel))
        .isNotNull();
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(3))
                   .get(TemplateEntityKeys.description))
        .isNotNull();
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(4))
                   .get("tags.key"))
        .isNotNull();
    assertThat(((Document) ((List<?>) ((Document) ((List<?>) criteriaObject.get("$and")).get(2)).get("$or")).get(5))
                   .get("tags.value"))
        .isNotNull();
  }
}
package io.harness.ng.core.environment.services.impl;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGCoreBaseTest;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public class EnvironmentServiceImplTest extends NGCoreBaseTest {
  @Inject EnvironmentServiceImpl environmentService;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> environmentService.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testEnvironmentServiceLayer() {
    Environment createEnvironmentRequest = Environment.builder()
                                               .accountId("ACCOUNT_ID")
                                               .identifier("IDENTIFIER")
                                               .orgIdentifier("ORG_ID")
                                               .projectIdentifier("PROJECT_ID")
                                               .build();

    // Create operations
    Environment createdEnvironment = environmentService.create(createEnvironmentRequest);
    assertThat(createdEnvironment).isNotNull();
    assertThat(createdEnvironment.getAccountId()).isEqualTo(createEnvironmentRequest.getAccountId());
    assertThat(createdEnvironment.getOrgIdentifier()).isEqualTo(createEnvironmentRequest.getOrgIdentifier());
    assertThat(createdEnvironment.getProjectIdentifier()).isEqualTo(createEnvironmentRequest.getProjectIdentifier());
    assertThat(createdEnvironment.getIdentifier()).isEqualTo(createEnvironmentRequest.getIdentifier());
    assertThat(createdEnvironment.getName()).isEqualTo(createEnvironmentRequest.getIdentifier());

    // Get Operations
    Optional<Environment> getEnvironment = environmentService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "IDENTIFIER");
    assertThat(getEnvironment).isPresent();
    assertThat(getEnvironment.get()).isEqualTo(createdEnvironment);

    // Update Operations
    Environment updateEnvironmentRequest = Environment.builder()
                                               .accountId("ACCOUNT_ID")
                                               .identifier("IDENTIFIER")
                                               .orgIdentifier("ORG_ID")
                                               .projectIdentifier("PROJECT_ID")
                                               .name("UPDATED_ENV")
                                               .build();

    Environment updatedEnvironment = environmentService.update(updateEnvironmentRequest);
    assertThat(updatedEnvironment).isNotNull();
    assertThat(updatedEnvironment.getAccountId()).isEqualTo(updateEnvironmentRequest.getAccountId());
    assertThat(updatedEnvironment.getOrgIdentifier()).isEqualTo(updateEnvironmentRequest.getOrgIdentifier());
    assertThat(updatedEnvironment.getProjectIdentifier()).isEqualTo(updateEnvironmentRequest.getProjectIdentifier());
    assertThat(updatedEnvironment.getIdentifier()).isEqualTo(updateEnvironmentRequest.getIdentifier());
    assertThat(updatedEnvironment.getName()).isEqualTo(updateEnvironmentRequest.getName());
    assertThat(updatedEnvironment.getId()).isEqualTo(createdEnvironment.getId());

    updateEnvironmentRequest.setIdentifier("NEW_ENV");
    assertThatThrownBy(() -> environmentService.update(updateEnvironmentRequest))
        .isInstanceOf(InvalidRequestException.class);

    // Upsert operations
    Environment upsertEnvironmentRequest = Environment.builder()
                                               .accountId("ACCOUNT_ID")
                                               .identifier("NEW_ENV")
                                               .orgIdentifier("ORG_ID")
                                               .projectIdentifier("NEW_PROJECT")
                                               .name("UPSERTED_ENV")
                                               .build();
    Environment upsertEnv = environmentService.upsert(upsertEnvironmentRequest);
    assertThat(upsertEnv).isNotNull();
    assertThat(upsertEnv.getAccountId()).isEqualTo(upsertEnvironmentRequest.getAccountId());
    assertThat(upsertEnv.getOrgIdentifier()).isEqualTo(upsertEnvironmentRequest.getOrgIdentifier());
    assertThat(upsertEnv.getProjectIdentifier()).isEqualTo(upsertEnvironmentRequest.getProjectIdentifier());
    assertThat(upsertEnv.getIdentifier()).isEqualTo(upsertEnvironmentRequest.getIdentifier());
    assertThat(upsertEnv.getName()).isEqualTo(upsertEnvironmentRequest.getName());

    // List services operations.
    Criteria criteriaFromFilter = EnvironmentFilterHelper.createCriteria("ACCOUNT_ID", "ORG_ID", "PROJECT_ID");
    Pageable pageRequest = PageUtils.getPageRequest(0, 100, null);
    Page<Environment> list = environmentService.list(criteriaFromFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(1);
    assertThat(list.getContent().get(0)).isEqualTo(updatedEnvironment);

    criteriaFromFilter = EnvironmentFilterHelper.createCriteria("ACCOUNT_ID", "ORG_ID", null);
    pageRequest = PageUtils.getPageRequest(0, 100, null);

    list = environmentService.list(criteriaFromFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(list.getContent()).containsOnly(updatedEnvironment, upsertEnv);

    // Delete operations
    boolean delete = environmentService.delete("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "UPDATED_ENV");
    assertThat(delete).isTrue();

    Optional<Environment> deletedEnvironment =
        environmentService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "UPDATED_ENV");
    assertThat(deletedEnvironment.isPresent()).isFalse();
  }
}
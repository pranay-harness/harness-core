package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.dto.CreateOrganizationRequest;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationRequest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;

public class OrganizationResourceTest extends BaseTest {
  @Inject private OrganizationResource organizationResource;

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    CreateOrganizationRequest request = random(CreateOrganizationRequest.class);

    OrganizationDTO organizationDto = organizationResource.createOrganization(request);

    assertThat(organizationDto).isNotNull();
    assertThat(organizationDto.getId()).isNotNull();
    assertThat(organizationResource.getOrganization(organizationDto.getId()).orElse(null)).isEqualTo(organizationDto);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateOrganization() {
    CreateOrganizationRequest createRequest = random(CreateOrganizationRequest.class);
    OrganizationDTO createdOrganization = organizationResource.createOrganization(createRequest);
    String organizationId = createdOrganization.getId();
    UpdateOrganizationRequest updateRequest = random(UpdateOrganizationRequest.class);

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.updateOrganization(organizationId, updateRequest);

    OrganizationDTO updatedOrganization = organizationResource.getOrganization(organizationId).orElse(null);
    assertThat(updatedOrganization).isNotNull();
    assertThat(updateRequest.getColor()).isEqualTo(updatedOrganization.getColor());
    assertThat(updateRequest.getDescription()).isEqualTo(updatedOrganization.getDescription());
    assertThat(updateRequest.getTags()).isEqualTo(updatedOrganization.getTags());
    assertThat(updateRequest.getName()).isEqualTo(updatedOrganization.getName());

    assertThat(updatedOrganization.getAccountId()).isEqualTo(createdOrganization.getAccountId());
    assertThat(updatedOrganization.getIdentifier()).isEqualTo(createdOrganization.getIdentifier());
    assertThat(updatedOrganization.getId()).isEqualTo(createdOrganization.getId());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateNonExistentOrganization() {
    UpdateOrganizationRequest updateRequest = random(UpdateOrganizationRequest.class);

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.updateOrganization(randomAlphabetic(10), updateRequest);

    assertThat(updatedOrganizationOptional).isNotPresent();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListOrganizations() {
    String accountId = randomAlphabetic(10);
    assertThat(organizationResource.getOrganizationsForAccount(accountId)).isEmpty();

    CreateOrganizationRequest createRequest = random(CreateOrganizationRequest.class);
    createRequest.setAccountId(accountId);
    OrganizationDTO createdOrganization = organizationResource.createOrganization(createRequest);

    assertThat(organizationResource.getOrganizationsForAccount(accountId)).isNotEmpty();
    assertThat(organizationResource.getOrganizationsForAccount(accountId)).hasSize(1);
  }
}

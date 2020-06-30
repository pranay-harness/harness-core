package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Optional;

public class OrganizationResourceTest extends BaseTest {
  @Inject private OrganizationResource organizationResource;

  private String getAccountIdentifier() {
    return random(String.class);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    CreateOrganizationDTO createOrganizationDTO = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();

    OrganizationDTO organizationDto = organizationResource.create(accountIdentifier, createOrganizationDTO).getData();

    assertThat(organizationDto).isNotNull();
    assertThat(organizationDto.getId()).isNotNull();
    assertThat(organizationResource.get(accountIdentifier, organizationDto.getIdentifier()).getData().orElse(null))
        .isEqualTo(organizationDto);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateOrganization() {
    CreateOrganizationDTO createRequest = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();

    OrganizationDTO createdOrganization = organizationResource.create(accountIdentifier, createRequest).getData();
    String organizationIdentifier = createdOrganization.getIdentifier();
    UpdateOrganizationDTO updateOrganizationDTO = random(UpdateOrganizationDTO.class);

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.update(accountIdentifier, organizationIdentifier, updateOrganizationDTO).getData();

    OrganizationDTO updatedOrganization =
        organizationResource.get(accountIdentifier, organizationIdentifier).getData().orElse(null);
    assertThat(updatedOrganization).isNotNull();
    assertThat(updateOrganizationDTO.getColor()).isEqualTo(updatedOrganization.getColor());
    assertThat(updateOrganizationDTO.getDescription()).isEqualTo(updatedOrganization.getDescription());
    assertThat(updateOrganizationDTO.getTags()).isEqualTo(updatedOrganization.getTags());
    assertThat(updateOrganizationDTO.getName()).isEqualTo(updatedOrganization.getName());

    assertThat(updatedOrganization.getAccountIdentifier()).isEqualTo(createdOrganization.getAccountIdentifier());
    assertThat(updatedOrganization.getIdentifier()).isEqualTo(createdOrganization.getIdentifier());
    assertThat(updatedOrganization.getId()).isEqualTo(createdOrganization.getId());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateNonExistentOrganization() {
    UpdateOrganizationDTO updateRequest = random(UpdateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.update(accountIdentifier, randomAlphabetic(10), updateRequest).getData();

    assertThat(updatedOrganizationOptional).isNotPresent();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListOrganizations() {
    String accountIdentifier = randomAlphabetic(10);
    assertThat(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData()).isEmpty();

    CreateOrganizationDTO firstOrganizationDTO = random(CreateOrganizationDTO.class);
    OrganizationDTO firstOrganization = organizationResource.create(accountIdentifier, firstOrganizationDTO).getData();

    assertThat(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData()).isNotEmpty();
    assertThat(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData()).hasSize(1);

    CreateOrganizationDTO secondOrganizationDTO = random(CreateOrganizationDTO.class);
    String secondOrgIdentifier = randomAlphabetic(10);
    secondOrganizationDTO.setIdentifier(secondOrgIdentifier);
    OrganizationDTO secondOrganization =
        organizationResource.create(accountIdentifier, secondOrganizationDTO).getData();

    Page<OrganizationDTO> organizationList =
        organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData();

    assertThat(organizationList.getTotalElements()).isEqualTo(2);
    assertThat(organizationList.getContent()).isNotNull();
    assertThat(organizationList.getContent().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    CreateOrganizationDTO organizationDTO = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();

    OrganizationDTO firstOrganization = organizationResource.create(accountIdentifier, organizationDTO).getData();

    boolean isDeleted = organizationResource.delete(accountIdentifier, firstOrganization.getIdentifier()).getData();
    assertThat(isDeleted).isTrue();
    assertThat(organizationResource.get(accountIdentifier, firstOrganization.getIdentifier()).getData().isPresent())
        .isFalse();
  }
}

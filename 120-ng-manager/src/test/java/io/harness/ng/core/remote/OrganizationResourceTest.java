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

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    CreateOrganizationDTO createOrganizationDTO = random(CreateOrganizationDTO.class);

    OrganizationDTO organizationDto = organizationResource.create(createOrganizationDTO).getData();

    assertThat(organizationDto).isNotNull();
    assertThat(organizationDto.getId()).isNotNull();
    assertThat(organizationResource.get(organizationDto.getId()).getData().orElse(null)).isEqualTo(organizationDto);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateOrganization() {
    CreateOrganizationDTO createRequest = random(CreateOrganizationDTO.class);
    OrganizationDTO createdOrganization = organizationResource.create(createRequest).getData();
    String organizationId = createdOrganization.getId();
    UpdateOrganizationDTO updateOrganizationDTO = random(UpdateOrganizationDTO.class);

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.update(organizationId, updateOrganizationDTO).getData();

    OrganizationDTO updatedOrganization = organizationResource.get(organizationId).getData().orElse(null);
    assertThat(updatedOrganization).isNotNull();
    assertThat(updateOrganizationDTO.getColor()).isEqualTo(updatedOrganization.getColor());
    assertThat(updateOrganizationDTO.getDescription()).isEqualTo(updatedOrganization.getDescription());
    assertThat(updateOrganizationDTO.getTags()).isEqualTo(updatedOrganization.getTags());
    assertThat(updateOrganizationDTO.getName()).isEqualTo(updatedOrganization.getName());

    assertThat(updatedOrganization.getAccountId()).isEqualTo(createdOrganization.getAccountId());
    assertThat(updatedOrganization.getIdentifier()).isEqualTo(createdOrganization.getIdentifier());
    assertThat(updatedOrganization.getId()).isEqualTo(createdOrganization.getId());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateNonExistentOrganization() {
    UpdateOrganizationDTO updateRequest = random(UpdateOrganizationDTO.class);

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.update(randomAlphabetic(10), updateRequest).getData();

    assertThat(updatedOrganizationOptional).isNotPresent();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListOrganizations() {
    String accountId = randomAlphabetic(10);
    assertThat(organizationResource.list(accountId, "", 0, 10, new ArrayList<>()).getData()).isEmpty();

    CreateOrganizationDTO firstOrganizationDTO = random(CreateOrganizationDTO.class);
    firstOrganizationDTO.setAccountId(accountId);
    OrganizationDTO firstOrganization = organizationResource.create(firstOrganizationDTO).getData();

    assertThat(organizationResource.list(accountId, "", 0, 10, new ArrayList<>()).getData()).isNotEmpty();
    assertThat(organizationResource.list(accountId, "", 0, 10, new ArrayList<>()).getData()).hasSize(1);

    CreateOrganizationDTO secondOrganizationDTO = random(CreateOrganizationDTO.class);
    String secondOrgIdentifier = "test_identifier";
    secondOrganizationDTO.setAccountId(accountId);
    secondOrganizationDTO.setIdentifier(secondOrgIdentifier);
    OrganizationDTO secondOrganization = organizationResource.create(secondOrganizationDTO).getData();

    Page<OrganizationDTO> organizationList =
        organizationResource.list(accountId, "identifier==" + secondOrgIdentifier, 0, 10, new ArrayList<>()).getData();

    assertThat(organizationList.getTotalElements()).isEqualTo(1);
    assertThat(organizationList.getContent()).isNotNull();
    assertThat(organizationList.getContent().size()).isEqualTo(1);

    OrganizationDTO fetchedOrganizationDTO = organizationList.getContent().get(0);

    assertThat(fetchedOrganizationDTO.getAccountId()).isEqualTo(accountId);
    assertThat(fetchedOrganizationDTO.getIdentifier()).isEqualTo(secondOrgIdentifier);
    assertThat(fetchedOrganizationDTO.getId()).isEqualTo(secondOrganization.getId());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    CreateOrganizationDTO organizationDTO = random(CreateOrganizationDTO.class);
    String accountId = randomAlphabetic(10);
    organizationDTO.setAccountId(accountId);
    OrganizationDTO firstOrganization = organizationResource.create(organizationDTO).getData();

    boolean isDeleted = organizationResource.delete(firstOrganization.getId()).getData();
    assertThat(isDeleted).isTrue();
    assertThat(organizationResource.get(firstOrganization.getId()).getData().isPresent()).isFalse();
  }
}

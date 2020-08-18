package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.ng.core.remote.OrganizationMapper.applyUpdateToOrganization;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.rule.Owner;
import io.harness.utils.PageTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrganizationResourceTest extends CategoryTest {
  private OrganizationService organizationService;
  private OrganizationResource organizationResource;

  private String getAccountIdentifier() {
    return random(String.class);
  }

  @Before
  public void doSetup() {
    organizationService = mock(OrganizationService.class);
    organizationResource = new OrganizationResource(organizationService);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateOrganization() {
    CreateOrganizationDTO createOrganizationDTO = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();

    Organization organization = createOrganization(accountIdentifier);

    when(organizationService.create(any(Organization.class))).thenReturn(organization);

    when(organizationService.get(accountIdentifier, organization.getIdentifier()))
        .thenReturn(Optional.of(organization));

    OrganizationDTO organizationDto = organizationResource.create(accountIdentifier, createOrganizationDTO).getData();

    assertThat(organizationDto).isNotNull();
    assertThat(organizationDto.getId()).isNotNull();
    assertThat(organizationResource.get(accountIdentifier, organizationDto.getIdentifier()).getData().orElse(null))
        .isEqualTo(organizationDto);
  }

  private Organization createOrganization(String accountIdentifier) {
    return Organization.builder()
        .accountIdentifier(accountIdentifier)
        .id(random(String.class))
        .identifier(random(String.class))
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateOrganization() {
    CreateOrganizationDTO createRequest = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();
    Organization organization = createOrganization(accountIdentifier);
    when(organizationService.create(any(Organization.class))).thenReturn(organization);
    when(organizationService.get(accountIdentifier, organization.getIdentifier()))
        .thenReturn(Optional.of(organization));

    OrganizationDTO createdOrganization = organizationResource.create(accountIdentifier, createRequest).getData();
    String organizationIdentifier = createdOrganization.getIdentifier();
    UpdateOrganizationDTO updateOrganizationDTO = random(UpdateOrganizationDTO.class);

    Organization updatedOrg = createOrganization(accountIdentifier, organizationIdentifier, updateOrganizationDTO);
    when(organizationService.update(applyUpdateToOrganization(organization, updateOrganizationDTO)))
        .thenReturn(updatedOrg);

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
  }

  private Organization createOrganization(
      String accountIdentifier, String organizationIdentifier, UpdateOrganizationDTO updateOrganizationDTO) {
    return Organization.builder()
        .accountIdentifier(accountIdentifier)
        .id(organizationIdentifier)
        .color(updateOrganizationDTO.getColor())
        .description(updateOrganizationDTO.getDescription())
        .tags(updateOrganizationDTO.getTags())
        .name(updateOrganizationDTO.getName())
        .identifier(organizationIdentifier)
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateNonExistentOrganization() {
    UpdateOrganizationDTO updateOrganizationDTO = random(UpdateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();
    when(organizationService.get(anyString(), anyString())).thenReturn(Optional.empty());

    Optional<OrganizationDTO> updatedOrganizationOptional =
        organizationResource.update(accountIdentifier, randomAlphabetic(10), updateOrganizationDTO).getData();

    assertThat(updatedOrganizationOptional).isNotPresent();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListOrganizations() {
    String accountIdentifier = randomAlphabetic(10);
    List<Organization> orgList = new ArrayList<>();

    when(organizationService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(orgList, 0));
    assertTrue(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData().isEmpty());

    orgList.add(createOrganization(accountIdentifier));

    when(organizationService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(orgList, 1));

    assertFalse(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData().isEmpty());
    assertThat(organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData().getItemCount())
        .isEqualTo(1);

    orgList.add(createOrganization(accountIdentifier));

    when(organizationService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(orgList, 2));

    NGPageResponse<OrganizationDTO> organizationList =
        organizationResource.list(accountIdentifier, 0, 10, new ArrayList<>()).getData();

    assertThat(organizationList.getItemCount()).isEqualTo(2);
    assertThat(organizationList.getContent()).isNotNull();
    assertThat(organizationList.getContent().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDelete() {
    CreateOrganizationDTO organizationDTO = random(CreateOrganizationDTO.class);
    String accountIdentifier = getAccountIdentifier();
    String orgIdentifier = randomAlphabetic(10);
    when(organizationService.delete(accountIdentifier, orgIdentifier)).thenReturn(Boolean.TRUE);

    boolean isDeleted = organizationResource.delete(accountIdentifier, orgIdentifier).getData();
    assertThat(isDeleted).isTrue();

    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.empty());

    assertThat(organizationResource.get(accountIdentifier, orgIdentifier).getData().isPresent()).isFalse();
  }
}

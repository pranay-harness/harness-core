package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.utils.PageTestUtils.getPage;

import static java.lang.Long.parseLong;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.rule.Owner;

import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

public class OrganizationResourceTest extends CategoryTest {
  private OrganizationService organizationService;
  private OrganizationResource organizationResource;

  String accountIdentifier = randomAlphabetic(10);
  String identifier = randomAlphabetic(10);
  String name = randomAlphabetic(10);

  @Before
  public void setup() {
    organizationService = mock(OrganizationService.class);
    organizationResource = new OrganizationResource(organizationService);
  }

  private OrganizationDTO getOrganizationDTO(String accountIdentifier, String identifier, String name) {
    return OrganizationDTO.builder().accountIdentifier(accountIdentifier).identifier(identifier).name(name).build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreate() {
    OrganizationDTO organizationDTO = getOrganizationDTO(accountIdentifier, identifier, name);
    Organization organization = toOrganization(organizationDTO);
    organization.setVersion((long) 0);

    when(organizationService.create(accountIdentifier, organizationDTO)).thenReturn(organization);

    ResponseDTO<OrganizationDTO> responseDTO = organizationResource.create(accountIdentifier, organizationDTO);

    assertEquals(organization.getVersion().toString(), responseDTO.getEntityTag());
    assertEquals(accountIdentifier, responseDTO.getData().getAccountIdentifier());
    assertEquals(identifier, responseDTO.getData().getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    OrganizationDTO organizationDTO = getOrganizationDTO(accountIdentifier, identifier, name);
    Organization organization = toOrganization(organizationDTO);
    organization.setVersion((long) 0);

    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.of(organization));

    ResponseDTO<OrganizationDTO> responseDTO = organizationResource.get(identifier, accountIdentifier);

    assertEquals(organization.getVersion().toString(), responseDTO.getEntityTag());
    assertEquals(accountIdentifier, responseDTO.getData().getAccountIdentifier());
    assertEquals(identifier, responseDTO.getData().getIdentifier());

    when(organizationService.get(accountIdentifier, identifier)).thenReturn(Optional.empty());

    boolean exceptionThrown = false;
    try {
      organizationResource.get(identifier, accountIdentifier);
    } catch (NotFoundException exception) {
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    String searchTerm = randomAlphabetic(10);
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    OrganizationDTO organizationDTO = getOrganizationDTO(accountIdentifier, identifier, name);
    Organization organization = toOrganization(organizationDTO);
    organization.setVersion((long) 0);
    ArgumentCaptor<OrganizationFilterDTO> argumentCaptor = ArgumentCaptor.forClass(OrganizationFilterDTO.class);

    when(organizationService.list(eq(accountIdentifier), any(), any()))
        .thenReturn(getPage(singletonList(organization), 1));

    ResponseDTO<PageResponse<OrganizationDTO>> response =
        organizationResource.list(accountIdentifier, searchTerm, pageRequest);

    verify(organizationService, times(1)).list(eq(accountIdentifier), any(), argumentCaptor.capture());
    OrganizationFilterDTO organizationFilterDTO = argumentCaptor.getValue();

    assertEquals(searchTerm, organizationFilterDTO.getSearchTerm());
    assertEquals(1, response.getData().getPageItemCount());
    assertEquals(accountIdentifier, response.getData().getContent().get(0).getAccountIdentifier());
    assertEquals(identifier, response.getData().getContent().get(0).getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    String ifMatch = "0";
    OrganizationDTO organizationDTO = getOrganizationDTO(accountIdentifier, identifier, name);
    Organization organization = toOrganization(organizationDTO);
    organization.setVersion(parseLong(ifMatch) + 1);

    when(organizationService.update(accountIdentifier, identifier, organizationDTO)).thenReturn(organization);

    ResponseDTO<OrganizationDTO> response =
        organizationResource.update(ifMatch, identifier, accountIdentifier, organizationDTO);

    assertEquals("1", response.getEntityTag());
    assertEquals(accountIdentifier, response.getData().getAccountIdentifier());
    assertEquals(identifier, response.getData().getIdentifier());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDelete() {
    String ifMatch = "0";

    when(organizationService.delete(accountIdentifier, identifier, Long.valueOf(ifMatch))).thenReturn(true);

    ResponseDTO<Boolean> response = organizationResource.delete(ifMatch, identifier, accountIdentifier);

    assertNull(response.getEntityTag());
    assertTrue(response.getData());
  }
}

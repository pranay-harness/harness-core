package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.KARAN;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.io.harness.ng.utils.PageTestUtils;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;

public class AccountOrganizationResourceTest {
  private OrganizationService organizationService;
  private AccountOrganizationResource accountOrganizationResource;

  @Before
  public void doSetup() {
    organizationService = mock(OrganizationService.class);
    accountOrganizationResource = new AccountOrganizationResource(organizationService);
  }

  private Organization createOrganization(String accountIdentifier) {
    return Organization.builder()
        .id(randomAlphabetic(10))
        .accountIdentifier(accountIdentifier)
        .identifier(randomAlphabetic(10))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList_For_Search() {
    String accountIdentifier = randomAlphabetic(10);

    List<Organization> organizationList = new ArrayList<>();

    organizationList.add(createOrganization(accountIdentifier));
    organizationList.add(createOrganization(accountIdentifier));
    when(organizationService.list(any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(organizationList, 2));
    String text = "text";
    final NGPageResponse<OrganizationDTO> organizationDTOs =
        accountOrganizationResource.search(accountIdentifier, text, 0, 10, null).getData();

    assertNotNull(organizationDTOs);
    assertNotNull("Page contents should not be null", organizationDTOs.getContent());

    List<OrganizationDTO> returnedDTOs = organizationDTOs.getContent();

    assertNotNull("Returned project DTOs page should not null ", returnedDTOs);
    assertEquals(returnedDTOs.size(), organizationList.size());
  }
}

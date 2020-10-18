package io.harness.ng.core.entitysetupusage.resource;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class EntitySetupUsageResourceTest {
  @InjectMocks EntitySetupUsageResource entitySetupUsageResource;
  @Mock EntitySetupUsageService entitySetupUsageService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void listTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String searchTerm = "searchTerm";
    entitySetupUsageResource.list(
        100, 100, accountIdentifier, orgIdentifier, projectIdentifier, identifier, searchTerm);
    Mockito.verify(entitySetupUsageService, times(1))
        .list(eq(100), eq(100), eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(identifier),
            eq(searchTerm));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void isEntityReferenced() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    entitySetupUsageResource.isEntityReferenced(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    Mockito.verify(entitySetupUsageService, times(1))
        .isEntityReferenced(eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(identifier));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void saveTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    EntitySetupUsageDTO entitySetupUsageDTO =
        EntitySetupUsageDTO.builder().accountIdentifier(accountIdentifier).build();
    entitySetupUsageResource.save(entitySetupUsageDTO);
    Mockito.verify(entitySetupUsageService, times(1)).save(eq(entitySetupUsageDTO));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void deleteTest() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    entitySetupUsageResource.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier, true);
    Mockito.verify(entitySetupUsageService, times(1))
        .delete(eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(identifier), eq(true));
  }
}
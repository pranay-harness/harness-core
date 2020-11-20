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
    String referredEntityFQN = "referredEntityFQN";
    entitySetupUsageResource.listAllEntityUsage(100, 100, accountIdentifier, referredEntityFQN, searchTerm);
    Mockito.verify(entitySetupUsageService, times(1))
        .listAllEntityUsage(eq(100), eq(100), eq(accountIdentifier), eq(referredEntityFQN), eq(searchTerm));
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void isEntityReferenced() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String referredEntityFQN = "referredEntityFQN";
    entitySetupUsageResource.isEntityReferenced(accountIdentifier, referredEntityFQN);
    Mockito.verify(entitySetupUsageService, times(1)).isEntityReferenced(eq(accountIdentifier), eq(referredEntityFQN));
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
    String referredEntityFQN = "referredEntityFQN";
    String referredByEntityFQN = "referredByEntityFQN";
    entitySetupUsageResource.delete(accountIdentifier, referredEntityFQN, referredEntityFQN);
    Mockito.verify(entitySetupUsageService, times(1))
        .delete(eq(accountIdentifier), eq(referredEntityFQN), eq(referredEntityFQN));
  }
}

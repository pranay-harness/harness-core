package io.harness.ng.core.entityReference.resouce;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.entityReference.service.EntityReferenceService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class EntityReferenceResourceTest {
  @InjectMocks EntityReferenceResource entityReferenceResource;
  @Mock EntityReferenceService entityReferenceService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void list() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String identifier = "identifier";
    String searchTerm = "searchTerm";
    entityReferenceResource.list(100, 100, accountIdentifier, orgIdentifier, projectIdentifier, identifier, searchTerm);
    Mockito.verify(entityReferenceService, times(1))
        .list(eq(100), eq(100), eq(accountIdentifier), eq(orgIdentifier), eq(projectIdentifier), eq(identifier),
            eq(searchTerm));
  }
}
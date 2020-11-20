package io.harness.ng.core.entitysetupusage;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

public class EntitySetupUsageFilterHelperTest extends CategoryTest {
  @InjectMocks EntitySetupUsageFilterHelper entitySetupUsageFilterHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createCriteriaFromEntityFilter() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredEntityFQN = "referredEntityFQN";
    String searchTerm = "searchTerm";

    Criteria criteria =
        entitySetupUsageFilterHelper.createCriteriaFromEntityFilter(accountIdentifier, referredEntityFQN, searchTerm);
    assertThat(criteria.getCriteriaObject().size()).isEqualTo(3);
    assertThat(criteria.getCriteriaObject().get(EntitySetupUsageKeys.referredEntityFQN)).isEqualTo(referredEntityFQN);
  }
}

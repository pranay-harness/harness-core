package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.exception.UnsupportedOperationException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import software.wings.service.impl.DelegateProfileManagerServiceImpl;
import software.wings.service.intfc.DelegateProfileManagerService;

import java.util.HashSet;

public class DelegateProfileManagerServiceTest {
  private DelegateProfileManagerService delegateProfileManagerService = new DelegateProfileManagerServiceImpl();
  private static final String ACCOUNT_ID = generateUuid();
  private static String DELEGATE_PROFILE_ID = generateUuid();

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldList() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.list(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldGet() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.get(ACCOUNT_ID, DELEGATE_PROFILE_ID);
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    DelegateProfileDetails profileDetail =
        DelegateProfileDetails.builder().accountId(ACCOUNT_ID).uuid(DELEGATE_PROFILE_ID).name("test").build();
    delegateProfileManagerService.update(profileDetail);
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldAdd() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    DelegateProfileDetails profileDetail = DelegateProfileDetails.builder().accountId(ACCOUNT_ID).name("test").build();
    delegateProfileManagerService.add(profileDetail);
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldUpdateScopingRules() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    ScopingRuleDetails scopingRuleDetail =
        ScopingRuleDetails.builder().description("test").environmentIds(new HashSet<>(asList("PROD"))).build();
    delegateProfileManagerService.updateScopingRules(ACCOUNT_ID, DELEGATE_PROFILE_ID, asList(scopingRuleDetail));
  }

  @Test
  @Owner(developers = OwnerRule.SANJA)
  @Category(UnitTests.class)
  public void shouldDelete() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.delete(ACCOUNT_ID, DELEGATE_PROFILE_ID);
  }

  @Test
  @Owner(developers = OwnerRule.MARKO)
  @Category(UnitTests.class)
  public void shouldUpdateSelectors() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("not implemented");
    delegateProfileManagerService.updateSelectors(ACCOUNT_ID, DELEGATE_PROFILE_ID, asList("selector"));
  }
}

package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.schema.type.trigger.QLCreateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLTriggerActionInput;
import software.wings.service.intfc.TriggerService;

public class CreateTriggerDataFetcherTest extends CategoryTest {
  @Mock TriggerService triggerService;

  @InjectMocks CreateTriggerDataFetcher createTriggerDataFetcher = new CreateTriggerDataFetcher(triggerService);

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validateTriggerShouldThrowAppIdMustNotBeEmptyException() {
    QLCreateTriggerInput qlCreateTriggerInput = QLCreateTriggerInput.builder().applicationId(null).build();
    createTriggerDataFetcher.validateTrigger(qlCreateTriggerInput, "accountId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validateTriggerShouldThrowAppIdDoesntBelongToThisAccountException() {
    QLCreateTriggerInput qlCreateTriggerInput = QLCreateTriggerInput.builder().applicationId(null).build();
    createTriggerDataFetcher.validateTrigger(qlCreateTriggerInput, "accountId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validateTriggerShouldThrowTriggerNameMustNotBeEmptyException() {
    QLCreateTriggerInput qlCreateTriggerInput = QLCreateTriggerInput.builder().name(null).build();
    createTriggerDataFetcher.validateTrigger(qlCreateTriggerInput, "accountId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validateTriggerShouldThrowEntityIdMustNotBeEmptyException() {
    QLCreateTriggerInput qlCreateTriggerInput =
        QLCreateTriggerInput.builder().action(QLTriggerActionInput.builder().entityId(null).build()).build();
    createTriggerDataFetcher.validateTrigger(qlCreateTriggerInput, "accountId");
  }
}

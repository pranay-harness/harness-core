package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.SkipCondition.SkipConditionType;

public class SkipConditionTest extends WingsBaseTest {
  private static final String EXPERSSION_STRING = "${app.name}==\"APP_NAME\"";

  @Test
  @Category(UnitTests.class)
  public void testInstanceForAlwaysSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion("true");
    assertThat(skipCondition.getExpression()).isNull();
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.ALWAYS_SKIP);
  }

  @Test
  @Category(UnitTests.class)
  public void testInstanceForDoNotSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(null);
    assertThat(skipCondition.getExpression()).isNull();
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.DO_NOT_SKIP);
  }

  @Test
  @Category(UnitTests.class)
  public void testInstanceForConditionalSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(EXPERSSION_STRING);
    assertThat(skipCondition.getExpression()).isEqualTo(EXPERSSION_STRING);
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.CONDITIONAL_SKIP);
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchDisableAssertionDoNotSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion("true");
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo("true");
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchDisableAssertionConditionalSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(null);
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo(null);
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchDisableAssertionForAlwaysSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(EXPERSSION_STRING);
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo(EXPERSSION_STRING);
  }
}
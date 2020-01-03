package software.wings.helpers.ext.helm;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class HelmHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnTrueWhenStringPresentInValuesYaml() {
    String valuesYaml = "abc\ndef";
    String toFind = "def";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenStringNotPresentInValuesYaml() {
    String valuesYaml = "abc\ndef";
    String toFind = "defg";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldIgnoreCommentsInValuesYaml() {
    String valuesYaml = "abc\n#def";
    String toFind = "def";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isFalse();
  }
}
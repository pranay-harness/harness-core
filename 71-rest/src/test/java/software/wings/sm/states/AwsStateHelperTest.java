package software.wings.sm.states;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.Map;

public class AwsStateHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testFetchRequiredAsgCapacity() {
    AwsStateHelper helper = spy(AwsStateHelper.class);
    Map<String, Integer> map = ImmutableMap.of("foo__1", 1, "foo__2", 1);
    assertThatThrownBy(() -> helper.fetchRequiredAsgCapacity(map, "foo__3"))
        .isInstanceOf(InvalidRequestException.class);
    assertThat(helper.fetchRequiredAsgCapacity(map, "foo__1")).isEqualTo(1);
  }
}
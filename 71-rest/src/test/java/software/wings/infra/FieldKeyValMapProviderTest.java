package software.wings.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.infra.data.DummyPerson;

import java.util.Map;

public class FieldKeyValMapProviderTest {
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String OCCUPATION = "occupation";
  public static final String CUSTOM_KEY = "customKey";

  @Test
  @Category(UnitTests.class)
  public void getQueryMapForClass() {
    DummyPerson person = new DummyPerson(ID, NAME, OCCUPATION);
    Map<String, Object> queryMap = person.getFieldMapForClass();
    assertEquals(queryMap.size(), 2);
    assertThat(queryMap.containsKey(CUSTOM_KEY)).isTrue();
    assertThat(queryMap.containsKey(NAME)).isTrue();
    assertThat(queryMap.containsKey(OCCUPATION)).isFalse();
    assertThat(queryMap.containsKey(ID)).isFalse();
  }
}

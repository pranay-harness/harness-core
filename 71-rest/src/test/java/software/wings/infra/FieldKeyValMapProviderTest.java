package software.wings.infra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.annotation.IncludeInFieldMap;

import java.util.Map;

public class FieldKeyValMapProviderTest {
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String OCCUPATION = "occupation";
  public static final String CUSTOM_KEY = "customKey";

  @AllArgsConstructor
  public class Person implements FieldKeyValMapProvider {
    @IncludeInFieldMap(key = CUSTOM_KEY) private String id;
    @IncludeInFieldMap private String name;

    private String occupation;
  }

  @Test
  @Category(UnitTests.class)
  public void getQueryMapForClass() {
    Person person = new Person(ID, NAME, OCCUPATION);
    Map<String, Object> queryMap = person.getFieldMapForClass();
    assertEquals(queryMap.size(), 2);
    assertTrue(queryMap.containsKey(CUSTOM_KEY));
    assertTrue(queryMap.containsKey(NAME));
    assertFalse(queryMap.containsKey(OCCUPATION));
  }
}

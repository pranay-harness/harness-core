package io.harness.data.structure;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CollectionUtilsTest extends CategoryTest {
  private final List<DummyPerson> personCollection =
      Arrays.asList(new DummyPerson("Oliver", 25), new DummyPerson("Jack", 36), new DummyPerson("John", 59));

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsPresent() {
    boolean present;
    present = CollectionUtils.isPresent(personCollection, dummyPerson -> dummyPerson.getName().equals("Jack"));
    assertThat(present).isTrue();

    present = CollectionUtils.isPresent(personCollection, dummyPerson -> dummyPerson.getName().equals("NoExist"));
    assertThat(present).isFalse();

    present =
        CollectionUtils.isPresent(new ArrayList<DummyPerson>(), dummyPerson -> dummyPerson.getName().equals("NoExist"));
    assertThat(present).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchIndex() {
    int index;
    index = CollectionUtils.fetchIndex(personCollection, dummyPerson -> dummyPerson.getName().equals("Jack"));
    assertThat(index).isEqualTo(1);

    index = CollectionUtils.fetchIndex(personCollection, dummyPerson -> dummyPerson.getName().equals("NoExist"));
    assertThat(index).isEqualTo(-1);

    index = CollectionUtils.fetchIndex(
        new ArrayList<DummyPerson>(), dummyPerson -> dummyPerson.getName().equals("NoExist"));
    assertThat(index).isEqualTo(-1);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFilterAndGetFirst() {
    Optional<DummyPerson> personOptional =
        CollectionUtils.filterAndGetFirst(personCollection, dummyPerson -> dummyPerson.getName().equals("Jack"));
    boolean isPersonPresent = personOptional.isPresent();
    assertThat(isPersonPresent).isEqualTo(true);
    assertThat(personOptional.get().getName()).isEqualTo("Jack");
    assertThat(personOptional.get().getAge()).isEqualTo(36);
  }

  @Data
  @AllArgsConstructor
  private class DummyPerson {
    private String name;
    private int age;
  }
}
/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.structure;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.VGLIJIN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNullIfEmpty() {
    assertThat(CollectionUtils.nullIfEmpty(null)).isNull();
    assertThat(CollectionUtils.nullIfEmpty(Collections.emptyList())).isNull();
    assertThat(CollectionUtils.nullIfEmpty(Collections.singletonList("a"))).isNotNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNullIfEmptyMap() {
    assertThat(CollectionUtils.nullIfEmptyMap(null)).isNull();
    assertThat(CollectionUtils.nullIfEmptyMap(Collections.emptyMap())).isNull();
    assertThat(CollectionUtils.nullIfEmptyMap(Collections.singletonMap("k", "v"))).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testCollectionToStream() {
    assertThat(CollectionUtils.collectionToStream(null).collect(Collectors.toList())).isEmpty();
    assertThat(CollectionUtils.collectionToStream(Collections.emptyList()).collect(Collectors.toList())).isEmpty();
    assertThat(CollectionUtils.collectionToStream(Sets.newHashSet("get", "ship", "done")).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder("get", "ship", "done");
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void testOverrideOperator() {
    assertThat(CollectionUtils.overrideOperator(null, 1)).isEqualTo(1);
    assertThat(CollectionUtils.overrideOperator("", null)).isNull();
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FieldKeyValMapProviderTest extends CategoryTest {
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String OCCUPATION = "occupation";
  public static final String CUSTOM_KEY = "customKey";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void getQueryMapForClass() {
    DummyPerson person = new DummyPerson(ID, NAME, OCCUPATION);
    Map<String, Object> queryMap = person.getFieldMapForClass();
    assertThat(2).isEqualTo(queryMap.size());
    assertThat(queryMap.containsKey(CUSTOM_KEY)).isTrue();
    assertThat(queryMap.containsKey(NAME)).isTrue();
    assertThat(queryMap.containsKey(OCCUPATION)).isFalse();
    assertThat(queryMap.containsKey(ID)).isFalse();
  }
}

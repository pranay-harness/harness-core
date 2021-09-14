/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.event;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventProtoToEntityHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetEntityTypeProtoEnumToRestEnumMap() {
    final Map<EntityTypeProtoEnum, EntityType> entityTypeProtoEnumToRestEnumMap =
        EventProtoToEntityHelper.getEntityTypeProtoEnumToRestEnumMap();
    assertThat(entityTypeProtoEnumToRestEnumMap.size()).isEqualTo(EntityType.values().length);
  }
}

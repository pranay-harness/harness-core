/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.task.converters;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.task.TaskServiceTestBase;
import io.harness.task.TaskServiceTestHelper;
import io.harness.task.service.HTTPTaskResponse;
import io.harness.task.service.TaskType;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResponseDataConverterRegistryTest extends TaskServiceTestBase {
  @Inject ResponseDataConverterRegistry responseDataConverterRegistry;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldRegisterAndObtain() {
    responseDataConverterRegistry.register(
        TaskType.HTTP, TaskServiceTestHelper.DummyHTTPResponseDataConverter.builder().build());
    ResponseDataConverter<HTTPTaskResponse> obtain = responseDataConverterRegistry.obtain(TaskType.HTTP);
    assertThat(obtain).isNotNull();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnDuplicateRegistration() {
    responseDataConverterRegistry.register(
        TaskType.HTTP, TaskServiceTestHelper.DummyHTTPResponseDataConverter.builder().build());
    assertThatThrownBy(()
                           -> responseDataConverterRegistry.register(
                               TaskType.HTTP, TaskServiceTestHelper.DummyHTTPResponseDataConverter.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnNotRegisteredType() {
    assertThatThrownBy(() -> responseDataConverterRegistry.obtain(TaskType.HTTP))
        .isInstanceOf(InvalidArgumentsException.class);
  }
}

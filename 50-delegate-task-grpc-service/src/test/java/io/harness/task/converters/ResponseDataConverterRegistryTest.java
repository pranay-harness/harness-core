package io.harness.task.converters;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.task.TaskServiceTest;
import io.harness.task.TaskServiceTestHelper;
import io.harness.task.service.HTTPTaskResponse;
import io.harness.task.service.TaskType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResponseDataConverterRegistryTest extends TaskServiceTest {
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
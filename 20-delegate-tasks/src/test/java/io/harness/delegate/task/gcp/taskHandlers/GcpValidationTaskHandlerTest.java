package io.harness.delegate.task.gcp.taskHandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.gcp.client.GcpClient;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GcpValidationTaskHandlerTest extends CategoryTest {
  @Mock private GcpClient gcpClient;
  @InjectMocks private GcpValidationTaskHandler taskHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeRequestSuccess() {
    final GcpResponse response = taskHandler.executeRequest(buildGcpValidationRequest());
    assertThat(response.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeRequestFailure() {
    doThrow(new RuntimeException("No Default Credentials found")).when(gcpClient).validateDefaultCredentials();

    final GcpResponse gcpResponse = taskHandler.executeRequest(buildGcpValidationRequest());

    assertThat(gcpResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(gcpResponse.getErrorMessage()).isEqualTo("No Default Credentials found.");
  }
  private GcpValidationRequest buildGcpValidationRequest() {
    return GcpValidationRequest.builder().delegateSelector("foo").build();
  }
}
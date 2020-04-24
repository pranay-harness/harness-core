package software.wings.service.impl.stackdriver;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.GcpConfig;

import java.util.ArrayList;
import java.util.List;

public class StackDriverLogDataCollectionInfoTest extends CategoryTest {
  private StackDriverLogDataCollectionInfo stackDriverLogDataCollectionInfo;

  @Before
  public void setupTests() {
    initMocks(this);
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    stackDriverLogDataCollectionInfo = StackDriverLogDataCollectionInfo.builder()
                                           .gcpConfig(GcpConfig.builder().build())
                                           .encryptedDataDetails(encryptedDataDetails)
                                           .build();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities =
        stackDriverLogDataCollectionInfo.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) executionCapabilities.get(0);
    assertThat(httpConnectionExecutionCapability.fetchCapabilityBasis())
        .isEqualTo("https://logging.googleapis.com/$discovery/rest?version=v2");
  }
}
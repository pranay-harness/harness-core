package io.harness.cvng.core.services.api;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.cvng.core.services.impl.StackdriverServiceImpl;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.*;
import software.wings.service.impl.datadog.DatadogServiceImpl;
import software.wings.service.intfc.datadog.DatadogService;

import java.util.ArrayList;
import java.util.List;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatadogServiceImplTest extends CategoryTest {
  @Mock private OnboardingService onboardingService;
  @InjectMocks private DatadogService datadogService = new DatadogServiceImpl();
  private String accountId;
  private String connectorIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  @Captor private ArgumentCaptor<OnboardingRequestDTO> requestCaptor;

  @Before
  public void setup() {
    accountId = generateUuid();
    connectorIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetDashboardList() {
  }
}

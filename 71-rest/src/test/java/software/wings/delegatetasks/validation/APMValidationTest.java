package software.wings.delegatetasks.validation;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.apm.APMDataCollectionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class APMValidationTest extends WingsBaseTest {
  private APMValidation spyValidation;
  private DelegateConnectionResult connectionResult;
  private String delegateId;
  private DelegateTask delegateTask;
  private Consumer delegateConnectionConsumer;
  private String baseUrl;
  private String validationUrl;
  private List<String> criteria;

  @Captor private ArgumentCaptor<Map<String, String>> headerCaptor;

  @Before
  public void setUp() {
    initMocks(this);
    delegateId = generateUuid();
    delegateTask = DelegateTask.builder()
                       .uuid(generateUuid())
                       .data(TaskData.builder().taskType(TaskType.APM_24_7_METRIC_DATA_COLLECTION_TASK.name()).build())
                       .build();
    delegateConnectionConsumer = Mockito.spy(Consumer.class);

    baseUrl = generateUuid();
    validationUrl = generateUuid();

    spyValidation = Mockito.spy(new APMValidation(delegateId, delegateTask, delegateConnectionConsumer));
    connectionResult = DelegateConnectionResult.builder().validated(true).build();

    criteria = Collections.singletonList(baseUrl);
    doReturn(criteria).when(spyValidation).getCriteria();

    doReturn(connectionResult).when(spyValidation).validateSecretManager();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidate_InvalidSecretManager() {
    connectionResult.setValidated(false);
    doReturn(connectionResult).when(spyValidation).validateSecretManager();

    List<DelegateConnectionResult> results = spyValidation.validate();
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getCriteria()).isEqualTo(criteria.get(0));
    assertThat(results.get(0).isValidated()).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidate_APMDataCollectionInfo_False() {
    APMDataCollectionInfo dataCollectionInfo =
        APMDataCollectionInfo.builder().baseUrl(baseUrl).validationUrl(validationUrl).build();
    Object[] params = new Object[] {dataCollectionInfo};
    doReturn(params).when(spyValidation).getParameters();

    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().baseUrl(baseUrl).url(validationUrl).build();
    doReturn(false).when(spyValidation).validateCollector(config);

    List<DelegateConnectionResult> results = spyValidation.validate();
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getCriteria()).isEqualTo(criteria.get(0));
    assertThat(results.get(0).isValidated()).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidate_APMDataCollectionInfo_True() {
    APMDataCollectionInfo dataCollectionInfo =
        APMDataCollectionInfo.builder().baseUrl(baseUrl).validationUrl(validationUrl).build();
    Object[] params = new Object[] {dataCollectionInfo};
    doReturn(params).when(spyValidation).getParameters();

    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().baseUrl(baseUrl).url(validationUrl).build();
    doReturn(true).when(spyValidation).validateCollector(config);

    List<DelegateConnectionResult> results = spyValidation.validate();
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getCriteria()).isEqualTo(criteria.get(0));
    assertThat(results.get(0).isValidated()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidate_APMValidateCollector_True() {
    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().baseUrl(baseUrl).url(validationUrl).build();
    Object[] params = new Object[] {config};
    doReturn(params).when(spyValidation).getParameters();

    doReturn(true).when(spyValidation).validateCollector(config);

    List<DelegateConnectionResult> results = spyValidation.validate();
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0).getCriteria()).isEqualTo(criteria.get(0));
    assertThat(results.get(0).isValidated()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateCollector_validateAcceptHeaderChanged() {
    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().baseUrl(baseUrl).url(validationUrl).build();
    Object[] params = new Object[] {config};
    doReturn(params).when(spyValidation).getParameters();

    APMRestClient apmRestClient = Mockito.mock(APMRestClient.class);
    doReturn(apmRestClient).when(spyValidation).getAPMRestClient(any());

    spyValidation.validateCollector(config);

    verify(apmRestClient, times(1)).validate(any(), headerCaptor.capture(), any());

    assertThat(headerCaptor.getValue().get("Accept")).isEqualTo("application/json");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateCollector_validateAcceptHeaderUnChanged() {
    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder().baseUrl(baseUrl).url(validationUrl).headers(new HashMap<>()).build();
    config.getHeaders().put("Accept", "text/html");
    Object[] params = new Object[] {config};
    doReturn(params).when(spyValidation).getParameters();

    APMRestClient apmRestClient = Mockito.mock(APMRestClient.class);
    doReturn(apmRestClient).when(spyValidation).getAPMRestClient(any());

    spyValidation.validateCollector(config);

    verify(apmRestClient, times(1)).validate(any(), headerCaptor.capture(), any());

    assertThat(headerCaptor.getValue().get("Accept")).isEqualTo("text/html");
  }
}
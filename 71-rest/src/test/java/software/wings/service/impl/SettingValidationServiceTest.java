package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobCollection;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.Query;
import retrofit2.Call;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.InstanaConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.ValidationResult;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.newrelic.NewRelicApplicationsResponse;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Pranjal on 09/14/2018
 */
public class SettingValidationServiceTest extends WingsBaseTest {
  @Inject private SettingValidationService settingValidationService;
  @Inject private AnalysisService analysisService;
  @Inject private NewRelicService newRelicService;
  @Inject private SplunkDelegateService splunkDelegateService;
  @Inject private ElkDelegateService elkDelegateService;
  @Inject private LogzDelegateService logzDelegateService;
  @Inject private SumoDelegateService sumoDelegateService;
  @Inject private InstanaDelegateService instanaDelegateService;
  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Inject private DynaTraceDelegateService dynaTraceDelegateService;
  @Inject private APMDelegateService apmDelegateService;
  @Inject private RequestExecutor requestExecutor;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ElkAnalysisService elkAnalysisService;

  private String accountId;
  private SplunkDelegateService spySplunkDelegateService;
  private RequestExecutor spyRequestExecutor;
  private Query<SettingAttribute> spyQuery;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setupTests() throws Exception {
    accountId = generateUuid();
    spyRequestExecutor = spy(requestExecutor);

    FieldUtils.writeField(analysisService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(newRelicService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(settingValidationService, "analysisService", analysisService, true);
    FieldUtils.writeField(settingValidationService, "newRelicService", newRelicService, true);
    FieldUtils.writeField(elkDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(logzDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(instanaDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(newRelicDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(appdynamicsDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(dynaTraceDelegateService, "requestExecutor", spyRequestExecutor, true);
    FieldUtils.writeField(apmDelegateService, "requestExecutor", spyRequestExecutor, true);

    spySplunkDelegateService = spy(splunkDelegateService);
    when(delegateProxyFactory.get(eq(SplunkDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(spySplunkDelegateService);
    when(delegateProxyFactory.get(eq(ElkDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(elkDelegateService);
    when(delegateProxyFactory.get(eq(LogzDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(logzDelegateService);
    when(delegateProxyFactory.get(eq(SumoDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(sumoDelegateService);
    when(delegateProxyFactory.get(eq(InstanaDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(instanaDelegateService);
    when(delegateProxyFactory.get(eq(NewRelicDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(newRelicDelegateService);
    when(delegateProxyFactory.get(eq(AppdynamicsDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(appdynamicsDelegateService);
    when(delegateProxyFactory.get(eq(DynaTraceDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(dynaTraceDelegateService);
    when(delegateProxyFactory.get(eq(APMDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(apmDelegateService);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidCredentialsSplunkConnector() {
    com.splunk.Service splunkService = Mockito.mock(com.splunk.Service.class);
    when(splunkService.getJobs()).thenThrow(new RuntimeException("invalid credentials"));
    doReturn(splunkService).when(spySplunkDelegateService).initSplunkService(any(SplunkConfig.class), anyList());
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(createSplunkConfig()).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("DataCollectionException: java.lang.RuntimeException: invalid credentials");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongUrlSplunkConnector() {
    final SplunkConfig splunkConfig = SplunkConfig.builder()
                                          .splunkUrl("https://google.com")
                                          .username(generateUuid())
                                          .password(generateUuid().toCharArray())
                                          .build();
    splunkConfig.setDecrypted(true);
    com.splunk.Service splunkService = Mockito.mock(com.splunk.Service.class);
    doThrow(new DataCollectionException("HttpException: HTTP 404")).when(splunkService).getJobs();
    doReturn(splunkService).when(spySplunkDelegateService).initSplunkService(any(SplunkConfig.class), anyList());
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(splunkConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage()).contains("HttpException: HTTP 404");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlSplunkConnector() {
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(createSplunkConfig()).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage()).contains("MalformedURLException");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidSplunkConnector() {
    JobCollection jobCollection = Mockito.mock(JobCollection.class);
    when(jobCollection.create(anyString(), any(JobArgs.class))).thenReturn(Mockito.mock(Job.class));
    com.splunk.Service splunkService = Mockito.mock(com.splunk.Service.class);
    when(splunkService.getJobs()).thenReturn(jobCollection);
    doReturn(splunkService).when(spySplunkDelegateService).initSplunkService(any(SplunkConfig.class), anyList());
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(createSplunkConfig()).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  private SplunkConfig createSplunkConfig() {
    final SplunkConfig splunkConfig = SplunkConfig.builder()
                                          .splunkUrl(generateUuid())
                                          .accountId(accountId)
                                          .username(generateUuid())
                                          .username(generateUuid())
                                          .password(generateUuid().toCharArray())
                                          .build();
    splunkConfig.setDecrypted(true);
    return splunkConfig;
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenUsernameAndNoPasswordElkConnector() {
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute()
            .withAccountId(accountId)
            .withName(generateUuid())
            .withValue(ElkConfig.builder().elkUrl(generateUuid()).username(generateUuid()).build())
            .build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: User name is given but password is empty");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenPasswordAndNoUsernameElkConnector() {
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute()
            .withAccountId(accountId)
            .withName(generateUuid())
            .withValue(ElkConfig.builder().elkUrl(generateUuid()).password(generateUuid().toCharArray()).build())
            .build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: User name is empty but password is given");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlElkConnector() {
    final ElkConfig elkConfig = ElkConfig.builder().elkUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(elkConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + elkConfig.getElkUrl() + "/");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlElkConnector() {
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute()
            .withAccountId(accountId)
            .withName(generateUuid())
            .withValue(ElkConfig.builder().elkUrl("https://elk-example.com/").build())
            .build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: elk-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidElkConnector() {
    doReturn(new Object()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute()
            .withAccountId(accountId)
            .withName(generateUuid())
            .withValue(ElkConfig.builder().elkUrl("https://elk-example.com/").build())
            .build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlLogzConnector() {
    final LogzConfig logzConfig = LogzConfig.builder().logzUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(logzConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + logzConfig.getLogzUrl() + "/");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlLogzConnector() {
    final LogzConfig logzConfig =
        LogzConfig.builder().logzUrl("https://logz-example.com").token(generateUuid().toCharArray()).build();
    logzConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(logzConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: logz-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongTokenLogzConnector() {
    final LogzConfig logzConfig =
        LogzConfig.builder().logzUrl("https://api.logz.io").token(generateUuid().toCharArray()).build();
    logzConfig.setDecrypted(true);
    doThrow(
        new DataCollectionException(
            "Response code: 401, Message: Unauthorized, Error: {\"code\":401,\"message\":\"HTTP 401 Unauthorized\""))
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(logzConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .contains(
            "DataCollectionException: Response code: 401, Message: Unauthorized, Error: {\"code\":401,\"message\":\"HTTP 401 Unauthorized\"");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidLogzConnector() {
    final LogzConfig logzConfig =
        LogzConfig.builder().logzUrl("https://logz-example.com").token(generateUuid().toCharArray()).build();
    logzConfig.setDecrypted(true);
    doReturn(new Object()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(logzConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlSumoConnector() {
    final SumoConfig sumoConfig = SumoConfig.builder()
                                      .sumoUrl("ranom-invalid-url")
                                      .accessId(generateUuid().toCharArray())
                                      .accessKey(generateUuid().toCharArray())
                                      .build();
    sumoConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(sumoConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("DataCollectionException: java.net.MalformedURLException: no protocol: " + sumoConfig.getSumoUrl());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlSumoConnector() {
    final SumoConfig sumoConfig = SumoConfig.builder()
                                      .sumoUrl("https://sumo-example.com")
                                      .accessId(generateUuid().toCharArray())
                                      .accessKey(generateUuid().toCharArray())
                                      .build();
    sumoConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(sumoConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage()).isEqualTo("SumoClientException: Error reading server response");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWongTokenSumoConnector() {
    final SumoConfig sumoConfig = SumoConfig.builder()
                                      .sumoUrl("https://api.us2.sumologic.com/api/v1/")
                                      .accessId(generateUuid().toCharArray())
                                      .accessKey(generateUuid().toCharArray())
                                      .build();
    sumoConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(sumoConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage()).contains("SumoServerException: ");
    assertThat(validationResult.getErrorMessage()).contains("\"status\" : 401");
    assertThat(validationResult.getErrorMessage()).contains("\"message\" : \"Credential could not be verified.\"\n");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlInstanaConnector() {
    final InstanaConfig instanaConfig = InstanaConfig.builder().instanaUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(instanaConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + instanaConfig.getInstanaUrl());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlInstanaConnector() {
    final InstanaConfig instanaConfig = InstanaConfig.builder()
                                            .instanaUrl("https://instana-example.com")
                                            .apiToken(generateUuid().toCharArray())
                                            .build();
    instanaConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(instanaConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: instana-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongTokenInstanaConnector() {
    final InstanaConfig instanaConfig = InstanaConfig.builder()
                                            .instanaUrl("https://integration-harness.instana.io/")
                                            .apiToken(generateUuid().toCharArray())
                                            .build();
    instanaConfig.setDecrypted(true);
    doThrow(new DataCollectionException(
                "Response code: 401, Message: Unauthorized, Error: {\"errors\":[\"Unauthorized request\"]}"))
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(instanaConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: Response code: 401, Message: Unauthorized, Error: {\"errors\":[\"Unauthorized request\"]}");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidInstanaConnector() {
    final InstanaConfig instanaConfig = InstanaConfig.builder()
                                            .instanaUrl("https://integration-harness.instana.io/")
                                            .apiToken(generateUuid().toCharArray())
                                            .build();
    instanaConfig.setDecrypted(true);
    doReturn(new Object()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(instanaConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlNewRelicConnector() {
    final NewRelicConfig newRelicConfig = NewRelicConfig.builder().newRelicUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(newRelicConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + newRelicConfig.getNewRelicUrl() + "/");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlNewRelicConnector() {
    final NewRelicConfig newRelicConfig = NewRelicConfig.builder()
                                              .newRelicUrl("https://newrelic-example.com")
                                              .apiKey(generateUuid().toCharArray())
                                              .build();
    newRelicConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(newRelicConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: newrelic-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongTokenNewRelicConnector() {
    final NewRelicConfig newRelicConfig =
        NewRelicConfig.builder().newRelicUrl("https://api.newrelic.com").apiKey(generateUuid().toCharArray()).build();
    newRelicConfig.setDecrypted(true);
    doThrow(
        new DataCollectionException(
            "Response code: 401, Message: Unauthorized, Error: {\"error\":{\"title\":\"The API key provided is invalid\"}}"))
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(newRelicConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: Response code: 401, Message: Unauthorized, Error: {\"error\":{\"title\":\"The API key provided is invalid\"}}");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidNewRelicConnector() {
    final NewRelicConfig newRelicConfig =
        NewRelicConfig.builder().newRelicUrl("https://api.newrelic.com").apiKey(generateUuid().toCharArray()).build();
    newRelicConfig.setDecrypted(true);
    doReturn(NewRelicApplicationsResponse.builder().applications(Lists.newArrayList()).build())
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(newRelicConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlAppdConnector() {
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder().controllerUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(appDynamicsConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + appDynamicsConfig.getControllerUrl() + "/");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlAppdConnector() {
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .controllerUrl("https://appd-example.com")
                                                    .accountname(generateUuid())
                                                    .username(generateUuid())
                                                    .password(generateUuid().toCharArray())
                                                    .build();
    appDynamicsConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(appDynamicsConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: appd-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongTokenAppdConnector() {
    final AppDynamicsConfig appDynamicsConfig =
        AppDynamicsConfig.builder()
            .controllerUrl("https://harness-test.saas.appdynamics.com/controller/")
            .accountname("harness-test")
            .username(generateUuid())
            .password(generateUuid().toCharArray())
            .build();
    appDynamicsConfig.setDecrypted(true);
    doThrow(new DataCollectionException("Response code: 401, Message: Unauthorized, Error: "))
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(appDynamicsConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("DataCollectionException: Response code: 401, Message: Unauthorized, Error: ");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidAppdConnector() {
    final AppDynamicsConfig appDynamicsConfig =
        AppDynamicsConfig.builder()
            .controllerUrl("https://harness-test.saas.appdynamics.com/controller/")
            .accountname(generateUuid())
            .username(generateUuid())
            .password(generateUuid().toCharArray())
            .build();
    appDynamicsConfig.setDecrypted(true);
    doReturn(new ArrayList<>()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(appDynamicsConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlDynatraceConnector() {
    final DynaTraceConfig dynaTraceConfig = DynaTraceConfig.builder().dynaTraceUrl(generateUuid()).build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(dynaTraceConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + dynaTraceConfig.getDynaTraceUrl());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlDynatraceConnector() {
    final DynaTraceConfig dynaTraceConfig = DynaTraceConfig.builder()
                                                .dynaTraceUrl("https://dynatrace-example.com")
                                                .apiToken(generateUuid().toCharArray())
                                                .build();
    dynaTraceConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(dynaTraceConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: java.net.UnknownHostException: dynatrace-example.com: Name or service not known");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenWrongTokenDynatraceConnector() {
    final DynaTraceConfig dynaTraceConfig = DynaTraceConfig.builder()
                                                .dynaTraceUrl("https://lvd51754.live.dynatrace.com/")
                                                .apiToken(generateUuid().toCharArray())
                                                .build();
    dynaTraceConfig.setDecrypted(true);
    doThrow(
        new DataCollectionException(
            "Response code: 401, Message: Unauthorized, Error: {\"error\":{\"code\":401,\"message\":\"Token Authentication failed\"}}"))
        .when(spyRequestExecutor)
        .executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(dynaTraceConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo(
            "DataCollectionException: Response code: 401, Message: Unauthorized, Error: {\"error\":{\"code\":401,\"message\":\"Token Authentication failed\"}}");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidDynatraceConnector() {
    final DynaTraceConfig dynaTraceConfig = DynaTraceConfig.builder()
                                                .dynaTraceUrl("https://lvd51754.live.dynatrace.com/")
                                                .apiToken(generateUuid().toCharArray())
                                                .build();
    dynaTraceConfig.setDecrypted(true);
    doReturn(new ArrayList<>()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(dynaTraceConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenIllegalUrlPrometheusConnector() {
    final PrometheusConfig prometheusConfig = PrometheusConfig.builder().url(generateUuid() + "/").build();
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(prometheusConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("IllegalArgumentException: Illegal URL: " + prometheusConfig.getUrl());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenInvalidUrlPrometheusConnector() {
    final PrometheusConfig prometheusConfig = PrometheusConfig.builder().url("https://prometheus-example.com").build();
    prometheusConfig.setDecrypted(true);
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(prometheusConfig).build());
    assertThat(validationResult.isValid()).isFalse();
    assertThat(validationResult.getErrorMessage())
        .isEqualTo("Error while saving configuration. The Base URL must end with a / (forward slash)");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateConnectivity_whenValidPrometheusConnector() {
    final PrometheusConfig prometheusConfig = PrometheusConfig.builder().url("https://prometheus-example.com/").build();
    prometheusConfig.setDecrypted(true);
    doReturn(new ArrayList<>()).when(spyRequestExecutor).executeRequest(any(Call.class));
    final ValidationResult validationResult = settingValidationService.validateConnectivity(
        aSettingAttribute().withAccountId(accountId).withName(generateUuid()).withValue(prometheusConfig).build());
    assertThat(validationResult.isValid()).isTrue();
    assertThat(validationResult.getErrorMessage()).isEmpty();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testElkValidate() throws IOException {
    final String url = "https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/";
    final String userName = "username";
    final String password = "password";

    when(wingsPersistence.createQuery(eq(SettingAttribute.class))).thenReturn(spyQuery);
    when(elkAnalysisService.getVersion(anyString(), any(ElkConfig.class), anyListOf(EncryptedDataDetail.class)))
        .thenThrow(IOException.class);

    ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(ACCOUNT_ID);
    elkConfig.setElkConnector(ElkConnector.KIBANA_SERVER);
    elkConfig.setElkUrl(url);
    elkConfig.setUsername(userName);
    elkConfig.setPassword(password.toCharArray());

    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(elkConfig);
    thrown.expect(WingsException.class);
    settingValidationService.validate(attribute);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHostConnectionValidationForPrivateKeyField() {
    HostConnectionAttributes.Builder hostConnectionAttributes =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(AccessType.KEY)
            .withAccountId(UUIDGenerator.generateUuid())
            .withConnectionType(ConnectionType.SSH)
            .withKeyless(false)
            .withUserName("TestUser")
            .withAuthenticationScheme(SSH_KEY);

    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(hostConnectionAttributes.build());

    thrown.expect(InvalidRequestException.class);
    settingValidationService.validate(attribute);

    hostConnectionAttributes.withKey("Test Private Key".toCharArray());
    attribute.setValue(hostConnectionAttributes.build());

    thrown = ExpectedException.none();
    settingValidationService.validate(attribute);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHostConnectionValidationForUsernameField() {
    HostConnectionAttributes.Builder hostConnectionAttributes =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(AccessType.KEY)
            .withAuthenticationScheme(SSH_KEY)
            .withConnectionType(ConnectionType.SSH)
            .withAccountId(UUIDGenerator.generateUuid())
            .withKey("Test Private Key".toCharArray())
            .withKeyless(false);

    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(hostConnectionAttributes.build());

    thrown.expect(InvalidRequestException.class);
    settingValidationService.validate(attribute);

    hostConnectionAttributes.withUserName("TestUser");
    attribute.setValue(hostConnectionAttributes.build());

    thrown = ExpectedException.none();
    settingValidationService.validate(attribute);
  }
}

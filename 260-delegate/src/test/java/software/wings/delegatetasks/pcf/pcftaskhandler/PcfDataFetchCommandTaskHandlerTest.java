package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType.FETCH_ORG;
import static software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType.FETCH_ROUTE;
import static software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType.FETCH_SPACE;
import static software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType.RUNNING_COUNT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfCommandResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PcfDataFetchCommandTaskHandlerTest extends WingsBaseTest {
  public static final String ENDPOINT_URL = "endpointUrl";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String ORGANIZATION = "organization";
  public static final String SPACE = "space";
  public static final String APP_NAME = "appName";

  @Mock private PcfDeploymentManager pcfDeploymentManager;
  @Mock private EncryptionService encryptionService;
  @Mock ExecutionLogCallback executionLogCallback;

  @InjectMocks @Inject private PcfDataFetchCommandTaskHandler pcfDataFetchCommandTaskHandler;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testWithDifferentPcfCommandRequest() {
    PcfCommandRequest pcfCommandRequest = PcfCommandDeployRequest.builder().build();
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    assertThatThrownBy(()
                           -> pcfDataFetchCommandTaskHandler.executeTaskInternal(
                               pcfCommandRequest, encryptedDataDetails, executionLogCallback, false))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNoActionTypeSpecified() {
    PcfCommandRequest pcfCommandRequest = getPcfInfraMappingDataRequest(null);
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();

    PcfCommandExecutionResponse response = pcfDataFetchCommandTaskHandler.executeTaskInternal(
        pcfCommandRequest, encryptedDataDetails, executionLogCallback, false);

    PcfInfraMappingDataResponse pcfCommandResponse = (PcfInfraMappingDataResponse) response.getPcfCommandResponse();
    assertThat(pcfCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetOrganization() throws PivotalClientApiException {
    PcfInfraMappingDataRequest pcfCommandRequest = getPcfInfraMappingDataRequest(FETCH_ORG);
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    List<String> organizations = Arrays.asList("org1", "org1");
    doReturn(organizations).when(pcfDeploymentManager).getOrganizations(any());

    PcfCommandExecutionResponse response = pcfDataFetchCommandTaskHandler.executeTaskInternal(
        pcfCommandRequest, encryptedDataDetails, executionLogCallback, false);

    verify(encryptionService, times(1)).decrypt(pcfCommandRequest.getPcfConfig(), encryptedDataDetails, false);

    ArgumentCaptor<PcfRequestConfig> argument = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(pcfDeploymentManager).getOrganizations(argument.capture());
    PcfRequestConfig pcfRequestConfig = argument.getValue();

    assertPcfRequestConfig(pcfCommandRequest, pcfRequestConfig);

    PcfCommandResponse pcfCommandResponse = response.getPcfCommandResponse();
    assertThat(pcfCommandResponse).isInstanceOf(PcfInfraMappingDataResponse.class);
    assertThat(((PcfInfraMappingDataResponse) pcfCommandResponse).getOrganizations()).isEqualTo(organizations);
    assertThat(pcfCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfCommandResponse.getOutput()).isEqualTo(StringUtils.EMPTY);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetSpaces() throws PivotalClientApiException {
    PcfCommandRequest pcfCommandRequest = getPcfInfraMappingDataRequest(FETCH_SPACE);
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    List<String> spaces = Arrays.asList("space1", "space2");
    doReturn(spaces).when(pcfDeploymentManager).getSpacesForOrganization(any());

    PcfCommandExecutionResponse response = pcfDataFetchCommandTaskHandler.executeTaskInternal(
        pcfCommandRequest, encryptedDataDetails, executionLogCallback, false);

    ArgumentCaptor<PcfRequestConfig> argument = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(pcfDeploymentManager).getSpacesForOrganization(argument.capture());
    PcfRequestConfig pcfRequestConfig = argument.getValue();

    assertPcfRequestConfig(pcfCommandRequest, pcfRequestConfig);

    PcfCommandResponse pcfCommandResponse = response.getPcfCommandResponse();
    assertThat(pcfCommandResponse).isInstanceOf(PcfInfraMappingDataResponse.class);
    assertThat(((PcfInfraMappingDataResponse) pcfCommandResponse).getSpaces()).isEqualTo(spaces);
    assertThat(pcfCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfCommandResponse.getOutput()).isEqualTo(StringUtils.EMPTY);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetRoutes() throws PivotalClientApiException {
    PcfCommandRequest pcfCommandRequest = getPcfInfraMappingDataRequest(FETCH_ROUTE);
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    List<String> routes = Arrays.asList("route1", "route2");
    doReturn(routes).when(pcfDeploymentManager).getRouteMaps(any());

    PcfCommandExecutionResponse response = pcfDataFetchCommandTaskHandler.executeTaskInternal(
        pcfCommandRequest, encryptedDataDetails, executionLogCallback, false);

    ArgumentCaptor<PcfRequestConfig> argument = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(pcfDeploymentManager).getRouteMaps(argument.capture());
    PcfRequestConfig pcfRequestConfig = argument.getValue();

    assertPcfRequestConfig(pcfCommandRequest, pcfRequestConfig);

    PcfCommandResponse pcfCommandResponse = response.getPcfCommandResponse();
    assertThat(pcfCommandResponse).isInstanceOf(PcfInfraMappingDataResponse.class);
    assertThat(((PcfInfraMappingDataResponse) pcfCommandResponse).getRouteMaps()).isEqualTo(routes);
    assertThat(pcfCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfCommandResponse.getOutput()).isEqualTo(StringUtils.EMPTY);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testRunningCount() throws PivotalClientApiException {
    PcfCommandRequest pcfCommandRequest = getPcfInfraMappingDataRequest(RUNNING_COUNT);
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    ApplicationSummary applicationSummary1 = getApplicationSummary("__1", "RUNNING", "id1");
    ApplicationSummary applicationSummary2 = getApplicationSummary("__2", "STOPPED", "id2");
    List<ApplicationSummary> apps = Arrays.asList(applicationSummary1, applicationSummary2);

    doReturn(apps).when(pcfDeploymentManager).getPreviousReleases(any(), any());

    PcfCommandExecutionResponse response = pcfDataFetchCommandTaskHandler.executeTaskInternal(
        pcfCommandRequest, encryptedDataDetails, executionLogCallback, false);

    ArgumentCaptor<PcfRequestConfig> argument = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(pcfDeploymentManager).getPreviousReleases(argument.capture(), any());
    PcfRequestConfig pcfRequestConfig = argument.getValue();

    assertPcfRequestConfig(pcfCommandRequest, pcfRequestConfig);

    PcfCommandResponse pcfCommandResponse = response.getPcfCommandResponse();
    assertThat(pcfCommandResponse).isInstanceOf(PcfInfraMappingDataResponse.class);
    assertThat(((PcfInfraMappingDataResponse) pcfCommandResponse).getRunningInstanceCount()).isEqualTo(1);
    assertThat(pcfCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfCommandResponse.getOutput()).isEqualTo(StringUtils.EMPTY);
  }

  @NotNull
  private ApplicationSummary getApplicationSummary(String s, String running, String id) {
    return ApplicationSummary.builder()
        .name(APP_NAME + s)
        .runningInstances(1)
        .instances(2)
        .requestedState(running)
        .diskQuota(2)
        .id(id)
        .memoryLimit(1)
        .build();
  }

  private void assertPcfRequestConfig(PcfCommandRequest pcfCommandRequest, PcfRequestConfig pcfRequestConfig) {
    assertThat(pcfRequestConfig.getEndpointUrl()).isEqualTo(pcfCommandRequest.getPcfConfig().getEndpointUrl());
    assertThat(pcfRequestConfig.getUserName())
        .isEqualTo(String.valueOf(pcfCommandRequest.getPcfConfig().getUsername()));
    assertThat(pcfRequestConfig.getPassword())
        .isEqualTo(String.valueOf(pcfCommandRequest.getPcfConfig().getPassword()));
    assertThat(pcfRequestConfig.isLimitPcfThreads()).isEqualTo(pcfCommandRequest.isLimitPcfThreads());
    assertThat(pcfRequestConfig.isLimitPcfThreads()).isEqualTo(pcfCommandRequest.isLimitPcfThreads());
    assertThat(pcfRequestConfig.isIgnorePcfConnectionContextCache())
        .isEqualTo(pcfCommandRequest.isIgnorePcfConnectionContextCache());
    assertThat(pcfRequestConfig.getTimeOutIntervalInMins()).isEqualTo(pcfCommandRequest.getTimeoutIntervalInMin());
  }

  private PcfInfraMappingDataRequest getPcfInfraMappingDataRequest(PcfInfraMappingDataRequest.ActionType actionType) {
    return PcfInfraMappingDataRequest.builder()
        .applicationNamePrefix(APP_NAME)
        .actionType(actionType)
        .limitPcfThreads(false)
        .ignorePcfConnectionContextCache(false)
        .timeoutIntervalInMin(1)
        .organization(ORGANIZATION)
        .space(SPACE)
        .pcfConfig(PcfConfig.builder()
                       .endpointUrl(ENDPOINT_URL)
                       .username(USERNAME.toCharArray())
                       .password(PASSWORD.toCharArray())
                       .build())
        .build();
  }
}

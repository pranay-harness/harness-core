package io.harness.delegate.task.helm;

import static io.harness.exception.WingsException.USER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmValidationParams;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.HelmClientException;
import io.harness.k8s.model.HelmVersion;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HttpHelmValidationHandlerTest extends CategoryTest {
  @Mock private HelmTaskHelperBase helmTaskHelperBase;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private SecretDecryptionService decryptionService;

  @InjectMocks private HttpHelmValidationHandler validationHandler;
  private String workingDir = "/repository/helm-validation/helm-folder";
  private SecretRefData passwordSecret = SecretRefData.builder()
                                             .identifier("passwordRef")
                                             .scope(Scope.ACCOUNT)
                                             .decryptedValue("password".toCharArray())
                                             .build();
  private SecretRefData usernameSecret = SecretRefData.builder()
                                             .identifier("usernameRef")
                                             .scope(Scope.ACCOUNT)
                                             .decryptedValue("test".toCharArray())
                                             .build();

  private String errorMessage = "Something went wrong";
  private String generalError = "GENERAL_ERROR";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoWithSuccess() throws Exception {
    stubSuccessfulCalls();
    verifySuccessfulCalls(buildValidationParams(HttpHelmAuthType.USER_PASSWORD, false));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoBothUsernameAndPasswordSecretRefWithSuccess() throws Exception {
    stubSuccessfulCalls();
    verifySuccessfulCalls(buildValidationParams(HttpHelmAuthType.USER_PASSWORD, true));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoAnonymousWithSuccess() throws Exception {
    stubSuccessfulCalls();
    verifySuccessfulCalls(buildValidationParams(HttpHelmAuthType.ANONYMOUS, false));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoWithFailure() throws Exception {
    stubFailureCalls();
    verifyFailureCalls(buildValidationParams(HttpHelmAuthType.USER_PASSWORD, false));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoBothUsernameAndPasswordSecretRefWithFailure() throws Exception {
    stubFailureCalls();
    verifyFailureCalls(buildValidationParams(HttpHelmAuthType.USER_PASSWORD, true));
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAddRepoAnonymousWithFailure() throws Exception {
    stubFailureCalls();
    verifyFailureCalls(buildValidationParams(HttpHelmAuthType.ANONYMOUS, false));
  }

  private HttpHelmValidationParams buildValidationParams(HttpHelmAuthType authType, boolean isUsernameSecretRef) {
    HttpHelmAuthenticationDTO auth = (authType == HttpHelmAuthType.USER_PASSWORD)
        ? HttpHelmAuthenticationDTO.builder()
              .authType(HttpHelmAuthType.USER_PASSWORD)
              .credentials(HttpHelmUsernamePasswordDTO.builder()
                               .username(!isUsernameSecretRef ? "test" : null)
                               .usernameRef(isUsernameSecretRef ? usernameSecret : null)
                               .passwordRef(passwordSecret)
                               .build())
              .build()
        : HttpHelmAuthenticationDTO.builder().authType(HttpHelmAuthType.ANONYMOUS).build();

    return HttpHelmValidationParams.builder()
        .connectorName("testhttphelmrepo")
        .httpHelmConnectorDTO(HttpHelmConnectorDTO.builder().helmRepoUrl("localhost").auth(auth).build())
        .encryptionDataDetails(Collections.emptyList())
        .build();
  }

  private void stubSuccessfulCalls() throws Exception {
    doReturn(workingDir).when(helmTaskHelperBase).createNewDirectoryAtPath(anyString());
    doNothing().when(helmTaskHelperBase).initHelm(anyString(), any(), anyLong());
    doReturn(HttpHelmUsernamePasswordDTO.builder().build()).when(decryptionService).decrypt(any(), anyList());
    doNothing()
        .when(helmTaskHelperBase)
        .addRepo(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(), anyLong());

    doNothing().when(helmTaskHelperBase).removeRepo(anyString(), anyString(), any(), anyLong());
    doNothing().when(helmTaskHelperBase).cleanup(anyString());
  }

  private void stubFailureCalls() throws Exception {
    doReturn(workingDir).when(helmTaskHelperBase).createNewDirectoryAtPath(anyString());
    doNothing().when(helmTaskHelperBase).initHelm(anyString(), any(), anyLong());
    doReturn(HttpHelmUsernamePasswordDTO.builder().build()).when(decryptionService).decrypt(any(), anyList());
    doThrow(new HelmClientException(errorMessage, USER))
        .when(helmTaskHelperBase)
        .addRepo(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(), anyLong());

    doReturn(generalError).when(ngErrorHelper).getErrorSummary(generalError);
    doReturn(ErrorDetail.builder().message(generalError).build()).when(ngErrorHelper).createErrorDetail(generalError);
  }

  private void verifySuccessfulCalls(HttpHelmValidationParams connectorValidationParams) throws Exception {
    HttpHelmAuthType authType = connectorValidationParams.getHttpHelmConnectorDTO().getAuth().getAuthType();
    ConnectorValidationResult result = validationHandler.validate(connectorValidationParams, "1234");
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);

    verify(helmTaskHelperBase, times(1)).createNewDirectoryAtPath(eq("./repository/helm-validation"));
    verify(helmTaskHelperBase, times(1)).initHelm(eq(workingDir), eq(HelmVersion.V3), anyLong());
    if (authType == HttpHelmAuthType.USER_PASSWORD) {
      verify(helmTaskHelperBase, times(1))
          .addRepo(anyString(), eq("testhttphelmrepo"), eq("localhost"), eq("test"), eq("password".toCharArray()),
              eq(workingDir), eq(HelmVersion.V3), anyLong());
    } else {
      verify(helmTaskHelperBase, times(1))
          .addRepo(anyString(), eq("testhttphelmrepo"), eq("localhost"), eq(null), eq(null), eq(workingDir),
              eq(HelmVersion.V3), anyLong());
    }
    verify(helmTaskHelperBase, times(1)).removeRepo(anyString(), eq(workingDir), eq(HelmVersion.V3), anyLong());
    verify(helmTaskHelperBase, times(1)).cleanup(eq(workingDir));
  }

  private void verifyFailureCalls(HttpHelmValidationParams connectorValidationParams) throws Exception {
    HttpHelmAuthType authType = connectorValidationParams.getHttpHelmConnectorDTO().getAuth().getAuthType();
    ConnectorValidationResult result = validationHandler.validate(connectorValidationParams, "1234");
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
    assertThat(result.getErrorSummary()).isEqualTo(generalError);
    assertThat(result.getErrors().size()).isEqualTo(1);
    assertThat(result.getErrors().get(0).getMessage()).isEqualTo(generalError);

    verify(helmTaskHelperBase, times(1)).createNewDirectoryAtPath(eq("./repository/helm-validation"));
    verify(helmTaskHelperBase, times(1)).initHelm(eq(workingDir), eq(HelmVersion.V3), anyLong());
    if (authType == HttpHelmAuthType.USER_PASSWORD) {
      verify(helmTaskHelperBase, times(1))
          .addRepo(anyString(), eq("testhttphelmrepo"), eq("localhost"), eq("test"), eq("password".toCharArray()),
              eq(workingDir), eq(HelmVersion.V3), anyLong());
    } else {
      verify(helmTaskHelperBase, times(1))
          .addRepo(anyString(), eq("testhttphelmrepo"), eq("localhost"), eq(null), eq(null), eq(workingDir),
              eq(HelmVersion.V3), anyLong());
    }
    verify(helmTaskHelperBase, times(0)).removeRepo(anyString(), eq(workingDir), eq(HelmVersion.V3), anyLong());
    verify(helmTaskHelperBase, times(0)).cleanup(eq(workingDir));
  }
}

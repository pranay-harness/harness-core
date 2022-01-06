/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.azure.KeyVaultADALAuthenticator;
import io.harness.helpers.ext.cyberark.CyberArkReadResponse;
import io.harness.helpers.ext.cyberark.CyberArkRestClient;
import io.harness.helpers.ext.cyberark.CyberArkRestClientFactory;
import io.harness.rule.Owner;

import software.wings.beans.CyberArkConfig;

import com.microsoft.azure.keyvault.KeyVaultClient;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author marklu on 2019-03-06
 */
@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyVaultADALAuthenticator.class, KeyVaultClient.class, CyberArkRestClientFactory.class})
@PowerMockIgnore(
    {"okhttp3.*", "javax.security.*", "org.apache.http.conn.ssl.", "javax.net.ssl.", "javax.crypto.*", "sun.*"})
public class SecretManagementDelegateServiceTest extends CategoryTest {
  private SecretManagementDelegateServiceImpl secretManagementDelegateService;

  @Before
  public void setup() throws Exception {
    initMocks(this);
    secretManagementDelegateService = new SecretManagementDelegateServiceImpl();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCyberArkConfigValidation_shouldPass() throws IOException {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("/");
    mockStatic(CyberArkRestClientFactory.class);
    CyberArkRestClient cyberArkRestClient = mock(CyberArkRestClient.class);
    CyberArkReadResponse cyberArkReadResponse = mock(CyberArkReadResponse.class);
    Call<CyberArkReadResponse> cyberArkReadResponseCall = mock(Call.class);
    Response<CyberArkReadResponse> cyberArkReadResponseResponse = Response.success(cyberArkReadResponse);
    when(CyberArkRestClientFactory.create(cyberArkConfig)).thenReturn(cyberArkRestClient);
    when(cyberArkRestClient.readSecret(anyString(), anyString())).thenReturn(cyberArkReadResponseCall);
    when(cyberArkReadResponseCall.execute()).thenReturn(cyberArkReadResponseResponse);
    secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCyberArkConfigValidation_shouldFail() throws IOException {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("/");
    mockStatic(CyberArkRestClientFactory.class);
    CyberArkRestClient cyberArkRestClient = mock(CyberArkRestClient.class);
    Call<CyberArkReadResponse> cyberArkReadResponseCall = mock(Call.class);
    Response<CyberArkReadResponse> cyberArkReadResponseResponse =
        Response.error(ResponseBody.create(MediaType.parse("application/json"), "error"),
            new okhttp3.Response.Builder()
                .message("xyz")
                .code(500)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .build());
    when(CyberArkRestClientFactory.create(cyberArkConfig)).thenReturn(cyberArkRestClient);
    when(cyberArkRestClient.readSecret(anyString(), anyString())).thenReturn(cyberArkReadResponseCall);
    when(cyberArkReadResponseCall.execute()).thenReturn(cyberArkReadResponseResponse);

    assertThatCode(() -> secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig))
        .isInstanceOf(SecretManagementDelegateException.class)
        .hasMessage("Failed to query the CyberArk REST endpoint. Please check your configurations and try again");
  }

  private CyberArkConfig getCyberArkConfig(String url) {
    final CyberArkConfig cyberArkConfig = new CyberArkConfig();
    cyberArkConfig.setName("TestCyberArk");
    cyberArkConfig.setDefault(true);
    cyberArkConfig.setCyberArkUrl(url);
    cyberArkConfig.setAppId(generateUuid());
    return cyberArkConfig;
  }
}

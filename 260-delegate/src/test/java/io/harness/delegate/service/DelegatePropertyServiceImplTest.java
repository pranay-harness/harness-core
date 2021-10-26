package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.MATT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.dmsclient.DelegateAgentDmsClient;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;
import io.harness.managerclient.HttpsCertRequirement;
import io.harness.managerclient.HttpsCertRequirement.CertRequirement;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;

public class DelegatePropertyServiceImplTest extends CategoryTest {
  private static final String ACCOUNT_ID = "account_id";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private DelegateAgentDmsClient delegateAgentDmsClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private Call<RestResponse<String>> propertyResponse;

  @InjectMocks @Inject DelegatePropertyServiceImpl propertyService;

  @Before
  public void setUp() throws IOException {
    when(delegateAgentDmsClient.getDelegateProperties(anyString(), any())).thenReturn(propertyResponse);
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void shouldHitCache() throws Exception {
    doReturn(
        Response.success(new RestResponse<>(
            GetDelegatePropertiesResponse.newBuilder()
                .addResponseEntry(Any.pack(
                    HttpsCertRequirement.newBuilder().setCertRequirement(CertRequirement.CERTIFICATE_REQUIRED).build()))
                .build()
                .toString())))
        .when(propertyResponse)
        .execute();
    GetDelegatePropertiesRequest request = GetDelegatePropertiesRequest.newBuilder().setAccountId(ACCOUNT_ID).build();
    GetDelegatePropertiesResponse response = propertyService.getDelegateProperties(request);
    propertyService.getDelegateProperties(request);

    verify(delegateAgentDmsClient, times(1)).getDelegateProperties(anyString(), any());

    assertThat(response.getResponseEntry(0).unpack(HttpsCertRequirement.class).getCertRequirement())
        .isEqualTo(CertRequirement.CERTIFICATE_REQUIRED);
  }
}

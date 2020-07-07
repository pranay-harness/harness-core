package io.harness.ng.core.services.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.services.api.NgSecretUsageService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.BambooConfig;

import java.io.IOException;
import java.util.List;

public class NGSecretUsageServiceTest extends BaseTest {
  private SecretManagerClient secretManagerClient;
  @Inject private NgSecretUsageService ngSecretUsageService;

  @Before
  public void setup() {
    secretManagerClient = mock(SecretManagerClient.class);
    ngSecretUsageService = new NGSecretUsageServiceImpl(secretManagerClient);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testGetEncryptionDetails() throws IOException {
    EncryptableSetting encryptableSetting = random(BambooConfig.class);
    EncryptedDataDetail encryptedDataDetail =
        EncryptedDataDetail.builder().encryptedData(random(EncryptedRecordData.class)).build();
    List<EncryptedDataDetail> encryptedDataDetails = Lists.newArrayList(encryptedDataDetail);
    Call<RestResponse<List<EncryptedDataDetail>>> request = mock(Call.class);

    when(secretManagerClient.getEncryptionDetails(any(), any(), any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(encryptedDataDetails)));

    List<EncryptedDataDetail> listReturned = ngSecretUsageService.getEncryptionDetails(encryptableSetting, null, null);

    Assertions.assertThat(listReturned).isNotEmpty();
    Assertions.assertThat(listReturned).isEqualTo(encryptedDataDetails);
  }
}

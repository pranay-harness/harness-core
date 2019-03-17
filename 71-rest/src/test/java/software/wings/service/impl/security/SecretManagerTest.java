package software.wings.service.impl.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AwsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.UsageRestrictions;

import java.util.ArrayList;
import java.util.List;

public class SecretManagerTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private AppService appService;
  @Mock private EnvironmentService envService;
  @Inject @InjectMocks private SecretManagerImpl secretManager;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    when(usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(anyString(), eq(Action.READ)))
        .thenReturn(mock(RestrictionsAndAppEnvMap.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testMaskEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    secretManager.maskEncryptedFields(awsConfig);
    assertArrayEquals(awsConfig.getSecretKey(), ENCRYPTED_FIELD_MASK.toCharArray());
  }

  @Test
  @Category(UnitTests.class)
  public void testResetUnchangedEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    AwsConfig maskedAwsConfig = AwsConfig.builder()
                                    .accountId(ACCOUNT_ID)
                                    .accessKey(ACCESS_KEY)
                                    .secretKey(ENCRYPTED_FIELD_MASK.toCharArray())
                                    .build();
    secretManager.resetUnchangedEncryptedFields(awsConfig, maskedAwsConfig);
    assertArrayEquals(maskedAwsConfig.getSecretKey(), SECRET_KEY);
  }

  @Test
  @Category(UnitTests.class)
  public void testListSecrets_withEmptyResponse() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse()).thenReturn(getSecretList(pageSize));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertEquals(offset + pageSize, finalPageResponse.getStart());
    assertEquals(pageSize, finalPageResponse.getPageSize());
    assertEquals(0, finalPageResponse.getTotal().longValue());

    verify(wingsPersistence, times(1)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testListSecrets_withLargePageSize_multipleBatches() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 10000;
    int offset = 0;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse())
        .thenReturn(getSecretList(PageRequest.DEFAULT_UNLIMITED), getSecretList(PageRequest.DEFAULT_UNLIMITED),
            getSecretList(3));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(true);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertEquals(2 * PageRequest.DEFAULT_UNLIMITED + 3, finalPageResponse.getStart());
    assertEquals(pageSize, finalPageResponse.getPageSize());
    assertEquals(2 * PageRequest.DEFAULT_UNLIMITED + 3, finalPageResponse.getTotal().longValue());

    verify(wingsPersistence, times(3)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testListSecrets_withFullResponse_singleBatch() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse()).thenReturn(getSecretList(pageSize * 2));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(false, false, false, true);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertEquals(offset + pageSize + 3, finalPageResponse.getStart());
    assertEquals(pageSize, finalPageResponse.getPageSize());
    assertEquals(pageSize, finalPageResponse.getTotal().longValue());

    verify(wingsPersistence, times(1)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testListSecrets_withFullResponse_multiBatches() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse())
        .thenReturn(getSecretList(pageSize * 2), getSecretList(pageSize * 2), getSecretList(pageSize));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);

    // Filter out the first 3 records based on usage restriction.
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(true, true, true, false);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    verify(wingsPersistence, times(3)).query(eq(EncryptedData.class), any(PageRequest.class));
    assertEquals(pageSize, finalPageResponse.getPageSize());
    assertEquals(3, finalPageResponse.getTotal().longValue());
    assertEquals(offset + 5 * pageSize, finalPageResponse.getStart());
  }

  private List<EncryptedData> getSecretList(int num) {
    List<EncryptedData> secretList = new ArrayList<>();
    for (int i = 0; i < num; i++) {
      secretList.add(new EncryptedData());
    }
    return secretList;
  }

  private PageRequest<EncryptedData> getPageRequest(int offset, int pageSize) {
    return PageRequestBuilder.aPageRequest()
        .withLimit(String.valueOf(pageSize))
        .withOffset(String.valueOf(offset))
        .build();
  }
}

package software.wings.service.impl.splunk;

import static io.harness.rule.OwnerRule.SRIRAM;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.Service;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.security.EncryptionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SplunkDelegateServiceImplTest extends WingsBaseTest {
  SplunkConfig config;

  @Before
  public void setUp() {
    config = SplunkConfig.builder()
                 .accountId("123")
                 .splunkUrl("https://input-prd-p-429h4vj2lsng.cloud.splunk.com:8089")
                 .username("123")
                 .password("123".toCharArray())
                 .build();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void initSplunkService() throws IllegalAccessException {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    FieldUtils.writeField(splunkDelegateService, "encryptionService", new EncryptionServiceImpl(null, null), true);
    splunkDelegateService.initSplunkService(config, Lists.emptyList());
    verify(splunkDelegateService, times(1)).initSplunkServiceWithToken(config);
    verify(splunkDelegateService, times(1)).initSplunkServiceWithBasicAuth(config);
  }

  @Test(expected = Exception.class)
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void initSplunkServiceOnlyToken() throws IllegalAccessException {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    when(splunkDelegateService.initSplunkServiceWithToken(config)).thenReturn(Mockito.mock(Service.class));
    FieldUtils.writeField(splunkDelegateService, "encryptionService", new EncryptionServiceImpl(null, null), true);
    splunkDelegateService.initSplunkService(config, Lists.emptyList());
    verify(splunkDelegateService, times(1)).initSplunkServiceWithToken(config);
    verify(splunkDelegateService, times(1)).initSplunkServiceWithBasicAuth(config);
  }
}

package software.wings.service.impl.splunk;

import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.SRIRAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.JobArgs;
import com.splunk.SavedSearch;
import com.splunk.SavedSearchCollection;
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
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.security.EncryptionServiceImpl;

import java.util.ArrayList;
import java.util.List;

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
    when(splunkDelegateService.initSplunkServiceWithToken(config)).thenReturn(mock(Service.class));
    FieldUtils.writeField(splunkDelegateService, "encryptionService", new EncryptionServiceImpl(null, null), true);
    splunkDelegateService.initSplunkService(config, Lists.emptyList());
    verify(splunkDelegateService, times(1)).initSplunkServiceWithToken(config);
    verify(splunkDelegateService, times(1)).initSplunkServiceWithBasicAuth(config);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSavedSearches_withCorrectResults() {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    Service service = mock(Service.class);
    doReturn(service).when(splunkDelegateService).initSplunkService(any(), anyList());
    SplunkConfig splunkConfig = mock(SplunkConfig.class);
    SavedSearchCollection savedSearchCollection = mock(SavedSearchCollection.class);
    SavedSearch savedSearch = mock(SavedSearch.class);
    when(savedSearch.getSearch()).thenReturn("search query");
    when(savedSearch.getTitle()).thenReturn("search query title");
    when(savedSearchCollection.values()).thenReturn(Lists.newArrayList(savedSearch));
    when(service.getSavedSearches(any(JobArgs.class))).thenReturn(savedSearchCollection);
    List<SplunkSavedSearch> splunkSavedSearches =
        splunkDelegateService.getSavedSearches(splunkConfig, new ArrayList<>());
    assertThat(splunkSavedSearches).hasSize(1);
    assertThat(splunkSavedSearches.get(0).getSearchQuery()).isEqualTo("search query");
    assertThat(splunkSavedSearches.get(0).getTitle()).isEqualTo("search query title");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSavedSearches_withException() {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    Service service = mock(Service.class);
    doReturn(service).when(splunkDelegateService).initSplunkService(any(), anyList());
    SplunkConfig splunkConfig = mock(SplunkConfig.class);
    SavedSearchCollection savedSearchCollection = mock(SavedSearchCollection.class);
    SavedSearch savedSearch = mock(SavedSearch.class);
    when(savedSearch.getSearch()).thenReturn("search query");
    when(savedSearch.getTitle()).thenReturn("search query title");
    when(savedSearchCollection.values()).thenReturn(Lists.newArrayList(savedSearch));
    when(service.getSavedSearches(any(JobArgs.class))).thenThrow(new RuntimeException("from test"));
    assertThatThrownBy(() -> splunkDelegateService.getSavedSearches(splunkConfig, new ArrayList<>()))
        .hasMessage("from test");
  }
}

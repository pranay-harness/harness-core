package software.wings.service.impl.splunk;

import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.SRIRAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobCollection;
import com.splunk.SavedSearch;
import com.splunk.SavedSearchCollection;
import com.splunk.Service;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVHistogram;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetHistogram() throws IOException {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    Service service = mock(Service.class);
    doReturn(service).when(splunkDelegateService).initSplunkService(any(), anyList());
    SplunkConfig splunkConfig = mock(SplunkConfig.class);
    JobCollection jobCollection = mock(JobCollection.class);
    when(service.getJobs()).thenReturn(jobCollection);
    Job job = mock(Job.class);
    when(jobCollection.create(any(), any())).thenReturn(job);
    when(job.getResults(any()))
        .thenReturn(getSplunkJsonResponseInputStream("splunk_json_response_for_histogram_query.json"));
    CVHistogram histogram = splunkDelegateService.getHistogram(splunkConfig, new ArrayList<>(), "exception");
    verify(jobCollection).create(eq("search exception | timechart count span=6h | table _time, count"), any());
    assertThat(histogram.getQuery()).isEqualTo("exception");
    assertThat(histogram.getBars()).hasSize(29);
    assertThat(histogram.getBars().get(0))
        .isEqualTo(CVHistogram.Bar.builder().count(10).timestamp(1589889600000L).build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSamples() throws IOException {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    Service service = mock(Service.class);
    doReturn(service).when(splunkDelegateService).initSplunkService(any(), anyList());
    SplunkConfig splunkConfig = mock(SplunkConfig.class);
    JobCollection jobCollection = mock(JobCollection.class);
    when(service.getJobs()).thenReturn(jobCollection);
    Job job = mock(Job.class);
    when(jobCollection.create(any(), any())).thenReturn(job);
    when(job.getResults(any()))
        .thenReturn(getSplunkJsonResponseInputStream("splunk_json_response_for_samples_query.json"));
    SplunkSampleResponse samples = splunkDelegateService.getSamples(splunkConfig, new ArrayList<>(), "exception");
    verify(jobCollection).create(eq("search exception | head 10"), any());
    assertThat(samples.getRawSampleLogs()).hasSize(10);
    assertThat(samples.getRawSampleLogs().get(0))
        .isEqualTo(
            "2020-05-26 18:10:39,278 [GitChangeSet] INFO  software.wings.yaml.gitSync.GitChangeSetRunnable - Not continuing with GitChangeSetRunnable job");
    assertThat(samples.getSample())
        .isEqualTo(JsonUtils.asObject("{\n"
                + "            \"linecount\": \"1\",\n"
                + "            \"splunk_server\": \"splunk-dev\",\n"
                + "            \"host\": \"splunk-dev\",\n"
                + "            \"index\": \"main\",\n"
                + "            \"sourcetype\": \"delegate-logs-local\",\n"
                + "            \"source\": \"delegate.2020-05-21.log\"\n"
                + "        }",
            HashMap.class));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSample_withEmptyResults() throws IOException {
    SplunkDelegateServiceImpl splunkDelegateService = spy(new SplunkDelegateServiceImpl());
    Service service = mock(Service.class);
    doReturn(service).when(splunkDelegateService).initSplunkService(any(), anyList());
    SplunkConfig splunkConfig = mock(SplunkConfig.class);
    JobCollection jobCollection = mock(JobCollection.class);
    when(service.getJobs()).thenReturn(jobCollection);
    Job job = mock(Job.class);
    when(jobCollection.create(any(), any())).thenReturn(job);
    when(job.getResults(any())).thenReturn(getSplunkJsonResponseInputStream("splunk_json_search_response_empty.json"));
    SplunkSampleResponse samples = splunkDelegateService.getSamples(splunkConfig, new ArrayList<>(), "exception");
    verify(jobCollection).create(eq("search exception | head 10"), any());
    assertThat(samples.getRawSampleLogs()).isEmpty();
    assertThat(samples.getSample()).isEmpty();
  }

  private InputStream getSplunkJsonResponseInputStream(String jsonFile) throws IOException {
    String splunkResponse =
        IOUtils.toString(SplunkDelegateServiceImpl.class.getResourceAsStream(jsonFile), StandardCharsets.UTF_8.name());
    return IOUtils.toInputStream(splunkResponse, Charset.defaultCharset());
  }
}

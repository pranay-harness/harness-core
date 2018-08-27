package software.wings.helpers.ext.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.api.client.util.Maps;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class GcsServiceTest extends WingsBaseTest {
  @Mock private GcpHelperService gcpHelperService;
  @Mock private Storage gcsStorageService;
  @Mock private Storage.Buckets bucketsObj;
  @Mock private Storage.Buckets.List listRequest;
  @Inject private GcsService gcsService;
  private static final String TEST_PROJECT_ID = "test";
  private static String serviceAccountFileContent = "{\"project_id\":\"test\"}";

  private static final GcpConfig gcpConfig = GcpConfig.builder()
                                                 .accountId("accountId")
                                                 .serviceAccountKeyFileContent(serviceAccountFileContent.toCharArray())
                                                 .build();

  @Before
  public void setUp() {
    setInternalState(gcsService, "gcpHelperService", gcpHelperService);
  }

  @Test
  public void shouldGetProject() {
    assertThat(gcsService.getProject(gcpConfig, null)).isNotNull().isEqualTo(TEST_PROJECT_ID);
  }

  @Test
  public void shouldListBuckets() throws IOException {
    HashMap<String, String> bucketList = Maps.newHashMap();
    bucketList.put("bucket", "bucketId");
    Buckets buckets = new Buckets();
    Bucket bucket = new Bucket();
    bucket.setName("bucket");
    bucket.setId("bucketId");
    buckets.setItems(Arrays.asList(bucket));
    when(gcpHelperService.getGcsStorageService(gcpConfig, null)).thenReturn(gcsStorageService);
    when(gcsStorageService.buckets()).thenReturn(bucketsObj);
    when(bucketsObj.list(TEST_PROJECT_ID)).thenReturn(listRequest);
    when(listRequest.execute()).thenReturn(buckets);
    assertThat(gcsService.listBuckets(gcpConfig, TEST_PROJECT_ID, null)).isNotNull().hasSize(1).containsKeys("bucket");
  }
}

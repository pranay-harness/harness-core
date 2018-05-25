package software.wings.helpers.ext.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.service.impl.GcpHelperService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class GcsServiceTest extends WingsBaseTest {
  @Inject GcpHelperService gcpHelperService;
  @Inject private GcsService gcsService;
  @Inject @InjectMocks private DelegateFileManager delegateFileManager;
  private static final String TEST_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String TEST_APP_ID = "qhG0adpZSbyb1RWyhm3qMQ";
  private static String serviceAccountFileContent = "";

  private static SettingAttribute GCP_PROVIDER_SETTING =
      aSettingAttribute()
          .withUuid("GCP_ID")
          .withAccountId(TEST_ACCOUNT_ID)
          .withAppId(TEST_APP_ID)
          .withValue(GcpConfig.builder()
                         .accountId(TEST_ACCOUNT_ID)
                         .serviceAccountKeyFileContent(serviceAccountFileContent.toCharArray())
                         .build())
          .build();

  @Before
  public void setUp() {
    setInternalState(gcsService, "gcpHelperService", gcpHelperService);
    try {
      // Read google credentials
      ClassLoader classLoader = getClass().getClassLoader();
      File file =
          new File(classLoader.getResource("software/wings/service/impl/webhook/google_credentials.json").getFile());
      serviceAccountFileContent = FileUtils.readFileToString(file, Charset.defaultCharset());
      GCP_PROVIDER_SETTING.setValue(GcpConfig.builder()
                                        .accountId(TEST_ACCOUNT_ID)
                                        .serviceAccountKeyFileContent(serviceAccountFileContent.toCharArray())
                                        .build());
    } catch (IOException ex) {
    }
  }

  @Test
  public void shouldCreateBucket() {
    GcpConfig gcpConfig = gcsService.validateAndGetCredentials(GCP_PROVIDER_SETTING);

    // Delete bucket in case exists
    gcsService.deleteBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Create a bucket
    gcsService.createBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Check if created
    Map<String, String> buckets = gcsService.listBuckets(gcpConfig, Collections.EMPTY_LIST);
    assertThat(buckets).containsKey("gcs-test-bucket-must-be-unique");
  }

  @Test
  public void shouldDeleteBucket() {
    GcpConfig gcpConfig = gcsService.validateAndGetCredentials(GCP_PROVIDER_SETTING);

    // Create a bucket
    gcsService.createBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Delete bucket in case exists
    gcsService.deleteBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Check if deleted
    Map<String, String> buckets = gcsService.listBuckets(gcpConfig, Collections.EMPTY_LIST);
    assertThat(buckets).doesNotContainKey("gcs-test-bucket-must-be-unique");
  }

  @Test
  public void shouldListBuckets() {
    GcpConfig gcpConfig = gcsService.validateAndGetCredentials(GCP_PROVIDER_SETTING);

    // Delete bucket in case exists
    gcsService.deleteBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Create a bucket
    gcsService.createBucket(gcpConfig, Collections.EMPTY_LIST, "gcs-test-bucket-must-be-unique");

    // Check if list works
    Map<String, String> buckets = gcsService.listBuckets(gcpConfig, Collections.EMPTY_LIST);
    assertThat(buckets).containsKey("gcs-test-bucket-must-be-unique");
  }
}

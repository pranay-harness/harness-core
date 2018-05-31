package software.wings.helpers.ext.gcs;

import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.service.impl.GcpHelperService;

import java.util.Map;

@Ignore("Unit tests should not depend on external resources")
public class GcsServiceTest extends WingsBaseTest {
  @Mock GcpHelperService gcpHelperService;
  @Inject private GcsService gcsService;
  @Inject @InjectMocks private DelegateFileManager delegateFileManager;
  private static final String TEST_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String TEST_APP_ID = "qhG0adpZSbyb1RWyhm3qMQ";
  private static String serviceAccountFileContent = "";

  private static final GcpConfig gcpConfig = GcpConfig.builder().accountId("accountId").build();

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
  }

  @Test
  @Ignore
  public void shouldListBuckets() {
    when(gcsService.listBuckets(gcpConfig, null)).thenReturn(null);
    Map<String, String> buckets = gcsService.listBuckets(gcpConfig, null);
  }
}

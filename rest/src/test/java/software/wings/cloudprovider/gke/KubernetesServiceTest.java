package software.wings.cloudprovider.gke;

import org.junit.Before;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;

import javax.inject.Inject;

import static software.wings.beans.KubernetesConfig.Builder.aKubernetesConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.PASSWORD;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesServiceTest extends WingsBaseTest {
  private static final int DESIRED_CAPACITY = 2;
  public static final String API_SERVER_URL = "apiServerUrl";
  public static final String USERNAME = "username";

  @Inject @InjectMocks private KubernetesContainerService kubernetesContainerService;

  private SettingAttribute connectorConfig = aSettingAttribute()
                                                 .withValue(aKubernetesConfig()
                                                                .withApiServerUrl(API_SERVER_URL)
                                                                .withUsername(USERNAME)
                                                                .withPassword(PASSWORD)
                                                                .build())
                                                 .build();

  @Before
  public void setUp() throws Exception {}
}

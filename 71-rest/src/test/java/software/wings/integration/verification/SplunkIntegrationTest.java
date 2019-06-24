package software.wings.integration.verification;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import com.splunk.Service;
import io.harness.category.element.IntegrationTests;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.SplunkConfig;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SplunkIntegrationTest extends BaseIntegrationTest {
  @Inject SplunkDelegateService splunkDelegateService; // = new SplunkDelegateServiceImpl();
  @Inject private ScmSecret scmSecret;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void initSplunkServiceWithToken()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    SplunkConfig config =
        SplunkConfig.builder()
            .accountId(accountId)
            .splunkUrl("https://input-prd-p-429h4vj2lsng.cloud.splunk.com:8089")
            .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
            .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
            .build();

    Method method =
        splunkDelegateService.getClass().getDeclaredMethod("initSplunkServiceWithToken", SplunkConfig.class);
    method.setAccessible(true);
    Object r = method.invoke(splunkDelegateService, config);
    assertTrue(((Service) r).getToken().startsWith("Splunk"));
  }

  @Test
  @Category(IntegrationTests.class)
  public void initSplunkServiceWithBasicAuth()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    SplunkConfig config =
        SplunkConfig.builder()
            .accountId(accountId)
            .splunkUrl("https://input-prd-p-429h4vj2lsng.cloud.splunk.com:8089")
            .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
            .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
            .build();

    Method method =
        splunkDelegateService.getClass().getDeclaredMethod("initSplunkServiceWithBasicAuth", SplunkConfig.class);
    method.setAccessible(true);
    Object r = method.invoke(splunkDelegateService, config);
    assertTrue(((Service) r).getToken().startsWith("Basic"));
  }

  @Test
  @Category(IntegrationTests.class)
  public void splunkLogQueryCustomHostFieldTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String expectedQuery =
        "search testQuery myHostNameField = harness.test.host.name | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t| table _time, _raw,cluster_label, myHostNameField | stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,myHostNameField";
    Method method = splunkDelegateService.getClass().getDeclaredMethod(
        "getQuery", String.class, String.class, String.class, boolean.class);
    method.setAccessible(true);

    Object r = method.invoke(splunkDelegateService, "testQuery", "myHostNameField", "harness.test.host.name", false);
    String formedQuery = (String) r;
    assertEquals(expectedQuery, formedQuery);
  }

  @Test
  @Category(IntegrationTests.class)
  public void splunkLogQuerAdvancedQueryTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String advancedQuery = "my advanced test query";
    String expectedQuery = advancedQuery
        + " myHostNameField = harness.test.host.name | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t| table _time, _raw,cluster_label, myHostNameField | stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,myHostNameField";
    Method method = splunkDelegateService.getClass().getDeclaredMethod(
        "getQuery", String.class, String.class, String.class, boolean.class);
    method.setAccessible(true);

    Object r = method.invoke(splunkDelegateService, advancedQuery, "myHostNameField", "harness.test.host.name", true);
    String formedQuery = (String) r;
    assertEquals(expectedQuery, formedQuery);
  }
}

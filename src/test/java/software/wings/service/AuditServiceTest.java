package software.wings.service;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.AuditService;
import software.wings.utils.JsonUtils;

import javax.inject.Inject;

/**
 * Created by rishi on 5/19/16.
 */
public class AuditServiceTest extends WingsBaseTest {
  @Inject private AuditService auditService;

  @Inject private JsonUtils jsonUtils;

  @Test
  public void shouldCreate() throws Exception {}

  @Test
  public void shouldCreate1() throws Exception {}

  @Test
  public void shouldFinalize() throws Exception {}

  @Test
  public void shouldList() throws Exception {}

  @Test
  public void shouldUpdateUser() throws Exception {}
}

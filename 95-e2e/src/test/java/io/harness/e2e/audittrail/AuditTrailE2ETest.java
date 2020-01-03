package io.harness.e2e.audittrail;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.testframework.restutils.AuditTrailUtils.getAuditTrailInfo;
import static io.harness.testframework.restutils.AuditTrailUtils.getYamlResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.beans.PageResponse;
import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ApplicationRestUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeaderYamlResponse;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuditTrailE2ETest extends AbstractE2ETest {
  private static String APP_NAME;
  private static String APP_NAME_UPDATED;
  private static String applicationId;
  private static String accountId;

  // Entities
  private static Application application;

  @Before
  public void createApplication() {
    APP_NAME = "K8sV1_Automation-" + System.currentTimeMillis();
    Application k8sV1App = anApplication().name(APP_NAME).build();
    application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), k8sV1App);
    applicationId = application.getUuid();
    accountId = application.getAccountId();
    assertThat(application).isNotNull();
  }

  public void updateApplication() {
    APP_NAME_UPDATED = APP_NAME + "update";
    Application k8sV1App =
        anApplication().name(APP_NAME_UPDATED).uuid(applicationId).accountId(accountId).description("").build();
    application = ApplicationRestUtils.updateApplication(bearerToken, k8sV1App, applicationId, accountId);
    assertThat(application).isNotNull();
  }
  /* While Querying we will get info for all the audit trail records, we have to find the record which is for CRUD we
   * performed*/
  public AuditHeader getRequiredHeader(
      List<AuditHeader> auditHeaderList, String operationType, String appName, String entityType) {
    AuditHeader reqAuditHeader = null;
    for (AuditHeader auditData : auditHeaderList) {
      if (isHeaderForThisOperation(auditData, operationType, appName, entityType)) {
        reqAuditHeader = auditData;
        break;
      }
    }
    return reqAuditHeader;
  }

  /* Returns true if the audit data is for the required CRUD operation*/
  public boolean isHeaderForThisOperation(
      AuditHeader auditData, String operationType, String appName, String entityType) {
    List<EntityAuditRecord> entityRecords = auditData.getEntityAuditRecords();
    for (EntityAuditRecord auditChangeData : entityRecords) {
      if ((auditChangeData.getAppName().equals(appName)) && (auditChangeData.getOperationType().equals(operationType))
          && (auditChangeData.getEntityType().equals(entityType))) {
        return true;
      }
    }
    return false;
  }

  /* Test to check whether a audit entry is created when a new application is added*/
  @Test
  @Owner(developers = DEEPAK)
  @Category(E2ETests.class)
  public void TC1_applicationCreationEntryExistTest() {
    /* We are querying for audit entry in time interval [-1min, 5 min]*/
    String filterString = (new JSONObject()
                               .put("preferenceType", "AUDIT_PREFERENCE")
                               .put("offset", 0)
                               .put("lastNDays", -1)
                               .put("startTime", System.currentTimeMillis() - 60000)
                               .put("endTime", System.currentTimeMillis() + 300000)
                               .put("includeAccountLevelResources", true)
                               .put("includeAppLevelResources", true))
                              .toString();
    RestResponse<PageResponse<AuditHeader>> auditEntryRestResponse =
        getAuditTrailInfo(bearerToken, getAccount().getUuid(), filterString);

    List<AuditHeader> auditHeaders = auditEntryRestResponse.getResource();
    for (int i = 0; i < auditHeaders.size(); i++) {
      if (isHeaderForThisOperation(auditHeaders.get(i), "CREATE", APP_NAME, "APPLICATION")) {
        return;
      }
    }
    Assert.fail("ERROR: Didn't Found an create audit trail entry for the application " + APP_NAME + "'");
  }

  /* Test to check that the yaml diff when we update the application*/
  @Test
  @Owner(developers = DEEPAK)
  @Category(E2ETests.class)
  public void TC2_correctYamlForApplicationUpdateTest() {
    String filterString = (new JSONObject()
                               .put("preferenceType", "AUDIT_PREFERENCE")
                               .put("offset", 0)
                               .put("lastNDays", -1)
                               .put("startTime", System.currentTimeMillis() - 60000)
                               .put("endTime", System.currentTimeMillis() + 300000)
                               .put("includeAccountLevelResources", true)
                               .put("includeAppLevelResources", true))
                              .toString();
    updateApplication();
    RestResponse<PageResponse<AuditHeader>> auditEntryRestResponse =
        getAuditTrailInfo(bearerToken, getAccount().getUuid(), filterString);
    List<AuditHeader> auditHeaders = auditEntryRestResponse.getResource();
    AuditHeader updateAuditHeader = getRequiredHeader(auditHeaders, "UPDATE", APP_NAME_UPDATED, "APPLICATION");
    if (updateAuditHeader == null) {
      Assert.fail("ERROR: Didn't Found an update audit trail entry for the application " + APP_NAME_UPDATED + "'");
      return;
    }
    /* We are assuming that only one yaml file will be changed in case of Application update*/
    String auditHeaderId = updateAuditHeader.getUuid();
    String entityId = updateAuditHeader.getEntityAuditRecords().get(0).getEntityId();

    RestResponse<AuditHeaderYamlResponse> updateYamlResponse =
        getYamlResponse(bearerToken, auditHeaderId, entityId, accountId);
    String oldYamlPath = updateYamlResponse.getResource().getOldYamlPath();
    String newYamlPath = updateYamlResponse.getResource().getNewYamlPath();
    String oldYaml = updateYamlResponse.getResource().getOldYaml();
    String newYaml = updateYamlResponse.getResource().getNewYaml();

    /* The previous yaml entry must contain the previous name and the new
       one must contain the updated app name */
    assertThat(oldYaml.equals(newYaml)).isTrue();
    assertThat(oldYamlPath.contains(APP_NAME)).isTrue();
    assertThat(newYamlPath.contains(APP_NAME_UPDATED)).isTrue();
  }
}

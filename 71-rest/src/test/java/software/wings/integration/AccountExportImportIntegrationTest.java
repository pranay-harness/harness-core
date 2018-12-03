package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.inject.Inject;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.dl.exportimport.ExportMode;
import software.wings.service.intfc.AccountService;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * @author marklu on 10/25/18
 */
public class AccountExportImportIntegrationTest extends BaseIntegrationTest {
  @Inject private AccountService accountService;

  private String accountId;

  @Before
  public void setUp() {
    super.loginAdminUser();

    Account account = accountService.getByName("Harness");
    assertNotNull(account);

    accountId = account.getUuid();
  }

  @Test
  public void testAccountExportImport() throws Exception {
    byte[] exportedAccountData = exportAccountData(accountId);
    assertNotNull(exportedAccountData);

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(exportedAccountData));

    boolean hasAccounts = false;
    boolean hasApplications = false;
    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      if (zipEntry.getName().equals("accounts.json")) {
        hasAccounts = true;
      } else if (zipEntry.getName().equals("applications.json")) {
        hasApplications = true;
      }
    }
    assertTrue(hasAccounts);
    assertTrue(hasApplications);

    importAccountData(accountId, exportedAccountData);
  }

  @Test
  public void testSepcificExport() throws Exception {
    byte[] exportedAccountData =
        exportSpecificAccountData(accountId, Application.class.getAnnotation(Entity.class).value());
    assertNotNull(exportedAccountData);

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(exportedAccountData));

    boolean hasUsers = false;
    boolean hasApplications = false;
    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      if (zipEntry.getName().equals("users.json")) {
        hasUsers = true;
      } else if (zipEntry.getName().equals("applications.json")) {
        hasApplications = true;
      }
    }
    assertFalse(hasUsers);
    assertTrue(hasApplications);
  }

  @Test
  public void testSepcificExport_noEntityTypes_shouldFail() {
    try {
      WebTarget target =
          client.target(API_BASE + "/account/export?accountId=" + accountId + "&mode=" + ExportMode.SPECIFIC);
      getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<String>>() {});
      fail("Should not reach here, exception is expected");
    } catch (Exception e) {
      // Exception is expected
    }
  }

  private byte[] exportAccountData(String accountId) {
    WebTarget target = client.target(API_BASE + "/account/export?accountId=" + accountId);
    byte[] responseZip = getRequestBuilderWithAuthHeader(target).get(new GenericType<byte[]>() {});
    assertTrue(isNotEmpty(responseZip));

    return responseZip;
  }

  private byte[] exportSpecificAccountData(String accountId, String entityType) {
    WebTarget target = client.target(API_BASE + "/account/export?accountId=" + accountId
        + "&mode=" + ExportMode.SPECIFIC + "&entityTypes=" + entityType);
    byte[] responseZip = getRequestBuilderWithAuthHeader(target).get(new GenericType<byte[]>() {});
    assertTrue(isNotEmpty(responseZip));

    return responseZip;
  }

  private void importAccountData(String accountId, byte[] accountDataJson) {
    MultiPart multiPart = new MultiPart();
    FormDataBodyPart formDataBodyPart =
        new FormDataBodyPart("file", accountDataJson, MediaType.MULTIPART_FORM_DATA_TYPE);
    multiPart.bodyPart(formDataBodyPart);

    WebTarget target = client.target(API_BASE + "/account/import?accountId=" + accountId);
    RestResponse<Void> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE), new GenericType<RestResponse<Void>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
  }
}

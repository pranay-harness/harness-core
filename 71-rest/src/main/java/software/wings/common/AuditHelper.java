package software.wings.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.k8s.model.AuditGlobalContextData;
import io.harness.manage.GlobalContextManager;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.service.intfc.AuditService;

import java.io.InputStream;

/**
 * AuditHelper uses threadlocal to stitch both request and response pay-load with the common http
 * header entries.
 *
 * @author Rishi
 */
@Singleton
@Slf4j
public class AuditHelper {
  private static final ThreadLocal<AuditHeader> auditThreadLocal = new ThreadLocal<>();

  @Inject private AuditService auditService;

  /**
   * Gets the.
   *
   * @return the audit header
   */
  public AuditHeader get() {
    return auditThreadLocal.get();
  }

  /**
   * Creates a new Audit log entry in database.
   *
   * @param header AuditHeader recieved from request.
   * @return AuditHeader after saving.
   */
  public AuditHeader create(AuditHeader header) {
    header = auditService.create(header);
    logger.debug("Saving auditHeader to thread local");
    // TODO: should be removed later as we can rely on GlobalContextManager to get AuditId
    auditThreadLocal.set(header);
    setGlobalContext(header);
    return header;
  }

  private void setGlobalContext(AuditHeader header) {
    GlobalContextManager.upsertGlobalContextRecord(AuditGlobalContextData.builder().auditId(header.getUuid()).build());
  }

  /**
   * Create string.
   *
   * @param header      the header
   * @param requestType the request type
   * @param inputStream the input stream
   * @return the string
   */
  public String create(AuditHeader header, RequestType requestType, InputStream inputStream) {
    return auditService.create(header, requestType, inputStream);
  }

  /**
   * Finalizes autdit log entry by recording response code and time.
   *
   * @param header  AuditHeader entry to update.
   * @param payload response payload.
   */
  public void finalizeAudit(AuditHeader header, byte[] payload) {
    auditService.finalize(header, payload);
    auditThreadLocal.remove();
  }
}

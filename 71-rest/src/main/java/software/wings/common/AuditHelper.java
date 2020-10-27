package software.wings.common;

import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.GlobalContext;
import io.harness.globalcontex.AuditGlobalContextData;
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
   * set audit context with audit header
   * @param header
   */
  public void setAuditContext(AuditHeader header) {
    logger.debug("Saving auditHeader to thread local");
    auditThreadLocal.set(header);
    setGlobalContext(header);
  }

  /**
   * Creates a new Audit log entry in database.
   *
   * @param header AuditHeader received from request.
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
    // TODO: move this in a place where we can remove it after we are done
    initGlobalContextGuard(new GlobalContext());
    upsertGlobalContextRecord(AuditGlobalContextData.builder().auditId(header.getUuid()).build());
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

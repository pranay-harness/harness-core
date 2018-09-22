package software.wings.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AuditHelper {
  private static final ThreadLocal<AuditHeader> auditThreadLocal = new ThreadLocal<>();
  private static final Logger logger = LoggerFactory.getLogger(AuditHelper.class);

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
    auditThreadLocal.set(header);
    return header;
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

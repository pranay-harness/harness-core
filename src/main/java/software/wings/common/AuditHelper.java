package software.wings.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.WingsBootstrap;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.service.intfc.AuditService;

/**
 * AuditHelper uses threadlocal to stitch both request and response pay-load with the common http
 * header entries.
 *
 * @author Rishi
 */
public class AuditHelper {
  private static final ThreadLocal<AuditHeader> auditThreadLocal = new ThreadLocal<AuditHeader>();
  private static AuditHelper instance = new AuditHelper();
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private AuditHelper() {}

  public static AuditHelper getInstance() {
    return instance;
  }

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
    try {
      AuditService auditService = WingsBootstrap.lookup(AuditService.class);
      header = auditService.create(header);
      logger.info("Saving auditHeader to thread local");
      auditThreadLocal.set(header);
      return header;
    } catch (RuntimeException exception) {
      logger.error("Exception occurred while trying to save HttpAuditHeader:=" + exception.getMessage(), exception);
      throw exception;
    }
  }

  /**
   * Creates a audit header for request or response with request/response body.
   *
   * @param header      AuditHeader entry.
   * @param requestType Request or Response.
   * @param httpBody    HttpBody for request or response.
   */
  public void create(AuditHeader header, RequestType requestType, byte[] httpBody) {
    try {
      AuditService auditService = WingsBootstrap.lookup(AuditService.class);
      auditService.create(header, requestType, httpBody);
    } catch (RuntimeException exception) {
      logger.error(
          "Exception occurred while trying to save payload - headerId" + (header != null ? header.getUuid() : null));
      throw exception;
    }
  }

  /**
   * Finalizes autdit log entry by recording response code and time.
   *
   * @param header  AuditHeader entry to update.
   * @param payload response payload.
   */
  public void finalizeAudit(AuditHeader header, byte[] payload) {
    AuditService auditService = WingsBootstrap.lookup(AuditService.class);
    auditService.finalize(header, payload);
    auditThreadLocal.remove();
  }
}

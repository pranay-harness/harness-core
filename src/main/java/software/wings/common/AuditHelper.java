package software.wings.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.app.WingsBootstrap;
import software.wings.beans.AuditHeader;
import software.wings.beans.AuditHeader.RequestType;
import software.wings.service.intfc.AuditService;

/**
 *  AuditHelper uses threadlocal to stitch both request and response pay-load with the common http header entries.
 *
 *
 * @author Rishi
 *
 */
public class AuditHelper {
  private static final ThreadLocal<AuditHeader> auditThreadLocal = new ThreadLocal<AuditHeader>();

  private static AuditHelper instance = new AuditHelper();

  private AuditHelper() {}

  public static AuditHelper getInstance() {
    return instance;
  }

  public AuditHeader get() {
    return auditThreadLocal.get();
  }

  public AuditHeader create(AuditHeader header) {
    try {
      AuditService auditService = WingsBootstrap.lookup(AuditService.class);
      header = auditService.create(header);
      logger.info("Saving auditHeader to thread local");
      auditThreadLocal.set(header);
      return header;
    } catch (RuntimeException rException) {
      logger.error("Exception occurred while trying to save HttpAuditHeader:=" + rException.getMessage(), rException);
      throw rException;
    }
  }

  private static Logger logger = LoggerFactory.getLogger(AuditHelper.class);

  public void finalizeAudit(AuditHeader header, byte[] payload) {
    AuditService auditService = WingsBootstrap.lookup(AuditService.class);
    auditService.finalize(header, payload);
    auditThreadLocal.remove();
  }

  public void create(AuditHeader header, RequestType requestType, byte[] httpBody) {
    try {
      AuditService auditService = WingsBootstrap.lookup(AuditService.class);
      auditService.create(header, requestType, httpBody);
    } catch (RuntimeException rException) {
      logger.error("Exception occurred while trying to save payload - headerId" + header.getUuid());
      throw rException;
    }
  }
}

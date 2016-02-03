package software.wings.service.intfc;

import software.wings.beans.AuditHeader;
import software.wings.beans.AuditPayload;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

/**
 *  HttpAuditService.
 *
 *
 * @author Rishi
 *
 */
public interface AuditService {
  public AuditHeader create(AuditHeader header);

  public AuditPayload create(AuditPayload payload);

  public void finalize(AuditHeader header);

  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req);
}

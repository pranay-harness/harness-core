package software.wings.service.intfc;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.User;

/**
 * HttpAuditService.
 *
 * @author Rishi
 */
public interface AuditService {
  public AuditHeader create(AuditHeader header);

  public void finalize(AuditHeader header, byte[] payload);

  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req);

  public String create(AuditHeader header, RequestType requestType, byte[] httpBody);

  public void updateUser(AuditHeader header, User user);
}

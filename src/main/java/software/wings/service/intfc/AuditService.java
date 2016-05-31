package software.wings.service.intfc;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * HttpAuditService.
 *
 * @author Rishi
 */
public interface AuditService {
  AuditHeader create(AuditHeader header);

  String create(AuditHeader header, RequestType requestType, byte[] httpBody);

  void finalize(AuditHeader header, byte[] payload);

  PageResponse<AuditHeader> list(PageRequest<AuditHeader> req);

  void updateUser(AuditHeader header, User user);

  AuditHeader read(String appId, String auditHeaderId);
}

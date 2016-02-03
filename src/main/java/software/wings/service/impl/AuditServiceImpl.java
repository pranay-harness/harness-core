package software.wings.service.impl;

import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import software.wings.beans.Application;
import software.wings.beans.AuditHeader;
import software.wings.beans.AuditPayload;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.AuditService;

/**
 *  Audit Service Implementation class.
 *
 *
 * @author Rishi
 *
 */
public class AuditServiceImpl implements AuditService {
  private Datastore datastore;

  public AuditServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return MongoHelper.queryPageRequest(datastore, AuditHeader.class, req);
    //		Query q = datastore.createQuery(HttpAuditHeader.class);
    //		q = MongoHelper.applyPageRequest(q, req);
    //
    //		long total = q.countAll();
    //		q.offset(req.getStart());
    //		q.limit(req.getPageSize());
    //		List<HttpAuditHeader> list = q.asList();
    //
    //		PageResponse<HttpAuditHeader> response = new PageResponse<>(req);
    //		response.setTotal(total);
    //		response.setResponse(list);
    //
    //		return response;
  }

  @Override
  public AuditHeader create(AuditHeader header) {
    Key<AuditHeader> key = datastore.save(header);
    return datastore.get(AuditHeader.class, key.getId());
  }

  @Override
  public AuditPayload create(AuditPayload payload) {
    Key<AuditPayload> key = datastore.save(payload);
    return datastore.get(AuditPayload.class, key.getId());
  }

  @Override
  public void finalize(AuditHeader header) {
    AuditHeader auditHeader = datastore.get(AuditHeader.class, header.getUuid());
    UpdateOperations ops = datastore.createUpdateOperations(AuditHeader.class)
                               .set("responseStatusCode", header.getResponseStatusCode())
                               .set("responseTime", header.getResponseTime());
    datastore.update(auditHeader, ops);
  }
}

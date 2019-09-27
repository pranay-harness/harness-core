package software.wings.search.entities.related.audit;

import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.wings.audit.AuditHeader;
import software.wings.audit.EntityAuditRecord;

import java.util.Map;

@Singleton
public class RelatedAuditViewBuilder {
  public RelatedAuditView getAuditRelatedEntityView(AuditHeader auditHeader, EntityAuditRecord entityAuditRecord) {
    return new RelatedAuditView(auditHeader.getUuid(), auditHeader.getCreatedBy().getName(), auditHeader.getCreatedAt(),
        entityAuditRecord.getAppId(), entityAuditRecord.getAffectedResourceId(), entityAuditRecord.getEntityName(),
        entityAuditRecord.getEntityType());
  }

  public Map<String, Object> getAuditRelatedEntityViewMap(
      AuditHeader auditHeader, EntityAuditRecord entityAuditRecord) {
    ObjectMapper mapper = new ObjectMapper();
    RelatedAuditView relatedAuditView = getAuditRelatedEntityView(auditHeader, entityAuditRecord);
    return mapper.convertValue(relatedAuditView, new TypeReference<Object>() {});
  }
}

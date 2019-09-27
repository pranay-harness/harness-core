package software.wings.search.entities.related.audit;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class RelatedAuditSearchEntity implements SearchEntity<AuditHeader> {
  @Inject private RelatedAuditChangeHandler relatedAuditChangeHandler;

  public static final String TYPE = "audits";
  public static final String VERSION = "0.1";
  public static final Class<AuditHeader> SOURCE_ENTITY_CLASS = AuditHeader.class;
  private static final String CONFIGURATION_PATH = "application/ApplicationSchema.json";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<AuditHeader> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return relatedAuditChangeHandler;
  }

  @Override
  public RelatedAuditView getView(AuditHeader auditHeader) {
    return null;
  }
}

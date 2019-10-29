package software.wings.search.entities.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.search.entities.SearchEntityTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceView.ServiceViewKeys;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.Map;

public class ServiceChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private ServiceChangeHandler serviceChangeHandler;

  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent nonDeleteChangeEvent;
  private ChangeEvent deleteChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private String documentId = generateUuid();

  @Before
  public void setup() throws IOException {
    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.SERVICE.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.SERVICE.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, deleteAuditHeader,
        ChangeType.UPDATE, EntityType.SERVICE.name(), Type.DELETE.name(), documentId);
    assertThat(deleteChangeEvent).isNotNull();

    nonDeleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, nonDeleteAuditHeader,
        ChangeType.UPDATE, EntityType.SERVICE.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteChangeEvent).isNotNull();

    nonDeleteEntityAuditRecord =
        SearchEntityTestUtils.createEntityAuditRecord(EntityType.SERVICE.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = serviceChangeHandler.handleChange(deleteChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(ServiceSearchEntity.TYPE, ServiceViewKeys.auditTimestamps, documentId,
             nonDeleteAuditHeader.getCreatedAt(), 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             ServiceSearchEntity.TYPE, ServiceViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = serviceChangeHandler.handleChange(nonDeleteChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }
}

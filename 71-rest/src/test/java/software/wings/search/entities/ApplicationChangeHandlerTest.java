package software.wings.search.entities;

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
import software.wings.search.entities.application.ApplicationChangeHandler;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.application.ApplicationView.ApplicationViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.Map;

public class ApplicationChangeHandlerTest extends WingsBaseTest {
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Mock private SearchDao searchDao;

  @Inject @InjectMocks private ApplicationChangeHandler applicationChangeHandler;

  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent deleteAuditHeaderChangeEvent;
  private ChangeEvent nonDeleteAuditHeaderChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private String documentId = generateUuid();

  @Before
  public void setup() throws IOException {
    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.APPLICATION.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.APPLICATION.name(), documentId, Type.CREATE.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteAuditHeaderChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class,
        deleteAuditHeader, ChangeType.UPDATE, EntityType.APPLICATION.name(), Type.DELETE.name(), documentId);
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class,
        nonDeleteAuditHeader, ChangeType.UPDATE, EntityType.APPLICATION.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteEntityAuditRecord = SearchEntityTestUtils.createEntityAuditRecord(
        EntityType.APPLICATION.name(), documentId, ChangeType.UPDATE.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = applicationChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(ApplicationSearchEntity.TYPE, ApplicationViewKeys.auditTimestamps, documentId,
             nonDeleteAuditHeader.getCreatedAt(), 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             ApplicationSearchEntity.TYPE, ApplicationViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = applicationChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }
}

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
import software.wings.search.entities.environment.EnvironmentChangeHandler;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.Map;

public class EnvironmentChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private EnvironmentChangeHandler environmentChangeHandler;

  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent nonDeleteChangeEvent;
  private ChangeEvent deleteChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private String documentId = generateUuid();

  @Before
  public void setup() throws IOException {
    nonDeleteEntityAuditRecord = SearchEntityTestUtils.createEntityAuditRecord(
        EntityType.ENVIRONMENT.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();

    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.ENVIRONMENT.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.ENVIRONMENT.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, deleteAuditHeader,
        ChangeType.UPDATE, EntityType.ENVIRONMENT.name(), Type.DELETE.name(), documentId);
    assertThat(deleteChangeEvent).isNotNull();

    nonDeleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, nonDeleteAuditHeader,
        ChangeType.UPDATE, EntityType.ENVIRONMENT.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteChangeEvent).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = environmentChangeHandler.handleChange(deleteChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.auditTimestamps, documentId, 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = environmentChangeHandler.handleChange(nonDeleteChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }
}

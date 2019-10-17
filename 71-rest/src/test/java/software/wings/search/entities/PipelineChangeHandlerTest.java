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
import software.wings.search.entities.pipeline.PipelineChangeHandler;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.Map;

public class PipelineChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private PipelineChangeHandler pipelineChangeHandler;

  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent nonDeleteChangeEvent;
  private ChangeEvent deleteChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private String documentId = generateUuid();

  @Before
  public void setup() throws IOException {
    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.PIPELINE.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.PIPELINE.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, deleteAuditHeader,
        ChangeType.UPDATE, EntityType.PIPELINE.name(), Type.DELETE.name(), documentId);
    assertThat(deleteChangeEvent).isNotNull();

    nonDeleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, nonDeleteAuditHeader,
        ChangeType.UPDATE, EntityType.PIPELINE.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteChangeEvent).isNotNull();

    nonDeleteEntityAuditRecord =
        SearchEntityTestUtils.createEntityAuditRecord(EntityType.PIPELINE.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = pipelineChangeHandler.handleChange(deleteChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(PipelineSearchEntity.TYPE, PipelineViewKeys.auditTimestamps, documentId,
             nonDeleteAuditHeader.getCreatedAt(), 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             PipelineSearchEntity.TYPE, PipelineViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = pipelineChangeHandler.handleChange(nonDeleteChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }
}

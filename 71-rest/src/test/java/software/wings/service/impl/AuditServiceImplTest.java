package software.wings.service.impl;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.WingsBaseTest;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.EntityType;
import software.wings.beans.EntityYamlRecord;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlPayload;

public class AuditServiceImplTest extends WingsBaseTest {
  @Mock private EntityHelper mockEntityHelper;
  @Mock private FeatureFlagService mockFeatureFlagService;
  @Mock private YamlResourceService mockYamlResourceService;
  @Mock private WingsPersistence mockWingsPersistence;

  @Inject @InjectMocks protected AuditServiceImpl auditServiceImpl;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testSaveEntityYamlForAudit() {
    RestResponse mockRestResponse = mock(RestResponse.class);
    doReturn(mockRestResponse).when(mockYamlResourceService).obtainEntityYamlVersion(anyString(), any());
    YamlPayload mockResource = mock(YamlPayload.class);
    doReturn(mockResource).when(mockRestResponse).getResource();
    final String YAML_CONTENT = "YamlContent";
    doReturn(YAML_CONTENT).when(mockResource).getYaml();
    doReturn("YamlPath").when(mockEntityHelper).getFullYamlPathForEntity(any(), any());
    auditServiceImpl.saveEntityYamlForAudit(anApplication().build(),
        EntityAuditRecord.builder().appId(APP_ID).entityId(APP_ID).entityType(EntityType.APPLICATION.name()).build(),
        ACCOUNT_ID);
    ArgumentCaptor<EntityYamlRecord> captor = ArgumentCaptor.forClass(EntityYamlRecord.class);
    verify(mockWingsPersistence).save(captor.capture());
    EntityYamlRecord record = captor.getValue();
    assertThat(record).isNotNull();
    assertThat(record.getEntityType()).isEqualTo(EntityType.APPLICATION.name());
    assertThat(record.getYamlContent()).isEqualTo(YAML_CONTENT);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetLatestYamlRecordIdForEntity() {
    String yamlPath = "YAML_PATH";
    String id = "ID";
    Query mockQuery = mock(Query.class);
    doReturn(mockQuery).when(mockWingsPersistence).createQuery(any());
    doReturn(mockQuery).when(mockQuery).filter(anyString(), any());
    doReturn(mockQuery).when(mockQuery).project(anyString(), anyBoolean());
    doReturn(mockQuery).when(mockQuery).order(any(Sort.class));
    EntityYamlRecord entityYamlRecord = EntityYamlRecord.builder().yamlPath(yamlPath).uuid(id).build();
    doReturn(entityYamlRecord).when(mockQuery).get();
    EntityAuditRecord entityAuditRecord = EntityAuditRecord.builder().build();
    auditServiceImpl.loadLatestYamlDetailsForEntity(entityAuditRecord, ACCOUNT_ID);
    assertThat(entityAuditRecord.getEntityOldYamlRecordId()).isEqualTo(id);
    assertThat(entityAuditRecord.getYamlPath()).isEqualTo(yamlPath);
  }
}
package software.wings.service.impl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.dl.WingsPersistence;

public class EntityHelperTest extends WingsBaseTest {
  @Mock private WingsPersistence mockWingsPersistence;
  @Inject @InjectMocks private EntityHelper entityHelper;

  @Test
  @Category(UnitTests.class)
  public void testLoad() {
    Environment environment = anEnvironment().withName(ENV_NAME).withAppId(APP_ID).build();
    EntityAuditRecordBuilder builder = EntityAuditRecord.builder();
    Query mockQuery = mock(Query.class);
    doReturn(mockQuery).when(mockWingsPersistence).createQuery(any());
    doReturn(mockQuery).when(mockQuery).filter(anyString(), anyString());
    doReturn(mockQuery).when(mockQuery).project(anyString(), anyBoolean());
    doReturn(singletonList(anApplication().withName(APP_NAME).build())).when(mockQuery).asList();
    entityHelper.loadMetaDataForEntity(environment, builder, Type.CREATE);
    EntityAuditRecord record = builder.build();
    assertThat(record).isNotNull();
    assertThat(record.getEntityName()).isEqualTo(ENV_NAME);
    assertThat(record.getAppId()).isEqualTo(APP_ID);
  }
}
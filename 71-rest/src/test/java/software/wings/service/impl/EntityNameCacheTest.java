package software.wings.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.persistence.NameAccess;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;

public class EntityNameCacheTest extends WingsBaseTest {
  @Mock private WingsPersistence mockWingsPersistence;
  @Inject @InjectMocks private EntityNameCache entityNameCache;

  @Test
  @Category(UnitTests.class)
  public void testEntityCache() throws Exception {
    Query mockQuery = mock(Query.class);
    doReturn(mockQuery).when(mockWingsPersistence).createQuery(any());
    doReturn(mockQuery).when(mockQuery).filter(anyString(), anyString());
    doReturn(mockQuery).when(mockQuery).project(anyString(), anyBoolean());

    NameAccess nameAccessApp = new NameAccess() {
      @Override
      public String getName() {
        return "App1";
      }
    };

    NameAccess nameAccessService = new NameAccess() {
      @Override
      public String getName() {
        return "Service1";
      }
    };

    NameAccess nameAccessTrigger = new NameAccess() {
      @Override
      public String getName() {
        return "Trigger1";
      }
    };

    doReturn(nameAccessApp).doReturn(nameAccessService).doReturn(nameAccessTrigger).when(mockQuery).get();

    String name = entityNameCache.getEntityName(EntityType.APPLICATION, "12345");
    assertEquals("App1", name);
    verify(mockWingsPersistence, times(1)).createQuery(any());

    name = entityNameCache.getEntityName(EntityType.APPLICATION, "12345");
    assertEquals("App1", name);
    verify(mockWingsPersistence, times(1)).createQuery(any());

    name = entityNameCache.getEntityName(EntityType.SERVICE, "12345");
    assertEquals("Service1", name);
    verify(mockWingsPersistence, times(2)).createQuery(any());

    name = entityNameCache.getEntityName(EntityType.SERVICE, "12345");
    assertEquals("Service1", name);
    verify(mockWingsPersistence, times(2)).createQuery(any());

    name = entityNameCache.getEntityName(EntityType.TRIGGER, "12345");
    assertEquals("Trigger1", name);
    verify(mockWingsPersistence, times(3)).createQuery(any());

    name = entityNameCache.getEntityName(EntityType.TRIGGER, "12345");
    assertEquals("Trigger1", name);
    verify(mockWingsPersistence, times(3)).createQuery(any());

    try {
      entityNameCache.getEntityName(EntityType.APPLICATION, null);
      assertTrue(false);
    } catch (Exception e) {
      assertTrue(true);
    }

    try {
      entityNameCache.getEntityName(null, "12345");
      assertTrue(false);
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}

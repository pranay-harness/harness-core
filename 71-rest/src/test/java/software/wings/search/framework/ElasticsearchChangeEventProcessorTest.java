package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElasticsearchChangeEventProcessorTest extends WingsBaseTest {
  @Spy private Set<SearchEntity<?>> searchEntities = new HashSet<>();
  @Inject @InjectMocks private ElasticsearchChangeEventProcessor elasticsearchChangeEventProcessor;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testProcessChange() {
    SearchEntity searchEntity = mock(SearchEntity.class);
    ChangeHandler changeHandler = mock(ChangeHandler.class);
    Class<? extends PersistentEntity> sourceClass = PersistentEntity.class;
    String token = "__TOKEN__";
    String uuid = "__UUID__";
    ChangeEvent changeEvent = new ChangeEvent<>(token, ChangeType.DELETE, sourceClass, uuid, null, null);
    List<Class<? extends PersistentEntity>> subscriptionEntities = new ArrayList<>();
    subscriptionEntities.add(sourceClass);
    searchEntities.add(searchEntity);

    when(searchEntity.getSubscriptionEntities()).thenReturn(subscriptionEntities);
    when(searchEntity.getChangeHandler()).thenReturn(changeHandler);
    when(changeHandler.handleChange(changeEvent)).thenReturn(true);

    boolean result = elasticsearchChangeEventProcessor.processChange(changeEvent);
    assertThat(result).isTrue();

    verify(searchEntity, times(1)).getChangeHandler();
    verify(changeHandler, times(1)).handleChange(changeEvent);

    when(changeHandler.handleChange(changeEvent)).thenThrow(new RuntimeException("Dummy error"));
    result = elasticsearchChangeEventProcessor.processChange(changeEvent);
    assertThat(result).isFalse();

    elasticsearchChangeEventProcessor.shutdown();
  }
}

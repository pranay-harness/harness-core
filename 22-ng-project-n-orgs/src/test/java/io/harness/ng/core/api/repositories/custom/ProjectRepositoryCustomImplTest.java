package io.harness.ng.core.api.repositories.custom;

import static io.harness.rule.OwnerRule.KARAN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class ProjectRepositoryCustomImplTest {
  private MongoTemplate mongoTemplate;
  private ProjectRepositoryCustomImpl projectRepository;

  @Before
  public void setup() {
    mongoTemplate = mock(MongoTemplate.class);
    projectRepository = new ProjectRepositoryCustomImpl(mongoTemplate);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testFindAll() {
    Project project = Project.builder()
                          .accountIdentifier(randomAlphabetic(10))
                          .orgIdentifier(randomAlphabetic(10))
                          .identifier(randomAlphabetic(10))
                          .name(randomAlphabetic(10))
                          .color(randomAlphabetic(10))
                          .build();
    Pageable pageable = Pageable.unpaged();

    when(mongoTemplate.find(any(Query.class), eq(Project.class))).thenReturn(singletonList(project));
    when(mongoTemplate.count(any(Query.class), eq(Project.class))).thenReturn(1L);

    Page<Project> projects = projectRepository.findAll(new Criteria(), pageable);

    assertEquals(pageable, projects.getPageable());
    assertEquals(1, projects.getContent().size());
    assertEquals(project, projects.getContent().get(0));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDelete() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    Long version = 0L;
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);
    ArgumentCaptor<Query> queryArgumentCaptor = ArgumentCaptor.forClass(Query.class);

    when(mongoTemplate.findAndModify(any(), any(), eq(Project.class))).thenReturn(null);

    Boolean deleted = projectRepository.delete(accountIdentifier, orgIdentifier, identifier, version);

    verify(mongoTemplate, times(1))
        .findAndModify(queryArgumentCaptor.capture(), updateArgumentCaptor.capture(), eq(Project.class));
    Query query = queryArgumentCaptor.getValue();
    Update update = updateArgumentCaptor.getValue();
    assertFalse(deleted);
    assertEquals(1, update.getUpdateObject().size());
    assertEquals(5, query.getQueryObject().size());
    assertTrue(query.getQueryObject().containsKey(ProjectKeys.accountIdentifier));
    assertEquals(accountIdentifier, query.getQueryObject().get(ProjectKeys.accountIdentifier));
    assertTrue(query.getQueryObject().containsKey(ProjectKeys.orgIdentifier));
    assertEquals(orgIdentifier, query.getQueryObject().get(ProjectKeys.orgIdentifier));
    assertTrue(query.getQueryObject().containsKey(ProjectKeys.identifier));
    assertEquals(identifier, query.getQueryObject().get(ProjectKeys.identifier));
    assertTrue(query.getQueryObject().containsKey(ProjectKeys.deleted));
    assertTrue(query.getQueryObject().containsKey(ProjectKeys.version));
    assertEquals(version, query.getQueryObject().get(ProjectKeys.version));
  }
}

package io.harness.ng.core;

import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnsupportedOperationException;
import io.harness.ng.core.entities.Project;
import io.harness.ngpipeline.common.RestQueryFilterParser;
import io.harness.rule.Owner;

import cz.jirutka.rsql.parser.RSQLParserException;
import java.util.ArrayList;
import java.util.Arrays;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

public class RestQueryFilterParserTest extends CategoryTest {
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQuery_For_BlankQuery() {
    String filterQuery = null;
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();

    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
    assertThat(criteria).isNotNull();

    filterQuery = "  ";
    criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
    assertThat(criteria).isNotNull();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQuery_For_ValidQueries() {
    String filterQuery = "modules=in=(CD,CV)";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
    assertThat(criteria).isNotNull();

    Document ownerDocument = (Document) criteria.getCriteriaObject().get("modules");
    assertThat(ownerDocument).isNotNull();
    ArrayList<String> ownerNamesList = (ArrayList) ownerDocument.get("$in");

    assertThat(ownerNamesList).isNotNull();
    assertThat(ownerNamesList.size()).isEqualTo(2);

    assertThat(ownerNamesList.containsAll(Arrays.asList(ModuleType.CD, ModuleType.CV))).isTrue();
  }

  @Test(expected = RSQLParserException.class)
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQuery_For_InValidQueries() {
    String filterQuery = "modules=in===(CD,CV)";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQuery_For_NonQueryable() {
    String filterQuery = "name==AB";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
  }
}
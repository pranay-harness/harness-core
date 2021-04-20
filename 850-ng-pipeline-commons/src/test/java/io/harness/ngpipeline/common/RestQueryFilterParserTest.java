package io.harness.ngpipeline.common;

import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnsupportedOperationException;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;

import cz.jirutka.rsql.parser.RSQLParserException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

public class RestQueryFilterParserTest extends CategoryTest {
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQueryForBlankQuery() {
    String filterQuery = null;
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();

    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
    assertThat(criteria).isNotNull();

    filterQuery = "  ";
    criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
    assertThat(criteria).isNotNull();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQueryForValidQueries() {
    String filterQuery = "name==AB";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
    assertThat(criteria).isNotNull();

    String name = (String) criteria.getCriteriaObject().get("name");
    assertThat(name).isNotNull();
    assertThat(name).isEqualTo("AB");
  }

  @Test(expected = RSQLParserException.class)
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQueryForInValidQueries() {
    String filterQuery = "metadata===AB";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
  }

  @Test(expected = UnsupportedOperationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQueryForNonQueryable() {
    String filterQuery = "metadata==AB";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
  }
}
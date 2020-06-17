package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import cz.jirutka.rsql.parser.RSQLParserException;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.entities.Project;
import io.harness.rule.Owner;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Arrays;

public class RestQueryFilterParserTest extends BaseTest {
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
    String filterQuery = "owners=in=(vikas,nikhil)";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
    assertThat(criteria).isNotNull();

    Document ownerDocument = (Document) criteria.getCriteriaObject().get("owners");
    assertThat(ownerDocument).isNotNull();
    ArrayList<String> ownerNamesList = (ArrayList) ownerDocument.get("$in");

    assertThat(ownerNamesList).isNotNull();
    assertThat(ownerNamesList.size()).isEqualTo(2);

    assertThat(ownerNamesList.containsAll(Arrays.asList("vikas", "nikhil"))).isTrue();
  }

  @Test(expected = RSQLParserException.class)
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetCriteriaFromFilterQuery_For_InValidQueries() {
    String filterQuery = "owners=in===(vikas,nikhil)";
    RestQueryFilterParser restQueryFilterParser = new RestQueryFilterParser();
    restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
  }
}

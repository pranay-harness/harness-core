package software.wings.service.impl.elk;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SRIRAM;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class ElkLogFetchRequestTest extends CategoryTest {
  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleQuery() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest("*exception*");
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"regexp\":{\"message\":{\"value\":\"*exception*\"}}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleOrQuery() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(".*exception.* or error");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"should\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleAndQuery() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(".*exception.* and error");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"must\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleAndOrQuery() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(".*exception.* and error or warn");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"must\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"bool\":{\"should\":[{\"regexp\":{\"message\":{\"value\":\"error\"}}},{\"regexp\":{\"message\":{\"value\":\"warn\"}}}]}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleAndOrBracketedQuery() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest("((.*exception.* and error) or warn)");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}},{\"regexp\":{\"message\":{\"value\":\"warn\"}}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleAndOrBracketedQueryWithTerm() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(
        "((.*exception.* and error) or warn) and source:/home/ubuntu/Harness/Manager/Manager/runtime/portal.log");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}},{\"regexp\":{\"message\":{\"value\":\"warn\"}}}]}},{\"term\":{\"source\":\"/home/ubuntu/harness/manager/manager/runtime/portal.log\"}}]}}]}}}");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void invalidBracket() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(
        "((.*exception.* and error) or warn) and source:/home/ubuntu/Harness/Manager/Manager/runtime/portal.log)");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}}],\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}],\"must\":[{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}},{\"regexp\":{\"message\":{\"value\":\"warn\"}}}]}},{\"term\":{\"source\":\"/home/ubuntu/Harness/Manager/Manager/runtime/portal.log\"}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void simpleQueryUpperCase() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(".*exception.* OR .*error.*");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"should\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\".*error.*\"}}}]}}]}}}");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void simpleQueryCaseInsensitive() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(".*Exception.* OR .*ERROR.*");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"should\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\".*error.*\"}}}]}}]}}}");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void multimessageterms() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(
        "((.*exception.* or error) or warn) ors source:/home/ubuntu/Harness/Manager/Manager/runtime/portal.log");
    JSONObject jsonObject = elkLogFetchRequest.eval();
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"must\":[{\"term\":{\"source\":\"/home/ubuntu/harness/manager/manager/runtime/portal.log\"}},{\"regexp\":{\"message\":{\"value\":\"ors\"}}},{\"bool\":{\"should\":[{\"bool\":{\"should\":[{\"regexp\":{\"message\":{\"value\":\".*exception.*\"}}},{\"regexp\":{\"message\":{\"value\":\"error\"}}}]}},{\"regexp\":{\"message\":{\"value\":\"warn\"}}}]}}]}}]}}}");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void extraOpenbraces() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest(
        "(((.*exception.* or error) or warn) ors source:/home/ubuntu/Harness/Manager/Manager/runtime/portal.log");
    JSONObject jsonObject = elkLogFetchRequest.eval();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void nonMessage() {
    ElkLogFetchRequest elkLogFetchRequest = getElkLogFetchRequest("level:Error OR level:Warn");
    assertThat(JsonUtils.asJson(elkLogFetchRequest.toElasticSearchJsonObject()))
        .isEqualTo(
            "{\"size\":10000,\"query\":{\"bool\":{\"filter\":[{\"bool\":{\"should\":[{\"term\":{\"beat.hostname\":\"ip-172-31-13-153\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-12-79\"}},{\"term\":{\"beat.hostname\":\"ip-172-31-8-144\"}}]}},{\"range\":{\"@timestamp\":{\"lt\":1518724315175,\"format\":\"epoch_millis\",\"gte\":1518724255175}}},{\"bool\":{\"should\":[{\"term\":{\"level\":\"error\"}},{\"term\":{\"level\":\"warn\"}}]}}]}}}");
  }

  private ElkLogFetchRequest getElkLogFetchRequest(String query) {
    return ElkLogFetchRequest.builder()
        .query(query)
        .indices("logstash-*")
        .hostnameField("beat.hostname")
        .messageField("message")
        .timestampField("@timestamp")
        .hosts(Sets.newHashSet("ip-172-31-8-144", "ip-172-31-12-79", "ip-172-31-13-153"))
        .startTime(1518724315175L - TimeUnit.MINUTES.toMillis(1))
        .endTime(1518724315175L)
        .queryType(ElkQueryType.TERM)
        .build();
  }
}

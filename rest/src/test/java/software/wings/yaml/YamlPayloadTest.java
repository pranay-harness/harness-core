package software.wings.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;

public class YamlPayloadTest {
  private String validYaml = "---\nanimal1: fish\nanimal2: monkey";
  private String invalidYaml = "---\ntest: \"fish \"monkey\"";

  @Test
  public void testValid() {
    YamlPayload yp = new YamlPayload(validYaml);

    assertThat(yp.getYaml()).isEqualTo(validYaml);
    assertThat(yp.getResponseMessages().size()).isEqualTo(0);
  }

  @Test
  public void testInvalid() {
    YamlPayload yp = new YamlPayload(invalidYaml);

    assertThat(yp.getYaml()).isEmpty();
    assertThat(yp.getResponseMessages().get(0).getErrorType()).isEqualTo(ResponseTypeEnum.ERROR);
    assertThat(yp.getResponseMessages().get(0).getCode()).isEqualTo(ErrorCode.INVALID_YAML_PAYLOAD);
  }
}

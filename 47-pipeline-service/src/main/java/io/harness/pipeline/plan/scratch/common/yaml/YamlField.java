package io.harness.pipeline.plan.scratch.common.yaml;

import com.google.protobuf.ByteString;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.plan.YamlFieldBlob;
import lombok.Value;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.validation.constraints.NotNull;

@Value
public class YamlField {
  private static final Charset CHARSET = Charset.forName(StandardCharsets.UTF_8.name());

  String name;
  @NotNull YamlNode node;

  public YamlField(String name, YamlNode node) {
    this.name = name;
    this.node = node;
  }

  public YamlField(YamlNode node) {
    this(null, node);
  }

  public YamlFieldBlob toFieldBlob() {
    YamlFieldBlob.Builder builder = YamlFieldBlob.newBuilder().setBlob(ByteString.copyFrom(node.toString(), CHARSET));
    if (name != null) {
      builder.setName(name);
    }
    return builder.build();
  }

  public static YamlField fromFieldFieldBlob(YamlFieldBlob fieldBlob) throws IOException {
    YamlField field = YamlUtils.readTree(fieldBlob.getBlob().toString(CHARSET));
    return new YamlField(EmptyPredicate.isEmpty(fieldBlob.getName()) ? null : fieldBlob.getName(), field.getNode());
  }
}

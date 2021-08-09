package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

import com.amazonaws.services.ec2.model.Tag;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
public class AwsFunctor implements ExpressionFunctor {
  public static class TagsFunctor {
    public String find(Collection<Tag> tags, String key) {
      return tags.stream()
          .filter(tag -> tag.getKey().equals(key))
          .map(Tag::getValue)
          .findFirst()
          .orElse(StringUtils.EMPTY);
    }
  }

  public final TagsFunctor tags = new TagsFunctor();
}

package software.wings.expression;

import com.amazonaws.services.ec2.model.Tag;
import io.harness.expression.ExpressionFunctor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

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

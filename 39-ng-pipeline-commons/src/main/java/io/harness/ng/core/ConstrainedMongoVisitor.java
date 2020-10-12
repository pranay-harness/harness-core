package io.harness.ng.core;

import com.github.rutledgepaulv.qbuilders.nodes.ComparisonNode;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import io.harness.exception.UnsupportedOperationException;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Set;

public class ConstrainedMongoVisitor extends MongoVisitor {
  private final Set<String> annotatedFields;

  public ConstrainedMongoVisitor(Set<String> annotatedFields) {
    this.annotatedFields = annotatedFields;
  }

  @Override
  public Criteria visit(ComparisonNode node) {
    String field = node.getField().asKey();
    if (!annotatedFields.contains(field)) {
      throw new UnsupportedOperationException(String.format("Filtering on field %s is not supported", field));
    }
    return super.visit(node);
  }
}

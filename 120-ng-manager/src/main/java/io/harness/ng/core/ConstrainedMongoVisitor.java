package io.harness.ng.core;

import static io.harness.exception.WingsException.USER;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.github.rutledgepaulv.qbuilders.nodes.ComparisonNode;
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import io.harness.exception.UnauthorizedException;
import org.springframework.data.mongodb.core.query.Criteria;

import java.sql.Date;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ConstrainedMongoVisitor extends MongoVisitor {
  private final UnaryOperator<Object> constrainedNormalizer;
  private final Set<String> annotatedFields;

  public ConstrainedMongoVisitor(Set<String> annotatedFields) {
    this.constrainedNormalizer = new DefaultNormalizer();
    this.annotatedFields = annotatedFields;
  }

  @Override
  public Criteria visit(ComparisonNode node) {
    ComparisonOperator operator = node.getOperator();

    Collection<?> values = node.getValues().stream().map(constrainedNormalizer).collect(Collectors.toList());
    String field = node.getField().asKey();

    if (!annotatedFields.contains(field)) {
      throw new UnauthorizedException(String.format("User not authorized to query on %s", field), USER);
    }

    if (ComparisonOperator.EQ.equals(operator)) {
      return where(field).is(single(values));
    } else if (ComparisonOperator.NE.equals(operator)) {
      return where(field).ne(single(values));
    } else if (ComparisonOperator.EX.equals(operator)) {
      return where(field).exists((Boolean) single(values));
    } else if (ComparisonOperator.GT.equals(operator)) {
      return where(field).gt(single(values));
    } else if (ComparisonOperator.LT.equals(operator)) {
      return where(field).lt(single(values));
    } else if (ComparisonOperator.GTE.equals(operator)) {
      return where(field).gte(single(values));
    } else if (ComparisonOperator.LTE.equals(operator)) {
      return where(field).lte(single(values));
    } else if (ComparisonOperator.IN.equals(operator)) {
      return where(field).in(values);
    } else if (ComparisonOperator.NIN.equals(operator)) {
      return where(field).nin(values);
    } else if (ComparisonOperator.RE.equals(operator)) {
      return where(field).regex((String) single(values));
    } else if (ComparisonOperator.SUB_CONDITION_ANY.equals(operator)) {
      return where(field).elemMatch(condition(node));
    }

    throw new UnsupportedOperationException("This visitor does not support the operator " + operator + ".");
  }

  private static class DefaultNormalizer implements UnaryOperator<Object> {
    @Override
    public Object apply(Object o) {
      if (o instanceof Instant) {
        return Date.from((Instant) o);
      }
      return o;
    }
  }
}

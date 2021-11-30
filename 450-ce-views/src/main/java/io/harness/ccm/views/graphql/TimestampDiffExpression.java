package io.harness.ccm.views.graphql;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import java.io.IOException;
import lombok.Getter;

public class TimestampDiffExpression extends Expression {
  private final SqlObject timestampExpression;
  private final int offset;
  @Getter public final QLCEViewTimeGroupType datePart;

  public TimestampDiffExpression(SqlObject timestampExpression, int offset, QLCEViewTimeGroupType datePart) {
    this.timestampExpression = timestampExpression;
    this.offset = offset;
    this.datePart = datePart;
  }

  @Override
  public boolean hasParens() {
    return false;
  }

  @Override
  protected void collectSchemaObjects(ValidationContext vContext) {
    SqlObject.collectSchemaObjects(timestampExpression, vContext);
  }

  @Override
  public void appendTo(AppendableExt app) throws IOException {
    app.append("TIMESTAMP_SUB(")
        .append(timestampExpression)
        .append(", INTERVAL ")
        .append(offset)
        .append(" ")
        .append(datePart)
        .append(")");
  }
}

package io.harness.ccm.views.graphql;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import lombok.Getter;

import java.io.IOException;

public class TimeTruncatedExpression extends Expression {
  private final SqlObject timestampExpression;
  @Getter public final QLCEViewTimeGroupType datePart;

  public TimeTruncatedExpression(Object timestampExpression, QLCEViewTimeGroupType datePart) {
    this.timestampExpression = Converter.toColumnSqlObject(timestampExpression);
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
    app.append("TIMESTAMP_TRUNC(").append(timestampExpression).append(",").append(datePart).append(")");
  }
}

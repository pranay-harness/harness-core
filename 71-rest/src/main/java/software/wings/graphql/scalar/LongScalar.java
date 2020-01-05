package software.wings.graphql.scalar;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LongScalar {
  public static final GraphQLScalarType type =
      GraphQLScalarType.newScalar()
          .name("Long")
          .description("Long Scalar")
          .coercing(new Coercing<Long, String>() {
            @Override
            public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
              Long data;
              if (dataFetcherResult instanceof Long) {
                return Long.toString((Long) dataFetcherResult);
              }
              throw new CoercingSerializeException("Invalid type, cannot serialize " + dataFetcherResult);
            }

            @Override
            public Long parseValue(Object input) throws CoercingParseValueException {
              try {
                return getValue(input);
              } catch (Exception e) {
                throw new CoercingParseValueException(e);
              }
            }

            @NotNull
            private Long getValue(Object input) {
              if (input instanceof Long) {
                return (Long) input;
              } else {
                return Long.parseLong(input.toString());
              }
            }

            @Override
            public Long parseLiteral(Object input) throws CoercingParseLiteralException {
              try {
                return getValue(input);
              } catch (Exception e) {
                throw new CoercingParseLiteralException(e);
              }
            }
          })
          .build();
}

package software.wings.graphql.scalar;

import static io.harness.time.EpochUtils.PST_ZONE_ID;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created this GraphQLScalar for DateTime scaclar.
 * At present adding support for String/Long/Int conversion to date.
 */
public final class GraphQLScalars {
  public static final GraphQLScalarType DATE_TIME =
      GraphQLScalarType.newScalar()
          .name("DateTime")
          .description("DateTime Scalar")
          .coercing(new Coercing() {
            @Override
            public Object serialize(Object obj) throws CoercingSerializeException {
              Object parsedObject = parseLiteral(obj);

              if (parsedObject instanceof ZonedDateTime) {
                return ((ZonedDateTime) parsedObject).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
              }

              return obj;
            }

            @Override
            public Object parseValue(Object obj) throws CoercingParseValueException {
              return serialize(obj);
            }

            @Override
            public Object parseLiteral(Object obj) throws CoercingParseLiteralException {
              if (obj instanceof StringValue) {
                return ZonedDateTime.parse(((StringValue) obj).getValue());
              } else if (obj instanceof IntValue) {
                return Instant.ofEpochMilli(((IntValue) obj).getValue().longValue()).atZone(ZoneId.of(PST_ZONE_ID));
              } else if (obj instanceof Long) {
                return Instant.ofEpochMilli(((Long) obj).longValue()).atZone(ZoneId.of(PST_ZONE_ID));
              } else {
                return obj;
              }
            }
          })
          .build();
}

package service.db.arango;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

public class ArangoQueryError extends Exception {

  public ArangoQueryError(JsonNode queryResult) {
    this(defaultIfBlank(queryResult.findPath("errorMessage").asText(),
        Json.stringify(queryResult)));
  }

  public ArangoQueryError(Throwable cause) {
    super(cause);
  }

  public ArangoQueryError(String errorMessage) {
    super(errorMessage);
  }
}

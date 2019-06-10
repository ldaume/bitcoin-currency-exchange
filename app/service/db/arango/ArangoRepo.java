package service.db.arango;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static play.libs.Json.newObject;
import static play.libs.Json.stringify;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;

public abstract class ArangoRepo {

  private static final String ERROR = "error";
  private final WSClient ws;
  private final Config config;
  private final String host;

  private RetryListener retryListener = new RetryListener() {
    @Override
    public <V> void onRetry(Attempt<V> attempt) {
      if (attempt.getAttemptNumber() > 1) {
        Logger
            .warn("... attempt {} to call the database. Error: {} ...", attempt.getAttemptNumber(),
                attempt.hasException() ? attempt.getExceptionCause().getMessage() : "unknown");
      }
    }
  };
  private Retryer<JsonNode> jsonNodeRetryer = RetryerBuilder.<JsonNode>newBuilder()
      .retryIfException(throwable -> !StringUtils
          .containsIgnoreCase(throwable.getMessage(), "unique constraint violated"))
      .withWaitStrategy(WaitStrategies.exponentialWait(100, 10, TimeUnit.SECONDS))
      .withStopStrategy(StopStrategies.stopAfterDelay(2, TimeUnit.MINUTES))
      .withRetryListener(retryListener)
      .build();

  public ArangoRepo(WSClient ws, Config config) {
    this.ws = ws;
    this.config = config;
    host = config.getString("arangodb.host");
  }


  protected JsonNode upsert(JsonNode search, JsonNode insert, JsonNode update, String collection)
      throws ArangoQueryError {
    Json.setObjectMapper(Json.mapper().setSerializationInclusion(Include.NON_NULL));
    String insertJson = stringify(insert);
    String updateJson = stringify(update);
    return query("UPSERT " + stringify(search)
        + " INSERT " + insertJson
        + " UPDATE " + updateJson
        + " IN " + collection + " OPTIONS { keepNull: false } "
        + " RETURN { doc: NEW, type: OLD ? 'update' : 'insert' , oldDoc: OLD }", ImmutableMap.of());
  }

  protected JsonNode query(String query) throws ArangoQueryError {
    return query(query, ImmutableMap.of());
  }

  protected JsonNode query(String query, Map<String, Object> bindVars) throws ArangoQueryError {
    return query(query, bindVars, null, null, null, null, null);
  }

  private JsonNode query(String query, Map<String, Object> bindVars, Long batchSize)
      throws ArangoQueryError {
    return query(query, bindVars, null, null, null, null, null);
  }

  private JsonNode query(String query, Map<String, Object> bindVars, Long batchSize, String host,
      String db, String user, String password)
      throws ArangoQueryError {
    try {
      final ObjectNode data = newObject();
      final ObjectNode bindVarJson = newObject();
      Optional.ofNullable(bindVars).orElse(ImmutableMap.of()).forEach(
          (key, value) -> {
            if (value instanceof String) {
              bindVarJson.put(key, (String) value);
            } else {
              bindVarJson.putPOJO(key, value);
            }
          }
      );
      data.set("bindVars", Json.toJson(bindVars));
      data.put("query", query);
      data.put("count", true);
      data.put("batchSize", Optional.ofNullable(batchSize).orElse(Long.MAX_VALUE));
      final String url = defaultIfBlank(host, this.host) + "/_db/" + defaultIfBlank(db,
          config.getString("arangodb.db")) + "/_api/cursor";
      return jsonNodeRetryer.call(()
          -> ws.url(url)
          .setAuth(defaultIfBlank(user, config.getString("arangodb.user")),
              defaultIfBlank(password, config.getString("arangodb.password")))
          .setFollowRedirects(true)
          .post(data)
          .thenApply(wsResponse -> {
            if (wsResponse.getStatus() >= 300) {
              String body = wsResponse.getBody();
              throw new RuntimeException(
                  new ArangoQueryError(wsResponse.getStatusText() + ": " + body));
            }
            JsonNode queryResult = wsResponse.asJson();
            if (queryResult.get(ERROR).asBoolean()) {
              throw new RuntimeException(new ArangoQueryError(queryResult));
            }
            return queryResult;
          }).toCompletableFuture().get());

    } catch (Exception e) {
      Logger.error("Could not query {}.", query, e);
      throw new ArangoQueryError(e);
    }
  }

}

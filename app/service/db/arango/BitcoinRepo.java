package service.db.arango;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import play.libs.Json;
import play.libs.ws.WSClient;
import service.entity.BitcoinRate;

public class BitcoinRepo extends ArangoRepo {

  @Inject
  public BitcoinRepo(WSClient ws, Config config) {
    super(ws, config);
  }

  public BitcoinRate getLastRate() throws ArangoQueryError {
    JsonNode result = query(""
        + "FOR r IN rates"
        + " sort r.timestamp DESC"
        + " LIMIT 1"
        + " RETURN r", ImmutableMap.of()).get("result");
    if (result.size() <= 0) {
      return null;
    }
    return Json.mapper().convertValue(result.get(0), BitcoinRate.class);
  }

  public List<BitcoinRate> getRates(LocalDateTime from, LocalDateTime to) throws ArangoQueryError {
    List<BitcoinRate> bitcoinRates = Lists.newArrayList();
    JsonNode result = query(""
            + "FOR r IN rates"
            + " FILTER r.timestamp >= DATE_TIMESTAMP(@from) && r.timestamp <= DATE_TIMESTAMP(@to)"
            + " sort r.timestamp"
            + " RETURN r",
        ImmutableMap.of("from", from.toString(),
            "to", to.toString()))
        .get("result");
    result.forEach(
        jsonNode -> bitcoinRates.add(Json.mapper().convertValue(jsonNode, BitcoinRate.class)));
    return bitcoinRates;


  }


  public boolean upsertRate(BitcoinRate bitcoinRate) throws ArangoQueryError {
    upsert(Json.newObject().put("timestamp", bitcoinRate.getTimestamp()),
        Json.toJson(bitcoinRate),
        Json.newObject().put("rate", bitcoinRate.getRate()),
        "rates");
    return true;
  }
}

package lifecycle;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import akka.actor.ActorSystem;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.typesafe.config.Config;
import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import lombok.val;
import play.Environment;
import play.Logger;
import play.Logger.ALogger;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import service.db.arango.BitcoinRepo;
import service.entity.BitcoinRate;

public class JobScheduler {


  private final Config config;
  private final WSClient ws;
  private final ALogger logger;
  private final BitcoinRepo bitcoinRepo;

  @Inject
  public JobScheduler(ActorSystem system, Environment environment, Config config,
      WSClient ws, BitcoinRepo bitcoinRepo) {
    this.config = config;
    this.ws = ws;
    this.bitcoinRepo = bitcoinRepo;
    logger = Logger.of(this.getClass());

    logger.info("init");
    boolean staging = config.hasPath("staging") && config.getBoolean("staging");
    logger.info("staging: {}", staging);
    if (environment.isProd() && !staging) {
      system.scheduler()
          .schedule(Duration
                  .create(config.getDuration("crawler.initial.delay", MILLISECONDS), MILLISECONDS),
              Duration.create(config.getDuration("crawler.interval", MILLISECONDS), MILLISECONDS),
              () -> {
                logger.info("Crawling data ...");
                Stopwatch stopWatch = Stopwatch.createStarted();
                try {
                  bitcoinRepo.upsertRate(crawlExchangeRate());

                } catch (Exception e) {
                  logger.error("... could not crawl data.", e);
                }
                logger
                    .info("... crawling took {}s", stopWatch.stop().elapsed(TimeUnit.SECONDS));
              },
              system.dispatcher());
    }
  }

  public BitcoinRate crawlExchangeRate()
      throws InterruptedException, ExecutionException, TimeoutException, RuntimeException {
    WSResponse wsResponse = ws.url("https://blockchain.info/ticker")
        .get()
        .toCompletableFuture()
        .get(config.getDuration("crawler.interval", MILLISECONDS) - 50, MILLISECONDS);

    if (Range.closed(200, 299).contains(wsResponse.getStatus())) {

      val exchangeRate = wsResponse.asJson().get("USD").get("sell").asDouble();

      logger.info("Current exchange rate is 1 BTC = {} USD", exchangeRate);
      return new BitcoinRate(Clock.systemUTC().millis(), exchangeRate);
    }
    throw new RuntimeException(
        "Could not crawl exchange rate. Status was " + wsResponse.getStatus());
  }


}

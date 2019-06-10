package controllers;

import com.typesafe.config.Config;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import service.db.arango.BitcoinRepo;

public class Bitcoin extends GenericController {

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm");
  private final BitcoinRepo bitcoinRepo;
  private final ALogger logger;

  @Inject
  public Bitcoin(HttpExecutionContext ec, Config config, BitcoinRepo bitcoinRepo) {
    super(ec, config);
    this.bitcoinRepo = bitcoinRepo;
    logger = Logger.of(this.getClass());
  }

  public CompletionStage<Result> latest() {
    return result(() -> {
      try {
        return ok(Json.toJson(bitcoinRepo.getLastRate()));
      } catch (Exception e) {
        return internalServerError();
      }
    });
  }

  public CompletionStage<Result> historic(String from, String to) {
    return result(() -> {
      try {
        LocalDateTime fromDate;
        LocalDateTime toDate;
        if (StringUtils.length(from) == 10 && StringUtils.length(to) == 10) {
          fromDate = LocalDate.parse(from, DATE_FORMATTER).atStartOfDay();
          toDate = LocalDate.parse(to, DATE_FORMATTER).atTime(23, 59);
        } else if (StringUtils.length(from) == 16 && StringUtils.length(to) == 16) {
          fromDate = LocalDateTime.parse(from, DATE_TIME_FORMATTER);
          toDate = LocalDateTime.parse(to, DATE_TIME_FORMATTER);
        } else {
          return badRequest(Json.newObject().put("errorMessage",
              "the dates must be in the format 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm'"));
        }
        return ok(Json.toJson(bitcoinRepo.getRates(fromDate, toDate)));
      } catch (DateTimeParseException e) {
        return badRequest(Json.newObject().put("errorMessage",
            "the dates must be in the format 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm'"));
      } catch (Exception e) {
        logger.error("Could not provide bitcoin rates.", e);
        return internalServerError();
      }
    });
  }
}

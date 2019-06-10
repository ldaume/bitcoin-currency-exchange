package service.db.arango;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import javax.inject.Inject;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import service.entity.BitcoinRate;
import utils.AppTestBase;

@Disabled
class BitcoinRepoTest extends AppTestBase {

  @Inject
  BitcoinRepo bitcoinRepo;

  @Test
  void getLastRate() throws Exception {
    val bitcoinRate = bitcoinRepo.getLastRate();
    assertThat(bitcoinRate).isNotNull();
  }

  @Test
  void getRates() throws Exception {
    var rates = bitcoinRepo
        .getRates(LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(2));
    assertThat(rates).isNotEmpty();
  }

  @Test
  void upsertRate() throws Exception {
    BitcoinRate bitcoinRate = new BitcoinRate(Clock.systemUTC().millis(), 666.67);
    assertThat(bitcoinRepo.upsertRate(bitcoinRate)).isTrue();
  }
}
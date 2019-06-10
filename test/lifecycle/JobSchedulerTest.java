package lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import utils.AppTestBase;

class JobSchedulerTest extends AppTestBase {

  @Inject
  JobScheduler scheduler;

  @Test
  void crawlExchangeRate() throws Exception {
    var exchangeRate = scheduler.crawlExchangeRate();
    assertThat(exchangeRate.getRate()).isPositive();
    assertThat(exchangeRate.getTimestamp()).isPositive();
  }
}
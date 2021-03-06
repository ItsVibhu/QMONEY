
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private final String API_KEY = "P2WW1SQXQMQX09H4";
  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException, RuntimeException {
    String url = buildUri(symbol, from, to);
    List<Candle> candles = new ArrayList<>();
    AlphavantageDailyResponse alphavantageDailyResponse = null;
    String response;

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    if (to.isBefore(from))
      throw new RuntimeException();

    try {
      response = this.restTemplate.getForObject(url, String.class);
      alphavantageDailyResponse = objectMapper.readValue(response, AlphavantageDailyResponse.class);

      Map<LocalDate, AlphavantageCandle> responseCandles = alphavantageDailyResponse.getCandles();

      for (Map.Entry<LocalDate, AlphavantageCandle> entry : responseCandles.entrySet()) {
        LocalDate keyDate = entry.getKey();
        if ((keyDate.isEqual(from) || keyDate.isEqual(to)) || (keyDate.isAfter(from) && keyDate.isBefore(to))) {
          AlphavantageCandle candle = entry.getValue();
          candle.setDate(keyDate);
          candles.add(candle);
        }
      }
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException("Alphavantage returned invalid response", e.getCause());
    }

    Collections.sort(candles, this.getComparator());

    return candles;
  }

  private Comparator<Candle> getComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  public String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String urlTemplate = String.format(
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&outputsize=full&apikey=%s",
        symbol, this.API_KEY);

    return urlTemplate;
  }

  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  // 1. Update the method signature to match the signature change in the
  // interface.
  // 2. Start throwing new StockQuoteServiceException when you get some invalid
  // response from
  // Alphavantage, or you encounter a runtime exception during Json parsing.
  // 3. Make sure that the exception propagates all the way from PortfolioManager,
  // so that the
  // external user's of our API are able to explicitly handle this exception
  // upfront.
  // CHECKSTYLE:OFF

}

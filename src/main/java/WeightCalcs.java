import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class WeightCalcs {

  /** Averages aggregations and constructs new main.BodyData. */
  private static BodyData createAvgBodyData(
      LocalDate date,
      double weightSum,
      double weightCount,
      double bodyFatSum,
      double bodyFatCount,
      double muscleMassSum,
      double muscleMassCount) {

    OptionalDouble bodyFat;
    if (bodyFatCount != 0) {
      bodyFat = OptionalDouble.of(bodyFatSum / bodyFatCount);
    } else bodyFat = OptionalDouble.empty();

    OptionalDouble muscleMass;
    if (muscleMassCount != 0) {
      muscleMass = OptionalDouble.of(muscleMassSum / muscleMassCount);
    } else muscleMass = OptionalDouble.empty();

    return new BodyData(date, weightSum / weightCount, bodyFat, muscleMass);
  }

  /** Calculates the rolling average of the data for the requested sliding window. */
  static ArrayList<BodyData> rollingAvg(int window, ArrayList<BodyData> data) {
    ArrayList<BodyData> means = new ArrayList<>();

    int maxIndex = data.size() - 1;
    int currIndex = 0;

    while (currIndex <= maxIndex) {
      int upperBound = currIndex + window;

      double weightSum = 0.0;
      double bodyFatSum = 0.0;
      int bodyFatCount = 0;
      double muscleMassSum = 0.0;
      int muscleMassCount = 0;

      BodyData bodyData;

      if (upperBound > maxIndex) {
        // smaller than window
        for (int i = currIndex; i <= maxIndex; i++) {
          bodyData = data.get(i);
          weightSum += bodyData.getWeight();
          if (bodyData.getBodyFat().isPresent()) {
            bodyFatSum += bodyData.getBodyFat().getAsDouble();
            bodyFatCount++;
          }
          if (bodyData.getMuscleMass().isPresent()) {
            muscleMassSum += bodyData.getMuscleMass().getAsDouble();
            muscleMassCount++;
          }
        }
        means.add(
            createAvgBodyData(
                data.get(currIndex).getDate(),
                weightSum,
                (maxIndex - currIndex + 1),
                bodyFatSum,
                bodyFatCount,
                muscleMassSum,
                muscleMassCount));
      } else {
        for (int i = 0; i < window; i++) {
          bodyData = data.get(currIndex + i);
          weightSum += bodyData.getWeight();
          if (bodyData.getBodyFat().isPresent()) {
            bodyFatSum += bodyData.getBodyFat().getAsDouble();
            bodyFatCount++;
          }
          if (bodyData.getMuscleMass().isPresent()) {
            muscleMassSum += bodyData.getMuscleMass().getAsDouble();
            muscleMassCount++;
          }
        }
        means.add(
            createAvgBodyData(
                data.get(currIndex).getDate(),
                weightSum,
                window,
                bodyFatSum,
                bodyFatCount,
                muscleMassSum,
                muscleMassCount));
      }

      currIndex += 1;
    }

    return means;
  }

  /** Underlying iteration and calculation for bandpass. */
  private static BandPassResult bandpass(
      BandPassCriteria criteria, LocalDate startDate, LocalDate endDate, ArrayList<BodyData> data) {

    Timeframe timeframe = getTimeframe(startDate, endDate, data);

    AtomicInteger count = new AtomicInteger();
    timeframe.data.forEach(
        bodyData -> {
          if (criteria.pass(bodyData)) count.getAndIncrement();
        });

    double result = ((double) count.get()) / timeframe.data.size();

    return new BandPassResult(timeframe.startDate, timeframe.endDate, result);
  }

  /** Interface to encapsulate passing criteria for different body data elements in a bandpass */
  public interface BandPassCriteria {
    boolean pass(BodyData bodyData);
  }

  public static class BandPassResult {
    LocalDate startDate;
    LocalDate endDate;
    double percent;

    public BandPassResult(LocalDate startDate, LocalDate endDate, double percent) {
      this.startDate = startDate;
      this.endDate = endDate;
      this.percent = percent;
    }
  }

  /**
   * Determines the percent of days in a given timeframe that occur withing a requested percent of
   * the target weight.
   *
   * @param bound Percent above or below target, written out of 1 (ie 50% is 0.5)
   */
  static BandPassResult bandpassWeight(
      double weight,
      double bound,
      LocalDate startDate,
      LocalDate endDate,
      ArrayList<BodyData> data) {

    class WeightBandPass implements BandPassCriteria {
      double upperBound = (1 + bound) * weight;
      double lowerBound = (1 - bound) * weight;

      public boolean pass(BodyData bodyData) {
        return bodyData.getWeight() <= upperBound && bodyData.getWeight() >= lowerBound;
      }
    }

    return bandpass(new WeightBandPass(), startDate, endDate, data);
  }

  /**
   * Determines the percent of days in a given timeframe that occur withing a requested percent of
   * the target body fat.
   *
   * <p>Days with no recorded body fat are considered out of range.
   *
   * @param bound Percent above or below target, written out of 1 (ie 50% is 0.5)
   */
  static BandPassResult bandpassBodyFat(
      double bodyFat,
      double bound,
      LocalDate startDate,
      LocalDate endDate,
      ArrayList<BodyData> data) {

    class BodyFatBandPass implements BandPassCriteria {
      double upperBound = (1 + bound) * bodyFat;
      double lowerBound = (1 - bound) * bodyFat;

      public boolean pass(BodyData bodyData) {
        if (bodyData.getBodyFat().isPresent()) {
          return bodyData.getBodyFat().getAsDouble() <= upperBound
              && bodyData.getBodyFat().getAsDouble() >= lowerBound;
        } else return false;
      }
    }

    return bandpass(new BodyFatBandPass(), startDate, endDate, data);
  }

  static TimeFrameReport reportForMonth(int month, int year, ArrayList<BodyData> data) {
    LocalDate monthStart = LocalDate.of(year, month, 1);
    LocalDate monthEnd = LocalDate.of(year, month, monthStart.lengthOfMonth());

    return reportForTimeFrame(monthStart, monthEnd, data);
  }

  static TimeFrameReport reportForTimeFrame(
      LocalDate startDate, LocalDate endDate, ArrayList<BodyData> data) {
    Timeframe timeframe = getTimeframe(startDate, endDate, data);

    AtomicReference<Double> minWeight = new AtomicReference<>(Double.MAX_VALUE);
    AtomicReference<Double> maxWeight = new AtomicReference<>(Double.MIN_VALUE);
    AtomicReference<Double> aggregateWeight = new AtomicReference<>(0.0);

    timeframe.data.forEach(
        bodyData -> {
          double currWeight = bodyData.getWeight();
          if (currWeight < minWeight.get()) minWeight.set(currWeight);
          if (currWeight > maxWeight.get()) maxWeight.set(currWeight);
          aggregateWeight.accumulateAndGet(currWeight, Double::sum);
        });

    return new TimeFrameReport(
        timeframe.startDate,
        timeframe.endDate,
        minWeight.get(),
        maxWeight.get(),
        (aggregateWeight.get() / timeframe.data.size()));
  }

  public static class Timeframe {
    LocalDate startDate;
    LocalDate endDate;
    List<BodyData> data;

    public Timeframe(LocalDate startDate, LocalDate endDate, List<BodyData> data) {
      this.startDate = startDate;
      this.endDate = endDate;
      this.data = data;
    }
  }

  /**
   * Returns a list capturing the timeframe [startDate, endDate] if the start and end dates are
   * contained within the dataset. If not, the earliest or latest date in the set will be used. If
   * the date is not exactly captured by the set, the closest date that does not exceed the target
   * date will be used.
   *
   * <p>Returned list is backed by arraylist.
   *
   * <p>visible for testing
   */
  protected static Timeframe getTimeframe(
      LocalDate startDate, LocalDate endDate, ArrayList<BodyData> data) {
    int earliestDateIndex = data.size() - 1;
    int latestDateIndex = 0;

    int startWindow = 0;
    int endWindow = data.size() - 1;

    LocalDate earliestDate = data.get(earliestDateIndex).getDate();
    LocalDate latestDate = data.get(latestDateIndex).getDate();

    if (startDate.isAfter(latestDate)) {
      return new Timeframe(latestDate, latestDate, new ArrayList<>());
    } else if (endDate.isBefore(earliestDate)) {
      return new Timeframe(earliestDate, earliestDate, new ArrayList<>());
    } else {

      if (!startDate.isBefore(earliestDate)) {
        earliestDateIndex = getIndex(startWindow, endWindow, startDate, false, data);
      }

      if (!endDate.isAfter(latestDate)) {
        latestDateIndex = getIndex(startWindow, endWindow, endDate, true, data);
      }

      return new Timeframe(
          data.get(earliestDateIndex).getDate(),
          data.get(latestDateIndex).getDate(),
          data.subList(latestDateIndex, earliestDateIndex + 1));
    }
  }

  /**
   * Recursive helper. Retrieves the index within the arraylist that represents the requested date
   * Assumes that the target date is captured within the bounds of the dataset.
   *
   * <p>If doNotExceed is true and the target date is not in the data set, this will select the date
   * that does not exceed the target. If false, the date that just exceeds the target will be
   * returned.
   */
  private static int getIndex(
      int startWindow,
      int endWindow,
      LocalDate date,
      boolean doNotExceed,
      ArrayList<BodyData> data) {

    int midpoint = (startWindow + endWindow) / 2;
    LocalDate midpointDate = data.get(midpoint).getDate();

    if (startWindow == endWindow) {
      if (midpointDate.isEqual(date)) return startWindow;
      else if (midpointDate.isBefore(date)) {
        if (doNotExceed && startWindow != 0) return startWindow - 1;
        else return startWindow;
      } else { // midpointDate.isAfter(date)
        if (doNotExceed && startWindow != data.size() - 1) return startWindow + 1;
        else return startWindow;
      }
    } else if (midpointDate.isEqual(date)) return midpoint;
    else if (midpointDate.isBefore(date))
      return getIndex(startWindow, midpoint - 1, date, doNotExceed, data);
    else return getIndex(midpoint + 1, endWindow, date, doNotExceed, data);
  }
}

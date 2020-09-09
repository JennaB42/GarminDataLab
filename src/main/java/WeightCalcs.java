
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

        double result = ((double) count.get()) / timeframe.data.size() * 100;

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

        // jrb: do we want default to be
        AtomicReference<Double> minWeight = new AtomicReference<Double>(Double.MAX_VALUE);
        AtomicReference<Double> maxWeight = new AtomicReference<Double>(Double.MIN_VALUE);
        AtomicReference<Double> aggregateWeight = new AtomicReference<Double>(0.0);

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

    private static class Timeframe {
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
     */
    private static Timeframe getTimeframe(
            LocalDate startDate, LocalDate endDate, ArrayList<BodyData> data) {
        int earliestDate = data.size() - 1;
        int latestDate = 0;

        int midpoint = data.size() / 2;
        int startWindow = 0;
        int endWindow = data.size() - 1;

        if (!startDate.isBefore(data.get(earliestDate).getDate())) {
            earliestDate = getIndex(midpoint, startWindow, endWindow, startDate, data);
        }

        if (!endDate.isAfter(data.get(latestDate).getDate())) {
            latestDate = getIndex(midpoint, startWindow, endWindow, endDate, data);
        }

        return new Timeframe(
                data.get(earliestDate).getDate(),
                data.get(latestDate).getDate(),
                data.subList(latestDate, earliestDate + 1));
    }

    /**
     * Recursive helper. Retrieves the index within the arraylist that represents the requested date
     * without exceeding it. Assumes that the target date is captured within the bounds of the
     * dataset.
     */
    private static int getIndex(
            int currIndex, int startWindow, int endWindow, LocalDate date, ArrayList<BodyData> data) {
        if (startWindow == endWindow) {
            return currIndex;
        } else if (startWindow == currIndex) {
            LocalDate startWindowDate = data.get(startWindow).getDate();
            if (date.isEqual(startWindowDate) || date.isAfter(startWindowDate)) return startWindow;
            else return endWindow;
        } else if (endWindow == currIndex) {
            LocalDate endWindowDate = data.get(endWindow).getDate();
            if (date.isEqual(endWindowDate) || date.isBefore(endWindowDate)) return endWindow;
            else return startWindow;
        } else {
            LocalDate currDate = data.get(currIndex).getDate();
            if (currDate.equals(date)) {
                return currIndex;
            } else if (currDate.isAfter(date)) {
                int nextIndex = (endWindow - currIndex) / 2 + currIndex;
                return getIndex(nextIndex, currIndex, endWindow, date, data);
            } else {
                int nextIndex = currIndex / 2;
                return getIndex(nextIndex, startWindow, currIndex, date, data);
            }
        }
    }
}

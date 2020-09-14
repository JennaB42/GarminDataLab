import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UserReports {

  static void bandpassWeightReport(
      double weight,
      double bound,
      LocalDate startDate,
      LocalDate endDate,
      ArrayList<BodyData> data) {
    WeightCalcs.BandPassResult bandpassResult =
        WeightCalcs.bandpassWeight(weight, bound, startDate, endDate, data);
    System.out.printf(
        "BandpassWeight of %.2f%% around %.2f (%s to %s): %.2f%%%n",
        bound * 100,
        weight,
        bandpassResult.startDate,
        bandpassResult.endDate,
        bandpassResult.percent);
  }

  static void bandpassBodyFatReport(
      double bodyFat,
      double bound,
      LocalDate startDate,
      LocalDate endDate,
      ArrayList<BodyData> data) {
    WeightCalcs.BandPassResult bandpassResult =
        WeightCalcs.bandpassBodyFat(bodyFat, bound, startDate, endDate, data);
    System.out.printf(
        "BandpassBodyFat of %.2f%% around %.1f%% (%s to %s): %.2f%%%n",
        bound * 100,
        bodyFat,
        bandpassResult.startDate,
        bandpassResult.endDate,
        bandpassResult.percent);
  }

  static void monthReport(int month, int year, ArrayList<BodyData> data) {
    TimeFrameReport report = WeightCalcs.reportForMonth(month, year, data);
    System.out.printf("Report for %d-%d: %s%n", month, year, report);
  }

  static void timeframeReport(LocalDate startDate, LocalDate endDate, ArrayList<BodyData> data) {
    TimeFrameReport report = WeightCalcs.reportForTimeFrame(startDate, endDate, data);
    System.out.printf("Report for %s to %s: %s%n", report.startDate, report.endDate, report);
  }

  static void rollingAvgReport(int window, ArrayList<BodyData> data, int displayLimit) {
    // jrb: optimization possible - only roll enough for the report
    ArrayList<BodyData> rolled = WeightCalcs.rollingAvg(window, data);
    String collect =
        rolled.stream()
            .map(BodyData::prettyPrint)
            .limit(displayLimit)
            .collect(Collectors.joining("\n"));
    System.out.printf(
        "\nRolled window of %d viewing first %d samples: \n%3sDate%10sWeight%6sBodyFat%4sMuscleMass \n%s%n",
        window, displayLimit, " ", " ", " ", " ", collect);
  }
}

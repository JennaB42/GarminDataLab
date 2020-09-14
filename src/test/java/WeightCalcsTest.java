import static org.junit.Assert.*;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

public class WeightCalcsTest {
  Double assertDoubleDelta = 0.0001;

  BodyData aug26 =
      new BodyData(
          LocalDate.of(2020, 8, 26), 145.3, OptionalDouble.of(31.2), OptionalDouble.of(50.8));
  BodyData aug27 =
      new BodyData(
          LocalDate.of(2020, 8, 27), 144.8, OptionalDouble.of(31.1), OptionalDouble.of(50.6));
  BodyData aug28 =
      new BodyData(
          LocalDate.of(2020, 8, 28), 145.8, OptionalDouble.of(31.4), OptionalDouble.of(50.8));
  BodyData aug30 =
      new BodyData(
          LocalDate.of(2020, 8, 30), 146.3, OptionalDouble.of(31.5), OptionalDouble.of(51.0));
  BodyData aug31 =
      new BodyData(
          LocalDate.of(2020, 8, 31), 145.7, OptionalDouble.empty(), OptionalDouble.empty());
  BodyData sep1 =
      new BodyData(
          LocalDate.of(2020, 9, 1), 144.9, OptionalDouble.of(31.2), OptionalDouble.of(50.6));
  BodyData sep2 =
      new BodyData(
          LocalDate.of(2020, 9, 2), 144.5, OptionalDouble.of(31.1), OptionalDouble.of(50.6));
  BodyData sep4 =
      new BodyData(
          LocalDate.of(2020, 9, 4), 144.5, OptionalDouble.of(31.1), OptionalDouble.of(50.5));

  // WeightWeek: Aug 26 - Sep 4
  //    no data for Aug 29 or Sep 3
  //    empty optionals for Aug 31
  ArrayList<BodyData> weekData =
      new ArrayList<>(Arrays.asList(sep4, sep2, sep1, aug31, aug30, aug28, aug27, aug26));

  @Test
  public void timeframeInternalSubset() {
    LocalDate startDate = LocalDate.of(2020, 8, 30);
    LocalDate endDate = LocalDate.of(2020, 9, 1);

    WeightCalcs.Timeframe timeframe = WeightCalcs.getTimeframe(startDate, endDate, weekData);

    assertEquals(startDate, timeframe.startDate);
    assertEquals(endDate, timeframe.endDate);
    List<BodyData> expectedData = Arrays.asList(sep1, aug31, aug30);
    assertEquals(expectedData, timeframe.data);
  }

  @Test
  public void timeframeNoOverlap() {
    LocalDate startDate = LocalDate.of(2020, 7, 30);
    LocalDate endDate = LocalDate.of(2020, 8, 1);

    WeightCalcs.Timeframe timeframe = WeightCalcs.getTimeframe(startDate, endDate, weekData);

    assertTrue(timeframe.data.isEmpty());
  }

  @Test
  public void timeframeContainedWithoutExactBoundaryDate() {
    LocalDate startDate = LocalDate.of(2020, 8, 29);
    LocalDate endDate = LocalDate.of(2020, 9, 3);

    WeightCalcs.Timeframe timeframe = WeightCalcs.getTimeframe(startDate, endDate, weekData);

    assertEquals(LocalDate.of(2020, 8, 30), timeframe.startDate);
    assertEquals(LocalDate.of(2020, 9, 2), timeframe.endDate);
    List<BodyData> expectedData = Arrays.asList(sep2, sep1, aug31, aug30);
    assertEquals(expectedData, timeframe.data);
  }

  @Test
  public void bandpassNoHitsWeight() {
    LocalDate startDate = LocalDate.of(2020, 8, 26);
    LocalDate endDate = LocalDate.of(2020, 9, 4);

    WeightCalcs.BandPassResult result =
        WeightCalcs.bandpassWeight(143.00, .005, startDate, endDate, weekData);

    assertEquals(0.0, result.percent, assertDoubleDelta);
    assertEquals(startDate, result.startDate);
    assertEquals(endDate, result.endDate);
  }

  @Test
  public void bandpassSomeHitsWeight() {
    LocalDate startDate = LocalDate.of(2020, 8, 26);
    LocalDate endDate = LocalDate.of(2020, 9, 4);

    WeightCalcs.BandPassResult result =
        WeightCalcs.bandpassWeight(144.5, .005, startDate, endDate, weekData);

    assertEquals(.50, result.percent, assertDoubleDelta);
    assertEquals(startDate, result.startDate);
    assertEquals(endDate, result.endDate);
  }

  @Test
  public void bandpassSomeHitsGappyBodyFat() {
    LocalDate startDate = LocalDate.of(2020, 8, 26);
    LocalDate endDate = LocalDate.of(2020, 9, 4);

    WeightCalcs.BandPassResult result =
        WeightCalcs.bandpassBodyFat(31.0, .005, startDate, endDate, weekData);

    assertEquals(3.0 / 8.0, result.percent, assertDoubleDelta);
    assertEquals(startDate, result.startDate);
    assertEquals(endDate, result.endDate);
  }

  @Test
  public void rollingAvgWeightData() {
    ArrayList<BodyData> threeDayAvg = WeightCalcs.rollingAvg(3, weekData);

    assertEquals(8, threeDayAvg.size());

    // full three day window
    double rolledSep4 = (sep4.getWeight() + sep2.getWeight() + sep1.getWeight()) / 3.0;
    assertEquals(rolledSep4, threeDayAvg.get(0).getWeight(), assertDoubleDelta);

    // partial two day window
    double rolledAug27 = (aug27.getWeight() + aug26.getWeight()) / 2.0;
    assertEquals(rolledAug27, threeDayAvg.get(6).getWeight(), assertDoubleDelta);

    // partial one day window
    assertEquals(aug26.getWeight(), threeDayAvg.get(7).getWeight(), assertDoubleDelta);
  }

  @Test
  public void rollingAvgBodyFatData() {
    ArrayList<BodyData> threeDayAvg = WeightCalcs.rollingAvg(3, weekData);

    assertEquals(8, threeDayAvg.size());

    // full three day window
    double rolledSep4 =
        (sep4.getBodyFat().orElseThrow()
                + sep2.getBodyFat().orElseThrow()
                + sep1.getBodyFat().orElseThrow())
            / 3.0;
    assertEquals(rolledSep4, threeDayAvg.get(0).getBodyFat().orElseThrow(), assertDoubleDelta);

    // partial two day window
    double rolledSep1 = (sep1.getBodyFat().orElseThrow() + aug30.getBodyFat().orElseThrow()) / 2.0;
    assertEquals(rolledSep1, threeDayAvg.get(2).getBodyFat().orElseThrow(), assertDoubleDelta);
  }

  @Test
  public void rollingAvgMuscleMassData() {
    ArrayList<BodyData> threeDayAvg = WeightCalcs.rollingAvg(3, weekData);

    assertEquals(8, threeDayAvg.size());

    // full three day window
    double rolledSep4 =
        (sep4.getMuscleMass().orElseThrow()
                + sep2.getMuscleMass().orElseThrow()
                + sep1.getMuscleMass().orElseThrow())
            / 3.0;
    assertEquals(rolledSep4, threeDayAvg.get(0).getMuscleMass().orElseThrow(), assertDoubleDelta);

    // partial two day window
    double rolledSep1 =
        (sep1.getMuscleMass().orElseThrow() + aug30.getMuscleMass().orElseThrow()) / 2.0;
    assertEquals(rolledSep1, threeDayAvg.get(2).getMuscleMass().orElseThrow(), assertDoubleDelta);
  }
}

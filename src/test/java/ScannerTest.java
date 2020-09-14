import static org.junit.Assert.*;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;

public class ScannerTest {

  @Test
  public void multipleEntriesPerDay() {
    Scanner scanner = new Scanner();
    ArrayList<BodyData> data =
        scanner.readFile(
            "/Users/jennabarton/IdeaProjects/GarminDataLab/src/test/resources/RepeatEntriesPerDay.csv");

    assertEquals(1, data.size());

    BodyData bodyData = data.get(0);
    assertEquals(LocalDate.of(2020, 7, 23), bodyData.getDate());
    assertEquals(144.9, bodyData.getWeight(), 0.0);
    assertEquals(31.0, bodyData.getBodyFat().orElseThrow(), 0.0);
    assertEquals(50.8, bodyData.getMuscleMass().orElseThrow(), 0.0);
  }

  @Test
  public void missingDataEntries() {
    Scanner scanner = new Scanner();
    ArrayList<BodyData> data =
        scanner.readFile(
            "/Users/jennabarton/IdeaProjects/GarminDataLab/src/test/resources/MissingDataEntries.csv");

    assertEquals(3, data.size());

    BodyData bodyData = data.get(1);
    assertEquals(LocalDate.of(2020, 9, 2), bodyData.getDate());
    assertEquals(145.0, bodyData.getWeight(), 0.0);
    assertTrue(bodyData.getBodyFat().isEmpty());
    assertTrue(bodyData.getMuscleMass().isEmpty());
  }
}

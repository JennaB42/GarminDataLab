import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalDouble;

public class Scanner {

  /**
   * Parse out desired data pieces. Data in the form of:
   *
   * <p>Time,Weight,Change,BMI,Body Fat,Skeletal Muscle Mass,Bone Mass,Body Water
   */
  private static BodyData parseBodyData(LocalDate date, String data) {
    String splitBy = ",";
    String invalidData = "--";

    String[] parsed = data.split(splitBy);
    double weight = Double.parseDouble(parsed[1].substring(0, parsed[1].indexOf(" ")));

    OptionalDouble bodyFat;
    try {
      if (!parsed[4].contains(invalidData)) {
        bodyFat =
            OptionalDouble.of(Double.parseDouble(parsed[4].substring(0, parsed[4].indexOf(" "))));
      } else bodyFat = OptionalDouble.empty();
    } catch (NumberFormatException e) {
      bodyFat = OptionalDouble.empty();
    }

    OptionalDouble muscleMass;
    try {
      if (!parsed[5].contains(invalidData)) {
        muscleMass =
            OptionalDouble.of(Double.parseDouble(parsed[5].substring(0, parsed[5].indexOf(" "))));
      } else muscleMass = OptionalDouble.empty();
    } catch (NumberFormatException e) {
      muscleMass = OptionalDouble.empty();
    }

    return new BodyData(date, weight, bodyFat, muscleMass);
  }

  public ArrayList<BodyData> readFile(String fileName) {
    String data;
    ArrayList<BodyData> allData = new ArrayList<>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      reader.readLine(); // read heading line
      data = reader.readLine();

      while (data != null) {
        Optional<LocalDate> dateOptional = BodyData.parseDate(data);
        if (dateOptional.isPresent()) {
          LocalDate date = dateOptional.get();
          BodyData bodyData;
          boolean readingData = true;

          if ((data = reader.readLine()) != null) {
            // parse out first data entry
            bodyData = parseBodyData(date, data);

            while (readingData) {
              // continue to parse until you get to a date or null
              data = reader.readLine();
              if (data != null) {
                // need to check if repeat data entries for this date
                dateOptional = BodyData.parseDate(data);
                if (dateOptional.isEmpty()) {
                  // there is more data; propagate the earliest data point
                  bodyData = parseBodyData(date, data);
                } else readingData = false;
              } else readingData = false;
            }
          } else {
            throw new IllegalStateException(
                String.format("Data for date %s does not exists", dateOptional.get()));
          }
          allData.add(bodyData);
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return allData;
  }
}

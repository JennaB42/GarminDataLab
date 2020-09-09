import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

public class BodyData {
    private LocalDate date;
    private Double weight;
    private OptionalDouble bodyFat;
    private OptionalDouble muscleMass;

    public BodyData(
            LocalDate date, Double weight, OptionalDouble bodyFat, OptionalDouble muscleMass) {
        this.date = date;
        this.weight = weight;
        this.bodyFat = bodyFat;
        this.muscleMass = muscleMass;
    }

    public Double getWeight() {
        return weight;
    }

    public LocalDate getDate() {
        return date;
    }

    public OptionalDouble getBodyFat() {
        return bodyFat;
    }

    public OptionalDouble getMuscleMass() {
        return muscleMass;
    }

    @Override
    public String toString() {
        if (bodyFat.isPresent())
            return date
                    + " weight "
                    + String.format("%.2f", weight)
                    + " bodyfat "
                    + String.format("%.1f%%", bodyFat.getAsDouble());
        else return date + " weight " + String.format("%.2f", weight) + " bodyfat N/A";
    }

    public String prettyPrint() {
        if (bodyFat.isPresent() && muscleMass.isPresent())
            return String.format(
                    "%s     %.2f lbs     %.1f%%     %.2f lbs",
                    date, weight, bodyFat.getAsDouble(), muscleMass.getAsDouble());
        else if (bodyFat.isPresent())
            return String.format("%s     %.2f lbs     %.1f%%", date, weight, bodyFat.getAsDouble());
        else if (muscleMass.isPresent())
            return String.format(
                    "%s     %.2f lbs               %.2f lbs", date, weight, muscleMass.getAsDouble());
        else return String.format("%s     %.2f lbs", date, weight);
    }
}

class Date {
    private String month; // jrb: this should turn into a switch to get the month as an int?
    private int day;
    private int year;

    public Date(String month, int day, int year) {
        this.month = month;
        this.day = day;
        this.year = year;
    }

    public static Optional<LocalDate> parseDate(String data) {
        String dateRegex = "\" [A-Z][a-z]{2} [0-9]{1,2}, [0-9]{4}\",";

        if (data.matches(dateRegex)) {
            int month = 1;
            switch (data.substring(2, 5).toLowerCase()) {
                case "jan":
                    month = 1;
                    break;
                case "feb":
                    month = 2;
                    break;
                case "mar":
                    month = 3;
                    break;
                case "apr":
                    month = 4;
                    break;
                case "may":
                    month = 5;
                    break;
                case "jun":
                    month = 6;
                    break;
                case "jul":
                    month = 7;
                    break;
                case "aug":
                    month = 8;
                    break;
                case "sep":
                    month = 9;
                    break;
                case "oct":
                    month = 10;
                    break;
                case "nov":
                    month = 11;
                    break;
                case "dec":
                    month = 12;
                    break;
            }
            int day = Integer.parseInt(data.substring(6, data.indexOf(',')));
            int year = Integer.parseInt(data.substring(data.indexOf(',') + 2, data.length() - 2));
            return Optional.of(LocalDate.of(year, month, day));
        } else return Optional.empty();
    }

    @Override
    public String toString() {
        return month + " " + day + " " + year;
    }
}

class TimeFrameReport {
    LocalDate startDate;
    LocalDate endDate;
    double min;
    double max;
    double avg;

    public TimeFrameReport(
            LocalDate startDate, LocalDate endDate, double min, double max, double avg) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

    @Override
    public String toString() {
        return String.format("{Min: %.2f, Max: %.2f, Avg: %.2f}", min, max, avg);
    }
}


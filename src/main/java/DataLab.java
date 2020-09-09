import java.time.LocalDate;
import java.util.ArrayList;

public class DataLab {

    public static void main(String[] args) {

        String fileDir = "/Users/jennabarton/IdeaProjects/scratch/src/";

        // jrb: have scanner take in a directory and read all files
        Scanner scanner = new Scanner();
        ArrayList<BodyData> weekData = scanner.readFile(fileDir.concat("WeightWeek.csv"));
        ArrayList<BodyData> yearData = scanner.readFile(fileDir.concat("WeightYear.csv"));

        LocalDate startDate = LocalDate.of(2020, 6, 29);
        LocalDate endDate = LocalDate.of(2020, 9, 2);

        UserReports.rollingAvgReport(1, yearData, 4);
        UserReports.rollingAvgReport(7, yearData, 4);

        UserReports.bandpassWeightReport(145.0, .005, startDate, endDate, weekData);
        UserReports.bandpassWeightReport(145.0, .005, startDate, endDate, yearData);

        UserReports.bandpassBodyFatReport(31.0, .005, startDate, endDate, weekData);
        UserReports.bandpassBodyFatReport(31.0, .005, startDate, endDate, yearData);

        UserReports.monthReport(7, 2020, yearData);

        UserReports.timeframeReport(startDate, endDate, weekData);
    }
}

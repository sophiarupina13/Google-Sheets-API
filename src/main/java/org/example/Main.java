package org.example;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class Main {
    private static final String APPLICATION_NAME = "Trip to SPB";
    private static final String SPREADSHEET_ID = "1_v-V3nfwbZGWpuE-lnJD67-glY9PAXWLDKdayYN96CQ";
    private static final String RANGE = "Sheet1!A2:C21";

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        int hoursInSPB = 48;
        int nightToSleep = 2;
        int sleepInHours = 8;
        double freeHoursToSeeTheSights = hoursInSPB - (nightToSleep * sleepInHours);

        List<List<Object>> values = getValuesFromGoogleSheet();

        values.sort(java.util.Comparator.comparing(row -> -Integer.parseInt((String) row.get(2))));

        Map<String, String> sightsAndHoursToSpend = new LinkedHashMap<>();
        for (List<Object> row : values) {
            String sight = (String) row.get(0);
            String timeToSpend = (String) row.get(1);
            String hoursToSpend = timeToSpend.substring(0, timeToSpend.length() - 1).replace(',', '.');
            sightsAndHoursToSpend.put(sight, hoursToSpend);
        }
/*
        for (Map.Entry<String, String> entry : sightsAndHoursToSpend.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
*/
        Map<String, Double> optimalRoute = makeTheOptimalRoute(sightsAndHoursToSpend, freeHoursToSeeTheSights);
        for (Map.Entry<String, Double> entry : optimalRoute.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + "ч");
        }
    }

    private static List<List<Object>> getValuesFromGoogleSheet() throws GeneralSecurityException, IOException {
        String credentialsFilePath = "src/main/resources/credentials.json";
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsFilePath);

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        FileInputStream credentialsStream = new FileInputStream(credentialsFilePath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));


        Sheets sheetsService = new Sheets.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        List<List<Object>> values = response.getValues();

        return values;
    }

    private static Map<String, Double> makeTheOptimalRoute(Map<String, String> sightsAndHoursToSpend, double freeHoursToSeeTheSights) {
        Map<String, Double> optimalRoute = new LinkedHashMap<>();

        double totalTime = 0;

        for (Map.Entry<String, String> entry : sightsAndHoursToSpend.entrySet()) {
            String sight = entry.getKey();
            double hoursToSpend = Double.parseDouble(entry.getValue());

            if (totalTime + hoursToSpend <= freeHoursToSeeTheSights) {
                optimalRoute.put(sight, hoursToSpend);
                totalTime += hoursToSpend;
            }
        }

        return optimalRoute;
    }

}
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;

public class TestGemini {
    public static void main(String[] args) {
        try {
            String apiKey = "AIzaSyCyLnytdlMKXEmvjsbrXdt7W7eivDsKQSk";
            String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{ \"contents\": [{ \"parts\":[{ \"text\": \"Hello\" }] }] }";
            
            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);			
            }

            int code = conn.getResponseCode();
            System.out.println("Response Code: " + code);
            InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
            try (Scanner scanner = new Scanner(is, "UTF-8")) {
                System.out.println(scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class MockLoadTester {

	static String API_KEY_ID = "<API_KEY_ID>";
	static String API_KEY_SECRET = "<API_KEY_SECRET>";

	static String HARBOR_ID = "5c544422c7dc9735767b23ce";
	static String SHIP_ID = "5d3ccab3526ad28f53205574";
	static String WORKSPACE_ID = "347880";
	static String SERVICE_ID = "113902";
	static String TXN_ID = "3853352";


	public static void main(String[] args) throws IOException, InterruptedException {



		// Create 20 Mocks
		List<Long> mockIds = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			HttpURLConnection con = createPostConnection(
					"https://mock.blazemeter.com/api/v1/workspaces/"+WORKSPACE_ID+"/service-mocks");
			String createMockReqBody = "{\"id\":null, \"serviceId\":"+SERVICE_ID+", \"name\":\"mock" + i + "\", "
					+ "\"mockServiceTransactions\" : [ { \"txnId\" : "+TXN_ID+", \"priority\" : 10 } ], "
					+ "\"harborId\":\""+HARBOR_ID+"\", \"shipId\":\""+SHIP_ID+"\", \"type\":\"TRANSACTIONAL\"	}";

			sendRequest(con, createMockReqBody);
			String response = readResponse(con);
			System.out.println(response);
			JSONObject obj = new JSONObject(response);
			mockIds.add(obj.getJSONObject("result").getLong("id"));
		}

		//Deploy 20 mocks
		for (Long mockId : mockIds) {
			HttpURLConnection con = commonBasicConnection(
					"https://mock.blazemeter.com/api/v1/workspaces/"+WORKSPACE_ID+"/service-mocks/"+mockId+"/deploy");
			String response = readResponse(con);
			System.out.println(response);
		}

		//Wait till status is set to Running
		Map<Long,String> mockStatus = new HashMap<>();
		while (mockStatus.size() != mockIds.size()){
			for (Long mockId : mockIds) {
				if(mockStatus.containsKey(mockId)){
					continue;
				}
				HttpURLConnection con = commonBasicConnection(
						"https://mock.blazemeter.com/api/v1/workspaces/"+WORKSPACE_ID+"/service-mocks/"+mockId+"");
				String response = readResponse(con);
				System.out.println(response);
				JSONObject obj = new JSONObject(response);
				String status = obj.getJSONObject("result").getString("status");
				if(status.equalsIgnoreCase("RUNNING")){
					mockStatus.put(mockId, obj.getJSONObject("result").getString("status"));
				}
			}
			Thread.sleep(30000);
		}


	}

	private static void sendRequest(HttpURLConnection con, String body) throws IOException {
		try (OutputStream os = con.getOutputStream()) {
			byte[] input = body.getBytes(UTF_8);
			os.write(input, 0, input.length);
		}
	}

	private static String readResponse(HttpURLConnection con) {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			return response.toString();
		}catch (Exception e){
			System.out.println("Error "+ e.getMessage());
		}
		return null;
	}

	private static HttpURLConnection createPostConnection(String url) throws IOException {
		HttpURLConnection con = commonBasicConnection(url);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setDoOutput(true);
		return con;
	}

	private static HttpURLConnection commonBasicConnection(String url) throws IOException {
		URL createMockUrl = new URL(url);
		HttpURLConnection con = (HttpURLConnection) createMockUrl.openConnection();
		String auth = API_KEY_ID + ":" + API_KEY_SECRET;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		con.setRequestProperty("Authorization", authHeaderValue);
		con.setRequestProperty("Accept", "application/json");
		con.setRequestMethod("GET");
		return con;
	}


}

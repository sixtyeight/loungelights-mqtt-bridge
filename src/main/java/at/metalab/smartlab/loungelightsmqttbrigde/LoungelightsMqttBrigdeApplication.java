package at.metalab.smartlab.loungelightsmqttbrigde;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoungelightsMqttBrigdeApplication {

	private static IMqttClient client;

	private static String endpoint = "http://10.20.30.90:8083/set_color";

	private static MqttCallback mqttCallback = new MqttCallback() {

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			String strMessage = new String(message.getPayload(), "UTF-8");

			System.out.println(topic + ": " + strMessage);

			switch (topic) {
			case "homeassistant/light/loungelights_bridge/cmnd/POWER1":
				if ("ON".equals(strMessage)) {
					sendColor("strip", "FFFFFF");
				} else if ("OFF".equals(strMessage)) {
					sendColor("strip", "000000");
				}
				break;

			case "homeassistant/light/loungelights_bridge/cmnd/COLOR1":
				String[] rgb1 = StringUtils.split(strMessage, ",");
				sendColor("strip", String.format("%s%s%s", //
						hex(rgb1[0]), //
						hex(rgb1[1]), //
						hex(rgb1[2])));
				break;

			case "homeassistant/light/loungelights_bridge/cmnd/POWER2":
				if ("ON".equals(strMessage)) {
					sendColor("zumtobel", "FFFFFF");
				} else if ("OFF".equals(strMessage)) {
					sendColor("zumtobel", "000000");
				}
				break;

			case "homeassistant/light/loungelights_bridge/cmnd/COLOR2":
				String[] rgb2 = StringUtils.split(strMessage, ",");
				sendColor("zumtobel", String.format("%s%s%s", //
						hex(rgb2[0]), //
						hex(rgb2[1]), //
						hex(rgb2[2])));
				break;
			}

			System.out.println("done");
		}

		private String hex(String value) {
			String hex = Integer.toHexString(Integer.valueOf(value));
			if (hex.length() == 1) {
				hex = "0" + hex;
			}
			return hex;
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
		}
	};

	private static void sendColor(String device, String rgb) {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			String strUrl = endpoint + "/" + device + ":" + rgb;
			HttpGet httpGet = new HttpGet(strUrl);
			httpclient.execute(httpGet);

			System.out.println(strUrl);
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		String publisherId = "loungelights_bridge";

		client = new MqttClient("tcp://10.20.30.97:1883", publisherId,
				new MqttDefaultFilePersistence("/tmp/" + publisherId));

		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName("DVES_USER");
		options.setPassword("DVES_PASS".toCharArray());
		options.setAutomaticReconnect(true);
		options.setCleanSession(false);
		options.setConnectionTimeout(30);
		options.setWill("homeassistant/light/loungelights_bridge/LWT", "Offline".getBytes(), 0, true);

		client.setCallback(mqttCallback);
		client.connect(options);

		client.subscribe("homeassistant/light/loungelights_bridge/cmnd/POWER1");
		client.subscribe("homeassistant/light/loungelights_bridge/cmnd/COLOR1");
		client.subscribe("homeassistant/light/loungelights_bridge/cmnd/POWER2");
		client.subscribe("homeassistant/light/loungelights_bridge/cmnd/COLOR2");

		client.publish("homeassistant/light/loungelights_bridge/LWT", "Online".getBytes(), 0, true);

		SpringApplication.run(LoungelightsMqttBrigdeApplication.class, args);
	}

}

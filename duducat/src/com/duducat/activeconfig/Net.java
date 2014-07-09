package com.duducat.activeconfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

class Net {

	static class ServerResponse {

		int code;

		String msg;

		Object data;

		boolean OK() {
			return code == 0;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			return sb.append("code=").append(code).append(",").append("msg=")
					.append(msg).append(",").append("data=").append(data)
					.toString();
		}
	}

	static ServerResponse register(String appid, String appsecret, String info) {
		String url = String
				.format("http://api.duducat.com/ActiveConfig/v1/Register/");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("appid", appid));
		pairs.add(new BasicNameValuePair("secretkey", appsecret));
		pairs.add(new BasicNameValuePair("info", info));
		return postWithURL(url, pairs);
	}

	static ConfigItem requestWithKey(String appKey, String appSecret,
			String key, int type) {
		String k = String.format("%s:%d", key, type);
		String urlStr = String
				.format("http://api.duducat.com/ActiveConfig/v1/GetKey/?key=%s&appid=%s&secretkey=%s",
						k, appKey, appSecret);
		ServerResponse response = getWithURL(urlStr);
		if (response != null) {
			try {
				List<ConfigItem> models = jtm(response.data);
				if (models.size() > 0) {
					return models.get(0);
				}
			} catch (JSONException e) {
				Logger.e(String.format(
						"Json parse error with key = %s, response = %s", key,
						response), e);
			}
		}
		return null;
	}

	static List<ConfigItem> updateAll(String appKey, String appSecret,
			String keyAndHash) {
		String urlStr = String
				.format("http://api.duducat.com/ActiveConfig/v1/CheckUpdate/?key=%s&appid=%s&secretkey=%s",
						keyAndHash, appKey, appSecret);
		ServerResponse response = getWithURL(urlStr);
		List<ConfigItem> models = new ArrayList<ConfigItem>();
		if (response != null) {
			try {
				models = jtm(response.data);
			} catch (JSONException e) {
				Logger.e(String.format(
						"Json parse error with keyAndHash = %s, response = %s",
						keyAndHash, response), e);
			}
		}
		return models;
	}

	private static ServerResponse postWithURL(String URL,
			List<NameValuePair> pairs) {
		HttpPost request = new HttpPost(URL);
		try {
			request.setEntity(new UrlEncodedFormEntity(pairs));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return responseFromRequest(request);
	}

	private static ServerResponse getWithURL(String URL) {
		Logger.i(URL);
		HttpGet request = new HttpGet(URL);
		request.setHeader("author", "duducat");
		return responseFromRequest(request);
	}

	static class ConfigException extends Exception {
		public ConfigException() {
			// TODO Auto-generated constructor stub
		}

		public ConfigException(String msg) {
			super(msg);
		}

		public ConfigException(String msg, Throwable inner) {
			super(msg, inner);
		}

		public ConfigException(Throwable inner) {
			super(inner);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -3997118407491791006L;
	}

	static class FileNotFoundException extends ConfigException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7498233287007421410L;


	}

	static byte[] getFileWithUrl(String url) throws Exception {
		Logger.i(url);
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		try {
			HttpResponse rsp = client.execute(get);

			StatusLine status = rsp.getStatusLine();
			if (status.getStatusCode() == 200) {
				return EntityUtils.toByteArray(rsp.getEntity());
			} else {
				throw new FileNotFoundException();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ConfigException(e);
		}
	}

	private static ServerResponse responseFromRequest(HttpRequestBase request) {
		Logger.i(request.getURI().toString());
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		try {
			response = client.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String line;
				StringBuilder sb = new StringBuilder();
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				return str(sb.toString());
			} else {
				Logger.e(String
						.format("Request error, url =%s, status code = %d, reason = %s",
								request.getURI(), response.getStatusLine()
										.getStatusCode(), response
										.getStatusLine().getReasonPhrase()));
			}
		} catch (IOException e) {
			Logger.e("Request exception, url = " + request.getURI(), e);
		}
		return null;
	}

	/**
	 * Json to model
	 * 
	 * @param object
	 *            the object should be type of JsonArray or JsonObject
	 * @throws JSONException
	 */
	private static List<ConfigItem> jtm(Object object) throws JSONException {
		List<ConfigItem> models = new ArrayList<ConfigItem>();
		if (object != null) {
			JSONArray array = (JSONArray) object;
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				ConfigItem model = ConfigItem.fromJsonObject(jsonObject);
				if (model != null) {
					models.add(model);
				}
			}
		}
		return models;
	}

	/**
	 * String to duducat response
	 */
	private static ServerResponse str(String response) {
		ServerResponse r = null;
		if (response != null) {
			JSONTokener tokener = new JSONTokener(response);
			try {
				JSONObject object = (JSONObject) tokener.nextValue();
				r = new ServerResponse();
				r.code = object.getInt("code");
				r.msg = object.getString("msg");
				r.data = object.get("data");
			} catch (JSONException e) {
				r = null;
				Logger.e("", e);
			}
		}
		return r;
	}
}

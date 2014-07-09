package com.duducat.activeconfig;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

class ConfigItem {

	String key;

	int type;

	String value;

	String md5;

	String status;

	Date expireTime;

	public static ConfigItem fromJsonObject(JSONObject jsonObject) {
		ConfigItem model = null;
		String key = jsonObject.optString("key");
		if (key != null && key.trim().length() > 0) {
			model = new ConfigItem();
			model.key = key;
			model.value = jsonObject.optString("value");
			model.md5 = jsonObject.optString("md5");
			model.status = jsonObject.optString("status");
			if (model.isSuccess()) {
				model.expireTime = new Date(1000 * Long.parseLong(jsonObject.optString("endtime")));
				model.type = Integer.parseInt(jsonObject.optString("type"));
			}
		}
		return model;
	}

	boolean isExpired() {
		return expireTime != null && expireTime.getTime() < new Date().getTime();
	}

	boolean isValid() {
		return isSuccess() && !isExpired();
	}

	boolean isSuccess() {
		// single getKey will not return status,
		// so when the status is empty it is success
		return status.equals(StatusEnum.SUCCESS.getStatus()) || status.equals("");
	}

	boolean isNoupdate() {
		return status.equals(StatusEnum.NOUPDATE.getStatus());
	}

	enum StatusEnum {
		SUCCESS("success"), INVALID("invalid"), NOUPDATE("noupdate");
		private String status;

		private StatusEnum(String status) {
			this.status = status;
		}

		public String getStatus() {
			return status;
		}

	}

	public String toString() {
		JSONStringer js = new JSONStringer();
		try {
			return js.object().key("key").value(key).key("value").value(value).key("md5").value(md5).key("status").value(status).key("type").value(type).key("endDate").value(expireTime).endObject().toString();
		} catch (JSONException e) {
			Logger.e("json error", e);
			return "ConfigItem toString exception";
		}
	}
}

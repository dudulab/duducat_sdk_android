package com.duducat.activeconfig;

import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONStringer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.WindowManager;

class Utility {
	static String join(List<String> strs, String delimiter) {
		StringBuilder sb = new StringBuilder();
		if (strs != null && strs.size() > 0) {
			for (int i = 0; i < strs.size(); i++) {
				String v = strs.get(i);
				sb.append(v);
				if (i < strs.size() - 1) {
					sb.append(delimiter);
				}
			}
		}
		return sb.toString();
	}

	static String getInformation(Context context) {
		try {
			String version = Build.VERSION.RELEASE + "" + Build.ID;
			String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
			String lang = Locale.getDefault().getDisplayLanguage();
			String resolution = getDeviceResolution(context);
			String carrier = getCarrierName(context);
			return new JSONStringer().object().key("sys").value("android").key("version").value(version).key("device").value(deviceName).key("lang").value(lang).key("resolution").value(resolution).key("carrier").value(carrier).endObject().toString();
		} catch (JSONException e) {
			Logger.e("", e);
			return null;
		}
	}

	static String getDeviceResolution(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getWidth() + "*" + display.getHeight();
	}

	static String getCarrierName(Context context) {
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return manager.getNetworkOperatorName();
	}

	static Drawable getDrawable(byte[] bytes) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		return new BitmapDrawable(bitmap);
	}

}

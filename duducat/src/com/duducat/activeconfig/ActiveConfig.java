package com.duducat.activeconfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.duducat.activeconfig.Net.FileNotFoundException;
import com.duducat.activeconfig.Net.ServerResponse;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

enum ConfigType {
	Text(0), Image(1);

	int type;

	ConfigType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}

enum ItemStatus {
	OK("OK"), InvalidKey("InvalidKey"), InvalidPath("InvalidPath");

	String status;

	ItemStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}
}

public class ActiveConfig {

	private static boolean isRegistered;
	static Context context;
	private static String appKey;
	private static String appSecret;
	private static Db db;
	private static Timer timer;

	private static long updateInterval = 3600 * 1000;

	public static void register(final Context appContext, final String appKey, final String appSecret) {
		ActiveConfig.context = appContext;
		ActiveConfig.appKey = appKey;
		ActiveConfig.appSecret = appSecret;
		db = new Db(context);
		isRegistered = true;

		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... arg0) {
				String deviceInfo = Utility.getInformation(context);
				ServerResponse rsp = Net.register(appKey, appSecret, deviceInfo);
				if (rsp == null) {
					Logger.e("login failed");
					return false;
				}

				return true;
			}
		}.execute((Void[]) null);

		timer = new Timer();
		timer.schedule(new AutoUpdater(), updateInterval, updateInterval);
	}

	public static String getText(String key, String defaultValue) {
		if (!isRegistered) {
			throw new IllegalStateException("call register first");
		}

		ConfigItem item = db.get(key, ConfigType.Text.getType());
		if (item != null && !item.isExpired()) {
			return item.value;
		}

		if (item == null || !item.status.equalsIgnoreCase(ItemStatus.InvalidKey.getStatus())) {
			item = Net.requestWithKey(appKey, appSecret, key, ConfigType.Text.getType());
			if (item != null) {
				db.save(item);
				return item.value;
			}
		}

		return defaultValue;
	}

	public interface AsyncGetTextHandler {
		void OnSuccess(String value);

		void OnFailed();
	}

	public static void getTextAsync(final String key, final AsyncGetTextHandler handler) {
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... arg0) {
				return getText(key, null);
			}

			@Override
			protected void onPostExecute(String result) {
				if (result != null) {
					handler.OnSuccess(result);
				} else {
					handler.OnFailed();
				}
			}
		}.execute((Void[]) null);
	}

	static HashMap<Integer, String> textControls = new HashMap<Integer, String>();

	public static void setTextViewWithKey(final String key, final String defaultValue, final TextView textView) {
		textControls.put(textView.hashCode(), key);
		getTextAsync(key, new AsyncGetTextHandler() {

			@Override
			public void OnSuccess(String value) {
				if (textControls.get(textView.hashCode()).equals(key)) {
					textView.setText(value);
				}
			}

			@Override
			public void OnFailed() {
				if (textControls.get(textView.hashCode()).equals(key)) {
					textView.setText(defaultValue);
				}
			}
		});
	}

	public static Drawable getImage(String key, Drawable defaultValue) {
		if (!isRegistered) {
			throw new IllegalStateException("call register first");
		}

		try {
			ConfigItem item = db.get(key, ConfigType.Image.getType());
			if (item != null && !item.isExpired() && FileHelper.isExist(key)) {
				return Utility.getDrawable(FileHelper.readFile(key));
			}

			if (item != null) {
				if (item.status.equalsIgnoreCase(ItemStatus.InvalidKey.getStatus())) {
					return defaultValue;
				}

				if (!item.isExpired()) {

					if ( FileHelper.isExist(key)) {
						return Utility.getDrawable(FileHelper.readFile(key));
					} else if (!item.status.equalsIgnoreCase(ItemStatus.InvalidPath.getStatus())) {
						FileHelper.saveFile(key, Net.getFileWithUrl(item.value));
						item.status = ItemStatus.OK.getStatus();
						db.save(item);
						return Utility.getDrawable(FileHelper.readFile(key));
					} else {
						Logger.w("invalid path for key " + item.key);
						return defaultValue;
					}
				}
			}

			item = Net.requestWithKey(appKey, appSecret, key, ConfigType.Image.getType());
			if (item != null) {
				db.save(item);
				FileHelper.deleteFile(key);
				FileHelper.saveFile(key, Net.getFileWithUrl(item.value));
				item.status = ItemStatus.OK.getStatus();
				db.save(item);
				return Utility.getDrawable(FileHelper.readFile(key));
			} else {
				Logger.e("empty response");
				return defaultValue;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return defaultValue;
		}
	}

	static HashMap<Integer, String> imageControls = new HashMap<Integer, String>();

	public static void setImageViewWithKey(final String key, final Drawable defaultValue, final ImageView imageView) {
		imageControls.put(imageView.hashCode(), key);
		getImageAsync(key, new AsyncGetImageHandler() {

			@Override
			public void OnSuccess(Drawable result) {
				if (imageControls.get(imageView.hashCode()).equals(key)) {
					imageView.setImageDrawable(result);
				}
			}

			@Override
			public void OnFailed() {
				if (imageControls.get(imageView.hashCode()).equals(key)) {
					imageView.setImageDrawable(defaultValue);
				}
			}
		});
	}

	public interface AsyncGetImageHandler {
		void OnSuccess(Drawable result);

		void OnFailed();
	}

	public static void getImageAsync(final String key, final AsyncGetImageHandler handler) {
		new AsyncTask<Void, Void, Drawable>() {

			@Override
			protected Drawable doInBackground(Void... arg0) {
				return getImage(key, null);
			}

			@Override
			protected void onPostExecute(Drawable result) {
				if (result != null) {
					handler.OnSuccess(result);
				} else {
					handler.OnFailed();
				}
			}
		}.execute((Void[]) null);
	}

	private static List<ConfigItem> updateAllItems() {
		if (!isRegistered) {
			throw new IllegalStateException("call register first");
		}

		List<String> keyAndHashes = db.getAllKeys();
		List<ConfigItem> updated = new ArrayList<ConfigItem>();
		if (keyAndHashes.size() > 0) {
			List<ConfigItem> models = Net.updateAll(appKey, appSecret, Utility.join(keyAndHashes, ""));
			if (models.size() > 0) {
				for (ConfigItem model : models) {
					if (!model.isNoupdate()) {
						if (model.type == ConfigType.Image.getType()) {
							try {
								FileHelper.deleteFile(model.key);
								FileHelper.saveFile(model.key, Net.getFileWithUrl(model.value));
								updated.add(model);
							} catch (FileNotFoundException e) {
								model.status = ItemStatus.InvalidPath.getStatus();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							updated.add(model);
						}

						db.save(model);
					}
				}
			}
		}
		return updated;
	}

	static AsyncUpdateAllHandler updateAllHandler;

	public static void setUpdateAllHandler(AsyncUpdateAllHandler handler) {
		updateAllHandler = handler;
	}

	public interface AsyncUpdateAllHandler {
		void OnTextUpdated(String key, String value);

		void OnImageUpdated(String key, Drawable value);
	}

	public static void updateAll() {
		new AsyncTask<Void, Void, List<ConfigItem>>() {

			@Override
			protected List<ConfigItem> doInBackground(Void... arg0) {
				return updateAllItems();
			}

			@Override
			protected void onPostExecute(List<ConfigItem> result) {
				if (updateAllHandler != null) {
					for (ConfigItem item : result) {
						if (item.type == ConfigType.Text.getType()) {
							updateAllHandler.OnTextUpdated(item.key, item.value);
						} else {
							updateAllHandler.OnImageUpdated(item.key, Utility.getDrawable(FileHelper.readFile(item.key)));
						}
					}
				}
			}
		}.execute((Void[]) null);
	}

	public static void setUpdateInterval(long interval) {
		ActiveConfig.updateInterval = interval;
		timer.schedule(new AutoUpdater(), 0, updateInterval);
	}

	public static long getUpdateInterval() {
		return updateInterval;
	}

	public static void clearCache() {
		if (!isRegistered) {
			throw new IllegalStateException("call register first");
		}

		db.clear();
	}

	private static class AutoUpdater extends TimerTask {

		@Override
		public void run() {
			Logger.i("update all keys at time = " + new Date().getTime());
			updateAll();
		}
	}

}

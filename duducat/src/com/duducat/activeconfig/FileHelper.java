package com.duducat.activeconfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.content.Context;

class FileHelper {

	static void saveFile(String key, byte[] content) {
		File folder = ActiveConfig.context.getDir("duducat", Context.MODE_PRIVATE);
		File f = new File(folder, key);
		try {
			FileOutputStream out = new FileOutputStream(f);
			out.write(content);
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static boolean isExist(String key) {
		File folder = ActiveConfig.context.getDir("duducat", Context.MODE_PRIVATE);
		File f = new File(folder, key);
		return f.exists();
	}

	static byte[] getBytesFromInputStream(InputStream is) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[0xFFFF];

			for (int len; (len = is.read(buffer)) != -1;)
				os.write(buffer, 0, len);

			os.flush();

			return os.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	static byte[] readFile(String key) {
		byte[] content = null;
		File folder = ActiveConfig.context.getDir("duducat", Context.MODE_PRIVATE);
		File f = new File(folder, key);
		try {
			FileInputStream inputStream = new FileInputStream(f);
			content = getBytesFromInputStream(inputStream);
			inputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return content;
	}

	static boolean deleteFile(String key) {
		File folder = ActiveConfig.context.getDir("duducat", Context.MODE_PRIVATE);
		File f = new File(folder, key);
		return f.delete();
	}
}

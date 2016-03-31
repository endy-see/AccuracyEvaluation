package com.hanvon.handwriting.sentence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.R.integer;
import android.content.Context;
import android.util.Log;

public class HanwangInterface {
	static private String dicFileName = "com_JFSLM_hwrc_18030.bin";
//	static private String dicFileName = "HW_REC_CN.bin";

	private Recognizer mRecognizer = null;

	public HanwangInterface() {

	}

	public int initializeHanwangEngine(Context mContext, int mode, int range) {
		if (mContext == null) {
			return -1;
		}

		if (!LoadRecDic(mContext)) {
			return -2;
		}

		int returnValue = 0;
		mRecognizer = Recognizer.Create(mode);
		mRecognizer.setRecogRange(range);
		Log.e("HanwangInterface:", "汉王初始化引擎成功！returnValue="+returnValue);
		return returnValue;
	}

	public void destroyHanwangEngine() {
		if (mRecognizer != null) {
			Recognizer.Destroy(mRecognizer);
			mRecognizer = null;
		}
	}

	public int getOverlayRecognition(short[] inputPoints,
			List<String> candidateResults) {
		boolean isNull = candidateResults==null;
		Log.e("candidateResults.size=", candidateResults.size()+"");
		Log.e("candidateResults==null?", isNull+"");
		if (mRecognizer == null || inputPoints == null
				|| candidateResults == null || inputPoints.length == 0) {
			return -1;
		}

		return mRecognizer.getOverlayRecognition(inputPoints, candidateResults);
	}

	public int getSingleRecognition(short[] inputPoints, char[] candidateResults) {
		if (mRecognizer == null) {
			return -1;
		}

		return mRecognizer.getSingleRecognition(inputPoints, candidateResults);
	}

	private static final String buildPath(String... paths) {
		StringBuilder builder = new StringBuilder();
		for (String path : paths) {
			if (path.substring(0, 1).equals("/")
					|| path.substring(path.length() - 1).equals("/")) {
				builder.append(path);
			} else {
				builder.append("/");
				builder.append(path);
			}
		}
		return builder.toString();
	}

	private static final boolean isFileExist(final String path) {
		File file = new File(path);
		try {
			if (file.isDirectory() || !file.exists()) {
				new File(path.substring(0, path.lastIndexOf("/"))).mkdirs();
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static void saveInputStreamToFile(InputStream zin, String filePath,
			boolean force) throws Exception {
		if (isFileExist(filePath) && !force) {
			return;
		}

		FileOutputStream out = new FileOutputStream(filePath);
		byte[] b = new byte[512];
		int len = 0;
		while ((len = zin.read(b)) != -1) {
			out.write(b, 0, len);
		}
		out.close();
	}

	private boolean LoadRecDic(Context context) {
		try {
			final String mMainDirectory = "/data/data/"
					+ context.getPackageName() + "/";
			final String mCoreDic = dicFileName;
			final File ftest = new File(mMainDirectory + mCoreDic);
			if (!ftest.exists()) {
				ZipFile zip = new ZipFile(context.getPackageCodePath());
				Enumeration<? extends ZipEntry> emu = zip.entries();
				String fileName;
				ZipEntry fileEntry;
				InputStream stream;
				while (emu.hasMoreElements()) {
					fileEntry = emu.nextElement();
					fileName = fileEntry.getName();
					if (fileName.substring(0, 6).equals("assets")) {
						stream = zip.getInputStream(fileEntry);
						saveInputStreamToFile(
								stream,
								buildPath(mMainDirectory, fileName.substring(7)),
								true);
						stream.close();
					}
				}
				zip.close();
			}

			Recognizer.SetRecDic(mMainDirectory + dicFileName);
			Log.e("HanangInterface", "LoadRecDic:"+mMainDirectory + dicFileName);
			return true;

		} catch (Exception e) {
			return false;
		}
	}
}

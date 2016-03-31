package com.example.accuracyevaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.hciilab.recognization.lib.gPenLibHandwrite;
import net.hciilab.recognization.lib.gPenTargetType;

import com.example.accuracyevaluation.R;
import com.hanvon.handwriting.sentence.HanwangInterface;
import com.hanvon.handwriting.sentence.Recognizer;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

@SuppressLint("NewApi")
public class AccuracyJudgeActivity extends Activity {

	private Button controlButton;
	private TextView resultTextView;

	private final boolean DEBUG = true;

	private static final String TAG = "AccuracyJudgeActivity";
	private static final String INPUT_FILE_PATH = "/sdcard/sogou_handwrite_accuracy_evaluation.data";
	private static final String OUTPUT_DIR = "/sdcard/handwritingevaluation/output";
	private static final String OUTPUT_FILE_SCUT = "scut.result";
	private static final String OUTPUT_FILE_HANWANG = "hanwang.result";
	// 相关文件名字
	private static final String SO_NAME = "libgpen_api_so.so";// 单字识别引擎
	private static final String LANGUAGE_FILENAME = "char_bigram.dic";// 语言模型文件
	// private static final String LANGUAGE_FILENAME = "sogou_fixed.dict";

	private FileReader mFileReader = null;
	private FileWriter mFileWriter = null;
	private BufferedReader mBufferedReader = null;
	private BufferedWriter mBufferedWriter = null;

	private int scheduledCompanyId = 1;// 0 means SCUT, 1 means HW
	private int buttonStatus = 0;// 0 means start, 1 means stop
	private long wholeStartTime = 0;// record whole start
	private long wholeEndTime = 0;// record whole end

	private int mReconginazeMode = gPenLibHandwrite.APPROACH_OVERLAP;
	private int mReconginazeModeHW = Recognizer.Mode_CHS_Sentence_Overlap;

	@Override
	public final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		controlButton = (Button) findViewById(R.id.controlButton);
		resultTextView = (TextView) findViewById(R.id.resultTextView);
		controlButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				switch (buttonStatus) {
				case 0:
					wholeStartTime = System.currentTimeMillis();
					updateUIThread mThread = new updateUIThread(
							scheduledCompanyId);
					new Thread(mThread).start();
					controlButton.setText("停止测试");
					buttonStatus = 1;
					break;

				case 1:
					isEnded = true;
					controlButton.setText("开始测试");// TODO:synchronized
					buttonStatus = 0;
					break;
				default:
					scheduledCompanyId = 0;
					buttonStatus = 0;
					break;
				}
			}
		});
	}

	// half width character and full width character interactive conversion
	/**
	 * ASCII表中可见字符从!开始，偏移位值为33(Decimal)
	 */
	static final char DBC_CHAR_START = 33; // 半角!

	/**
	 * ASCII表中可见字符到~结束，偏移位值为126(Decimal)
	 */
	static final char DBC_CHAR_END = 126; // 半角~

	/**
	 * 全角对应于ASCII表的可见字符从！开始，偏移值为65281
	 */
	static final char SBC_CHAR_START = 65281; // 全角！

	/**
	 * 全角对应于ASCII表的可见字符到～结束，偏移值为65374
	 */
	static final char SBC_CHAR_END = 65374; // 全角～

	/**
	 * ASCII表中除空格外的可见字符与对应的全角字符的相对偏移
	 */
	static final int CONVERT_STEP = 65248; // 全角半角转换间隔

	/**
	 * 全角空格的值，它没有遵从与ASCII的相对偏移，必须单独处理
	 */
	static final char SBC_SPACE = 12288; // 全角空格 12288

	/**
	 * 半角空格的值，在ASCII中为32(Decimal)
	 */
	static final char DBC_SPACE = 32; // 半角空格

	private char halfCharToFullChar(char halfChar) {
		if (halfChar < DBC_SPACE || halfChar > DBC_CHAR_END) {
			return halfChar;
		}

		if (halfChar >= DBC_CHAR_START && halfChar <= DBC_CHAR_END) {
			return (char) (halfChar + CONVERT_STEP);
		}

		return SBC_SPACE;
	}

	private char fullCharToHalfChar(char fullChar) {
		if (fullChar < SBC_CHAR_START || fullChar > SBC_CHAR_END) {
			if (fullChar == SBC_SPACE) {
				return DBC_SPACE;
			}

			return fullChar;
		}

		return (char) (fullChar - CONVERT_STEP);
	}

	private boolean isEnded = false;
	private static final int MSG_START = 0;
	private static final int MSG_SUCCESSFUL = 1;
	private static final int MSG_FAILED = 2;
	private static final int MSG_END = 3;
	private static final int MSG_ERROR = 4;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_START:
				// sleep a while for result capture
				if (scheduledCompanyId > 0 && scheduledCompanyId < 2) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				isEnded = false;
				resultTextView.setText((String) msg.obj);
				break;

			case MSG_SUCCESSFUL:
				resultTextView.setText((String) msg.obj);
				break;

			case MSG_FAILED:
				resultTextView.setText((String) msg.obj);
				isEnded = true;
				controlButton.setText("开始测试");// TODO:synchronized
				buttonStatus = 0;
				scheduledCompanyId = 0;
				break;

			case MSG_END:
				if (scheduledCompanyId == 1) {
					wholeEndTime = System.currentTimeMillis();
					resultTextView.setText((String) msg.obj + "\n完成所有测试，共耗时："
							+ ((wholeEndTime - wholeStartTime)) / 1000 + "s.");
				} else {
					resultTextView.setText((String) msg.obj);
				}

				scheduledCompanyId++;
				switch (scheduledCompanyId) {
				case 1:
					// handle Hanwang test
					updateUIThread hThread = new updateUIThread(
							scheduledCompanyId);
					new Thread(hThread).start();
					break;

				default:
					// test ended
					isEnded = true;
					controlButton.setText("开始测试");// TODO:synchronized
					buttonStatus = 0;
					scheduledCompanyId = 0;
					break;
				}

				break;

			case MSG_ERROR:
				//error code
				String errorMsg = "";
				switch (msg.arg1) {
				case gPenLibHandwrite.AHR_INIT_ERROR:
					errorMsg = "初始化识别引擎错误";
					break;
					
				case gPenLibHandwrite.AHR_CONF_ERROR:
					errorMsg = "配置识别模式错误";
					break;

				case gPenLibHandwrite.AHR_RESET_ERROR:
					errorMsg = "重置缓冲区错误";
					break;
					
				case gPenLibHandwrite.AHR_SPLIT_ERROR:
				case gPenLibHandwrite.AHR_RECOG_ERROR:
					errorMsg = "识别错误";
					break;
					
				default:
					break;
				}
				
				resultTextView.setText(errorMsg);
				isEnded = true;
				controlButton.setText("开始测试");// TODO:synchronized
				buttonStatus = 0;
				scheduledCompanyId = 0;
				break;
				
			default:
				break;
			}
		}
	};
	
	private void sendErrorMsg(int errorCode) {
		Message msg = new Message();
		msg.what = MSG_ERROR;
		msg.arg1 = errorCode;
		mHandler.sendMessage(msg);
	}

	private int openInputFile(String inputFilePath) {
		if (inputFilePath == null || inputFilePath.length() == 0) {
			return -1;
		}

		try {
			mFileReader = new FileReader(inputFilePath);
			mBufferedReader = new BufferedReader(mFileReader);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	private int closeInputFile() {
		try {
			if (mBufferedReader != null) {
				mBufferedReader.close();
				mBufferedReader = null;
			}

			if (mFileReader != null) {
				mFileReader.close();
				mFileReader = null;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	private int openOutputFile(String outputDir, String outputFileName) {
		if (outputDir == null || outputDir.length() == 0
				|| outputFileName == null || outputFileName.length() == 0) {
			return -1;
		}

		File mFile = new File(outputDir);
		if (mFile.isDirectory() == false) {
			if (mFile.mkdirs() == false) {
				return -1;
			}
		}

		String outputFilePath = outputDir + "/" + outputFileName;

		try {
			mFileWriter = new FileWriter(outputFilePath);
			mBufferedWriter = new BufferedWriter(mFileWriter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	private int closeOutputFile() {
		try {
			if (mBufferedWriter != null) {
				mBufferedWriter.close();
				mBufferedWriter = null;
			}

			if (mFileWriter != null) {
				mFileWriter.close();
				mFileWriter = null;
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	/**
	 * 从raw中直接复制文件.
	 * 
	 * @param context
	 *            上下文
	 * @param dirName
	 *            目录
	 * @param fileName
	 *            文件名字
	 * @param rawID
	 *            raw资源id
	 * @return 是否成功
	 */
	private boolean copyFromRaw(Context context, String dirName,
			String fileName, int rawID) {
		try {
			// set the file path for the resource file
			String wordlibFilename = dirName + "/" + fileName;
			File dir = new File(dirName);

			// if the dir in the path not exit, create one
			if (!dir.exists())
				dir.mkdir();

			// if the resource file not exit, create one
			if (!(new File(wordlibFilename)).exists()) {
				// copy the "word_lib.dic" in res/raw to RESOURCEFILEPATH
				InputStream is = context.getResources().openRawResource(rawID);
				FileOutputStream fos = new FileOutputStream(wordlibFilename);
				byte[] buffer = new byte[8192];
				int count = 0;

				try {
					// copying . . .
					while ((count = is.read(buffer)) > 0) {
						fos.write(buffer, 0, count);
					}
				} catch (Exception e) {
					return false;

				} finally {
					fos.close();
					is.close();
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private int initializeClassifier(gPenLibHandwrite mLib) {
		Log.e("initializeClass", "这个类有执行么");

		if (mLib == null) {
			return -1;
		}

		Context mContext = getApplication();
		String soName = mContext.getFilesDir().getParentFile().getPath()
				+ "/lib/" + SO_NAME;

		if (DEBUG) {
			Log.d(TAG, "-->soname:" + soName);
		}
		/**
		 * 获取软件主目录/data/data/packagename/files
		 */
		String languageFileName = mContext.getFilesDir().getPath() + "/"
				+ LANGUAGE_FILENAME;

		File file = new File(languageFileName);
		 if (!file.exists())// 如果语言模型文件不存在，则复制
		{
			copyFromRaw(mContext, mContext.getFilesDir().getPath(),
					LANGUAGE_FILENAME, R.raw.char_bigram);
			// LANGUAGE_FILENAME, R.raw.sogou_fixed);
		}

		return mLib.iSetClassifier(soName, languageFileName);
	}

	/**
	 * @return -1 means error, 0 means none correct, 1 means first correct, 2
	 *         means second correct, N means Nth correct
	 * */
	private int isScutCorrect(List<String> candidateResults,
			char[] standardResult) {
		if (candidateResults == null || candidateResults.size() == 0
				|| standardResult == null || standardResult.length == 0) {
			return -1;
		}

		int i = 0;
		int returnValue = 0;
		char[] candidateArray = null;
		int candidateNo = 0;
		for (String eachResult : candidateResults) {
			candidateNo++;
			if (eachResult.length() != standardResult.length) {
				continue;
			}

			candidateArray = eachResult.toCharArray();
			for (i = 0; i < candidateArray.length; i++) {
				if (halfCharToFullChar(candidateArray[i]) != halfCharToFullChar(standardResult[i])) {
					break;
				}
			}

			if (i == candidateArray.length) {
				returnValue = candidateNo;
				break;
			}
		}

		return returnValue;
	}

	private int handleScutAccuracyJudge() {
		long testStartTime = 0;
		long testEndTime = 0;
		testStartTime = System.currentTimeMillis();
		int returnValue = 0;
		int successAmount = 0;
		int failureAmount = 0;
		int totalAmount = 0;
		int firstAmount = 0;
		int fifthAmount = 0;
		int otherAmount = 0;
		long startTime = 0;
		long endTime = 0;
		long totalTime = 0;
		long eachTime = 0;
		long maxTime = 0;
		long minTime = Integer.MAX_VALUE;
		long averageTime = 0;
		float firstRatio = 0;
		float fifthRatio = 0;
		float wholeRatio = 0;

		// add memory usage statistics
		long beforeLoadClassifierNativeHeapSize = 0;
		long beforeLoadClassifierNativeHeapAllocatedSize = 0;
		long beforeLoadClassifierNativeHeapFreeSize = 0;
		long afterLoadClassifierNativeHeapSize = 0;
		long afterLoadClassifierNativeHeapAllocatedSize = 0;
		long afterLoadClassifierNativeHeapFreeSize = 0;
		long beforeIRecognizeNativeHeapSize = 0;
		long beforeIRecognizeNativeHeapAllocatedSize = 0;
		long beforeIRecognizeNativeHeapFreeSize = 0;
		long afterIRecognizeNativeHeapSize = 0;
		long afterIRecognizeNativeHeapAllocatedSize = 0;
		long afterIRecognizeNativeHeapFreeSize = 0;
		long beforeReleaseClassifierNativeHeapSize = 0;
		long beforeReleaseClassifierHeapAllocatedSize = 0;
		long beforeReleaseClassifierNativeHeapFreeSize = 0;
		long afterReleaseClassifierNativeHeapSize = 0;
		long afterReleaseClassifierHeapAllocatedSize = 0;
		long afterReleaseClassifierNativeHeapFreeSize = 0;
		long globalAllocSize = 0;
		long globalFreedSize = 0;
		String memoryUsageInfo = null;

		gPenLibHandwrite mLib = null;
		String eachLine = null;
		String[] splitArray = null;
		String[] pointsArray = null;
		String[] viceArray = null;
		String itemId = null;
		String noticeMessage = null;
		char[] standardResult = null;
		int candidateAmount = 0;
		int[] strokePoints = null;

		List<String> candidateResults = new ArrayList<String>();

		StringBuilder mBuilder = new StringBuilder();

		Message scutMessage = mHandler.obtainMessage(MSG_START);
		noticeMessage = "华南理工大学手写" + getModeName() + "准确率测试，已开始！";
		scutMessage.obj = noticeMessage;
		scutMessage.sendToTarget();

		// open input file
		returnValue = openInputFile(INPUT_FILE_PATH);
		if (returnValue != 0) {
			scutMessage = mHandler.obtainMessage(MSG_FAILED);
			noticeMessage = "华南理工大学手写" + getModeName() + "准确率测试，打开输入文件失败！";
			scutMessage.obj = noticeMessage;
			scutMessage.sendToTarget();
			return returnValue;
		}

		// open output file
		returnValue = openOutputFile(OUTPUT_DIR, OUTPUT_FILE_SCUT);
		if (returnValue != 0) {
			scutMessage = mHandler.obtainMessage(MSG_FAILED);
			noticeMessage = "华南理工大学手写" + getModeName() +"准确率测试，打开输出文件失败！";
			scutMessage.obj = noticeMessage;
			scutMessage.sendToTarget();
			return returnValue;
		}

		beforeLoadClassifierNativeHeapSize = Debug.getNativeHeapSize();
		beforeLoadClassifierNativeHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		beforeLoadClassifierNativeHeapFreeSize = Debug.getNativeHeapFreeSize();

		Debug.startAllocCounting();

		// initialize recognition engine
		mLib = gPenLibHandwrite.getInstance();
		returnValue = initializeClassifier(mLib);
		if (returnValue != 0) {
			sendErrorMsg(gPenLibHandwrite.AHR_INIT_ERROR);
			return returnValue;
		}
//		GPEN_VER_SIMPLIFIED
		mLib.iSetVersion(gPenLibHandwrite.GPEN_VER_MIX);
		//mLib.iSetVersion(gPenLibHandwrite.GPEN_VER_SIMPLIFIED);
		
		afterLoadClassifierNativeHeapSize = Debug.getNativeHeapSize();
		afterLoadClassifierNativeHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		afterLoadClassifierNativeHeapFreeSize = Debug.getNativeHeapFreeSize();
		memoryUsageInfo = "beforeLoadClassifierNativeHeapSize:"
				+ beforeLoadClassifierNativeHeapSize
				+ "bytes, beforeLoadClassifierNativeHeapAllocatedSize:"
				+ beforeLoadClassifierNativeHeapAllocatedSize
				+ "bytes, beforeLoadClassifierNativeHeapFreeSize:"
				+ beforeLoadClassifierNativeHeapFreeSize
				+ "bytes, afterLoadClassifierNativeHeapSize:"
				+ afterLoadClassifierNativeHeapSize
				+ "bytes, afterLoadClassifierNativeHeapAllocatedSize:"
				+ afterLoadClassifierNativeHeapAllocatedSize
				+ "bytes, afterLoadClassifierNativeHeapFreeSize:"
				+ afterLoadClassifierNativeHeapFreeSize + "bytes.\n";

		try {
			mBufferedWriter.write(memoryUsageInfo);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// add decimal format for scores
		DecimalFormat mFormat = new DecimalFormat("0.0000");

		try {
			while ((eachLine = mBufferedReader.readLine()) != null
					&& isEnded == false) {
				Log.e("handlerScutAccuracyJud:", "eachLine="+mBufferedReader.readLine());
				candidateResults.clear();

				splitArray = eachLine.split("(=)|(;)");// TODO:regex
				if (splitArray == null || splitArray.length != 3) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				itemId = splitArray[0];
				if (splitArray[2].length() < 6) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				viceArray = splitArray[2].split(",");
				if (viceArray == null || viceArray.length == 0) {
					failureAmount++;
					continue;
				}

				standardResult = new char[viceArray.length];
				for (int i = 0; i < standardResult.length; i++) {
					standardResult[i] = (char) Integer.parseInt(
							viceArray[i].replace("\\u", ""), 16);
				}

				pointsArray = splitArray[1].split(",");
				if (pointsArray == null || pointsArray.length == 0
						|| pointsArray.length % 2 != 0) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				// construct points
				strokePoints = new int[pointsArray.length];
				for (int i = 0; i < pointsArray.length; i++) {
					strokePoints[i] = Integer.valueOf(pointsArray[i]);
				}

				beforeIRecognizeNativeHeapSize = Debug.getNativeHeapSize();
				beforeIRecognizeNativeHeapAllocatedSize = Debug
						.getNativeHeapAllocatedSize();
				beforeIRecognizeNativeHeapFreeSize = Debug
						.getNativeHeapFreeSize();

				// get recognition results without gesture!!!
				startTime = System.currentTimeMillis();
				
				returnValue = mLib.setTargetAndMode(gPenTargetType.GPEN_TR_SPECIAL_DC,
						mReconginazeMode);
				if (returnValue != 0) {
					sendErrorMsg(gPenLibHandwrite.AHR_CONF_ERROR);
					return returnValue;
				}
				
				returnValue = mLib.iReset();
				if (returnValue != 0) {
					sendErrorMsg(gPenLibHandwrite.AHR_RESET_ERROR);
					return returnValue;
				}
				
				int index = 0;
				while (index < strokePoints.length) {
					int[] tmp = Arrays.copyOfRange(strokePoints, index,
							index + 2);
					returnValue = mLib.processHandwrite(tmp);
					if (returnValue != 0) {
						sendErrorMsg(gPenLibHandwrite.AHR_RECOG_ERROR);
						return returnValue;
					}
					index += 2;
				}
				ArrayList<String> tmpResult = mLib.getAllShowResult();
				endTime = System.currentTimeMillis();
				if (tmpResult != null) {
					candidateResults.addAll(tmpResult);
				}

				afterIRecognizeNativeHeapSize = Debug.getNativeHeapSize();
				afterIRecognizeNativeHeapAllocatedSize = Debug
						.getNativeHeapAllocatedSize();
				afterIRecognizeNativeHeapFreeSize = Debug
						.getNativeHeapFreeSize();

				eachTime = endTime - startTime;

				// shenhaifeng
				System.out.println(itemId + ":" + eachTime);

				totalTime += eachTime;
				if (eachTime > maxTime) {
					maxTime = eachTime;
				}

				if (eachTime < minTime) {
					minTime = eachTime;
				}

				successAmount++;

				candidateAmount = candidateResults.size();
				returnValue = isScutCorrect(candidateResults, standardResult);
				switch (returnValue) {
				case 1:
					firstAmount++;
					break;
				case 2:
				case 3:
				case 4:
				case 5:
					fifthAmount++;
					break;
				case 6:
				case 7:
				case 8:
				case 9:
				case 10:
					otherAmount++;
				case -1:
				case 0:
				default:
					break;
				}

				if (candidateAmount > 0) {
					// write each result
					mBuilder.append(itemId).append("\t").append(standardResult)
							.append("\t").append(eachTime).append("\t")
							.append(returnValue).append("\t");

					for (int i = 0; i < candidateAmount; i++) {
						if (i == candidateAmount - 1) {
							mBuilder.append(candidateResults.get(i))
									.append("\t")
									.append("\tbeforeIRecognizeNativeHeapSize:")
									.append(beforeIRecognizeNativeHeapSize)
									.append("bytes, beforeIRecognizeNativeHeapAllocatedSize:")
									.append(beforeIRecognizeNativeHeapAllocatedSize)
									.append("bytes, beforeIRecognizeNativeHeapFreeSize:")
									.append(beforeIRecognizeNativeHeapFreeSize)
									.append("bytes, afterIRecognizeNativeHeapSize:")
									.append(afterIRecognizeNativeHeapSize)
									.append("bytes, afterIRecognizeNativeHeapAllocatedSize:")
									.append(afterIRecognizeNativeHeapAllocatedSize)
									.append("bytes, afterIRecognizeNativeHeapFreeSize:")
									.append(afterIRecognizeNativeHeapFreeSize)
									.append("bytes.\n");
						} else {
							mBuilder.append(candidateResults.get(i))
									.append("\t").append("\t");
						}
					}
				} else {
					// write each result
					mBuilder.append(itemId).append("\t").append(standardResult)
							.append("\t").append(eachTime).append("\t")
							.append(0).append("\t\n");
				}

				mBufferedWriter.write(mBuilder.toString());
				mBufferedWriter.flush();

				// reset mBuilder
				mBuilder.delete(0, mBuilder.length());
				if (successAmount % 100 == 0) {
					scutMessage = mHandler.obtainMessage(MSG_SUCCESSFUL);
					noticeMessage = "华南理工大学手写" + getModeName() + "准确率测试，已成功处理" + successAmount
							+ "条！";
					scutMessage.obj = noticeMessage;
					scutMessage.sendToTarget();
				}
			}

			// write final logs
			totalAmount = failureAmount + successAmount;
			if (successAmount > 0) {
				firstRatio = ((float) firstAmount / successAmount) * 100;
				fifthRatio = ((float) (firstAmount + fifthAmount) / successAmount) * 100;
				wholeRatio = ((float) (firstAmount + fifthAmount + otherAmount) / successAmount) * 100;
				averageTime = totalTime / successAmount;
			}

			testEndTime = System.currentTimeMillis();

			noticeMessage = "华南理工大学测试结果：共计" + totalAmount + "条， 成功："
					+ successAmount + "条，失败：" + failureAmount + "条，首选正确率："
					+ firstRatio + "%，前五候选正确率：" + fifthRatio + "%，总体正确率："
					+ wholeRatio + "%，总识别耗时：" + totalTime + "ms，平均单条识别耗时："
					+ averageTime + "ms，最大识别耗时：" + maxTime + "ms，最小识别耗时："
					+ minTime + "ms，该阶段测试总耗时："
					+ ((float) (testEndTime - testStartTime) / 1000) + "s.\n";
			mBufferedWriter.write(noticeMessage);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			returnValue = -3;
		} catch (NumberFormatException e2) {
			// TODO: handle exception
			// just for debug!!!
			// Log.e("NumberFormatException", "itemId:[" + itemId +
			// "], result:["
			// + splitArray[2] + "].");
			e2.printStackTrace();
			returnValue = -4;
		}

		beforeReleaseClassifierNativeHeapSize = Debug.getNativeHeapSize();
		beforeReleaseClassifierHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		beforeReleaseClassifierNativeHeapFreeSize = Debug
				.getNativeHeapFreeSize();

		// destroy recognition engine
		mLib.iReleaseClassifier();
		mLib = null;

		Debug.stopAllocCounting();
		globalAllocSize = Debug.getGlobalAllocSize();
		globalFreedSize = Debug.getGlobalFreedSize();
		Debug.resetGlobalAllocSize();
		Debug.resetGlobalFreedSize();
		Debug.resetAllCounts();

		afterReleaseClassifierNativeHeapSize = Debug.getNativeHeapSize();
		afterReleaseClassifierHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		afterReleaseClassifierNativeHeapFreeSize = Debug
				.getNativeHeapFreeSize();
		memoryUsageInfo = "beforeReleaseClassifierNativeHeapSize:"
				+ beforeReleaseClassifierNativeHeapSize
				+ "bytes, beforeReleaseClassifierHeapAllocatedSize:"
				+ beforeReleaseClassifierHeapAllocatedSize
				+ "bytes, beforeReleaseClassifierNativeHeapFreeSize:"
				+ beforeReleaseClassifierNativeHeapFreeSize
				+ "bytes, afterReleaseClassifierNativeHeapSize:"
				+ afterReleaseClassifierNativeHeapSize
				+ "bytes, afterReleaseClassifierHeapAllocatedSize:"
				+ afterReleaseClassifierHeapAllocatedSize
				+ "bytes, afterReleaseClassifierNativeHeapFreeSize:"
				+ afterReleaseClassifierNativeHeapFreeSize
				+ "bytes, globalAllocSize:" + globalAllocSize
				+ "bytes, globalFreedSize:" + globalFreedSize + "bytes.\n";

		try {
			mBufferedWriter.write(memoryUsageInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// close input and output file
		closeInputFile();
		closeOutputFile();

		scutMessage = mHandler.obtainMessage(MSG_END);
		noticeMessage = "华南理工大学测试，已完成！结果如下：\n共计" + totalAmount + "条， 成功："
				+ successAmount + "条，失败：" + failureAmount + "条，\n首选正确率："
				+ firstRatio + "%，前五候选正确率：" + fifthRatio + "%，总体正确率："
				+ wholeRatio + "%，\n总识别耗时：" + totalTime + "ms，平均单条识别耗时："
				+ averageTime + "ms，\n最大识别耗时：" + maxTime + "ms，最小识别耗时："
				+ minTime + "ms，该阶段测试总耗时："
				+ ((float) (testEndTime - testStartTime) / 1000) + "s.";
		scutMessage.obj = noticeMessage;
		scutMessage.sendToTarget();

		return returnValue;
	}

	private int isHanwangCorrect(List<String> candidateResults,
			char[] standardResult) {
		if (candidateResults == null || candidateResults.size() == 0
				|| standardResult == null || standardResult.length == 0) {
			return -1;
		}

		int i = 0;
		int returnValue = 0;
		char[] candidateArray = null;
		int candidateNo = 0;
		for (String eachResult : candidateResults) {
			candidateNo++;
			if (eachResult.length() != standardResult.length) {
				continue;
			}

			candidateArray = eachResult.toCharArray();
			for (i = 0; i < candidateArray.length; i++) {
				if (halfCharToFullChar(candidateArray[i]) != halfCharToFullChar(standardResult[i])) {
					break;
				}
			}

			if (i == candidateArray.length) {
				returnValue = candidateNo;
				break;
			}
		}

		return returnValue;
	}

	private int handleHanwangAccuracyJudge() {
		long testStartTime = 0;
		long testEndTime = 0;
		testStartTime = System.currentTimeMillis();
		int returnValue = 0;
		int successAmount = 0;
		int failureAmount = 0;
		int totalAmount = 0;
		int firstAmount = 0;
		int fifthAmount = 0;
		int otherAmount = 0;
		long startTime = 0;
		long endTime = 0;
		long totalTime = 0;
		long eachTime = 0;
		long maxTime = 0;
		long minTime = Integer.MAX_VALUE;
		long averageTime = 0;
		float firstRatio = 0;
		float fifthRatio = 0;
		float wholeRatio = 0;

		// add memory usage statistics
		long beforeLoadClassifierNativeHeapSize = 0;
		long beforeLoadClassifierNativeHeapAllocatedSize = 0;
		long beforeLoadClassifierNativeHeapFreeSize = 0;
		long afterLoadClassifierNativeHeapSize = 0;
		long afterLoadClassifierNativeHeapAllocatedSize = 0;
		long afterLoadClassifierNativeHeapFreeSize = 0;
		long beforeIRecognizeNativeHeapSize = 0;
		long beforeIRecognizeNativeHeapAllocatedSize = 0;
		long beforeIRecognizeNativeHeapFreeSize = 0;
		long afterIRecognizeNativeHeapSize = 0;
		long afterIRecognizeNativeHeapAllocatedSize = 0;
		long afterIRecognizeNativeHeapFreeSize = 0;
		long beforeReleaseClassifierNativeHeapSize = 0;
		long beforeReleaseClassifierHeapAllocatedSize = 0;
		long beforeReleaseClassifierNativeHeapFreeSize = 0;
		long afterReleaseClassifierNativeHeapSize = 0;
		long afterReleaseClassifierHeapAllocatedSize = 0;
		long afterReleaseClassifierNativeHeapFreeSize = 0;
		long globalAllocSize = 0;
		long globalFreedSize = 0;
		String memoryUsageInfo = null;

		HanwangInterface mInterface = null;
		String eachLine = null;
		String[] splitArray = null;
		String[] pointsArray = null;
		String[] viceArray = null;
		String itemId = null;
		String noticeMessage = null;
		char[] standardResult = null;
		int candidateAmount = 0;
		short[] strokePoints = null;
		// char[] candidateResults = new char[64];
		// int defaultScore = 0;
		List<String> candidateResults = new ArrayList<String>();

		StringBuilder mBuilder = new StringBuilder();

		Message hanwangMessage = mHandler.obtainMessage(MSG_START);
		noticeMessage = "汉王手写" + getModeName() + "准确率测试，已开始！";
		hanwangMessage.obj = noticeMessage;
		hanwangMessage.sendToTarget();

		// open input file
		returnValue = openInputFile(INPUT_FILE_PATH);
		if (returnValue != 0) {
			hanwangMessage = mHandler.obtainMessage(MSG_FAILED);
			noticeMessage = "汉王手写" + getModeName() + "准确率测试，打开输入文件失败！";
			hanwangMessage.obj = noticeMessage;
			hanwangMessage.sendToTarget();
			return returnValue;
		}

		// open output file
		returnValue = openOutputFile(OUTPUT_DIR, OUTPUT_FILE_HANWANG);
		if (returnValue != 0) {
			hanwangMessage = mHandler.obtainMessage(MSG_FAILED);
			noticeMessage = "汉王手写" + getModeName() + "准确率测试，打开输出文件失败！";
			hanwangMessage.obj = noticeMessage;
			hanwangMessage.sendToTarget();
			return returnValue;
		}

		beforeLoadClassifierNativeHeapSize = Debug.getNativeHeapSize();
		beforeLoadClassifierNativeHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		beforeLoadClassifierNativeHeapFreeSize = Debug.getNativeHeapFreeSize();

		Debug.startAllocCounting();

		// initialize recognition engine
		mInterface = new HanwangInterface();
		returnValue = mInterface.initializeHanwangEngine(
				AccuracyJudgeActivity.this,
				mReconginazeModeHW, Recognizer.Range_All);
		if (returnValue < 0) {
			hanwangMessage = mHandler.obtainMessage(MSG_FAILED);
			noticeMessage = "汉王手写" + getModeName() + "准确率测试，初始化识别引擎失败！";
			hanwangMessage.obj = noticeMessage;
			hanwangMessage.sendToTarget();
			return returnValue;
		}

		Log.e("AccuracyJudge", ":汉王初始化识别引擎成功！");
		afterLoadClassifierNativeHeapSize = Debug.getNativeHeapSize();
		afterLoadClassifierNativeHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		afterLoadClassifierNativeHeapFreeSize = Debug.getNativeHeapFreeSize();
		memoryUsageInfo = "beforeLoadClassifierNativeHeapSize:"
				+ beforeLoadClassifierNativeHeapSize
				+ "bytes, beforeLoadClassifierNativeHeapAllocatedSize:"
				+ beforeLoadClassifierNativeHeapAllocatedSize
				+ "bytes, beforeLoadClassifierNativeHeapFreeSize:"
				+ beforeLoadClassifierNativeHeapFreeSize
				+ "bytes, afterLoadClassifierNativeHeapSize:"
				+ afterLoadClassifierNativeHeapSize
				+ "bytes, afterLoadClassifierNativeHeapAllocatedSize:"
				+ afterLoadClassifierNativeHeapAllocatedSize
				+ "bytes, afterLoadClassifierNativeHeapFreeSize:"
				+ afterLoadClassifierNativeHeapFreeSize + "bytes.\n";

		try {
			mBufferedWriter.write(memoryUsageInfo);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			while ((eachLine = mBufferedReader.readLine()) != null
					&& isEnded == false) {
				candidateResults.clear();
				Log.e("candidateResults.size=", candidateResults.size()+"");

				splitArray = eachLine.split("(=)|(;)");// TODO:regex
				if (splitArray == null || splitArray.length != 3) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				itemId = splitArray[0];
				if (splitArray[2].length() < 6) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				viceArray = splitArray[2].split(",");
				if (viceArray == null || viceArray.length == 0) {
					failureAmount++;
					continue;
				}

				standardResult = new char[viceArray.length];
				for (int i = 0; i < standardResult.length; i++) {
					standardResult[i] = (char) Integer.parseInt(
							viceArray[i].replace("\\u", ""), 16);
				}

				pointsArray = splitArray[1].split(",");
				if (pointsArray == null || pointsArray.length == 0
						|| pointsArray.length % 2 != 0) {
					failureAmount++;
					// TODO:write warning logs
					continue;
				}

				// construct points
				strokePoints = new short[pointsArray.length];
				for (int i = 0; i < pointsArray.length; i++) {
					strokePoints[i] = Short.valueOf(pointsArray[i]);
				}

				beforeIRecognizeNativeHeapSize = Debug.getNativeHeapSize();
				beforeIRecognizeNativeHeapAllocatedSize = Debug
						.getNativeHeapAllocatedSize();
				beforeIRecognizeNativeHeapFreeSize = Debug
						.getNativeHeapFreeSize();

				// get recognition results without gesture!!!
				startTime = System.currentTimeMillis();

				candidateAmount = mInterface.getOverlayRecognition(
						strokePoints, candidateResults);
				Log.e("candidateAmount=", candidateAmount+"");
				endTime = System.currentTimeMillis();

				afterIRecognizeNativeHeapSize = Debug.getNativeHeapSize();
				afterIRecognizeNativeHeapAllocatedSize = Debug
						.getNativeHeapAllocatedSize();
				afterIRecognizeNativeHeapFreeSize = Debug
						.getNativeHeapFreeSize();

				eachTime = endTime - startTime;
				totalTime += eachTime;
				if (eachTime > maxTime) {
					maxTime = eachTime;
				}

				if (eachTime < minTime) {
					minTime = eachTime;
				}

				successAmount++;

				returnValue = isHanwangCorrect(candidateResults, standardResult);
				switch (returnValue) {
				case 1:
					firstAmount++;
					break;
				case 2:
				case 3:
				case 4:
				case 5:
					fifthAmount++;
					break;
				case 6:
				case 7:
				case 8:
				case 9:
				case 10:
					otherAmount++;
				case -1:
				case 0:
				default:
					break;
				}

				// write each result
				mBuilder.append(itemId).append("\t").append(standardResult)
						.append("\t").append(eachTime).append("\t")
						.append(returnValue).append("\t");
				for (int i = 0; i < candidateAmount; i++) {
					if (i == candidateAmount - 1) {
						Log.e("AccuracyJudgeActivity:", "handleHanwangAccuracyJudge:candidateResults["+i+"]="+candidateResults.get(i));
						mBuilder.append(candidateResults.get(i))
								.append("\t")
								.append("\tbeforeIRecognizeNativeHeapSize:")
								.append(beforeIRecognizeNativeHeapSize)
								.append("bytes, beforeIRecognizeNativeHeapAllocatedSize:")
								.append(beforeIRecognizeNativeHeapAllocatedSize)
								.append("bytes, beforeIRecognizeNativeHeapFreeSize:")
								.append(beforeIRecognizeNativeHeapFreeSize)
								.append("bytes, afterIRecognizeNativeHeapSize:")
								.append(afterIRecognizeNativeHeapSize)
								.append("bytes, afterIRecognizeNativeHeapAllocatedSize:")
								.append(afterIRecognizeNativeHeapAllocatedSize)
								.append("bytes, afterIRecognizeNativeHeapFreeSize:")
								.append(afterIRecognizeNativeHeapFreeSize)
								.append("bytes.\n");
					} else {
						mBuilder.append(candidateResults.get(i)).append("\t")
								.append("\t");
					}
				}

				mBufferedWriter.write(mBuilder.toString());

				// reset mBuilder
				mBuilder.delete(0, mBuilder.length());
				if (successAmount % 100 == 0) {
					hanwangMessage = mHandler.obtainMessage(MSG_SUCCESSFUL);
					noticeMessage = "汉王手写" + getModeName() + "准确率测试，已成功处理" + successAmount + "条！";
					hanwangMessage.obj = noticeMessage;
					hanwangMessage.sendToTarget();
				}
			}

			// write final logs
			totalAmount = failureAmount + successAmount;
			if (successAmount > 0) {
				firstRatio = ((float) firstAmount / successAmount) * 100;
				fifthRatio = ((float) (firstAmount + fifthAmount) / successAmount) * 100;
				wholeRatio = ((float) (firstAmount + fifthAmount + otherAmount) / successAmount) * 100;
				averageTime = totalTime / successAmount;
			}

			testEndTime = System.currentTimeMillis();

			noticeMessage = "汉王测试结果：共计" + totalAmount + "条， 成功："
					+ successAmount + "条，失败：" + failureAmount + "条，首选正确率："
					+ firstRatio + "%，前五候选正确率：" + fifthRatio + "%，总体正确率："
					+ wholeRatio + "%，总识别耗时：" + totalTime + "ms，平均单条识别耗时："
					+ averageTime + "ms，最大识别耗时：" + maxTime + "ms，最小识别耗时："
					+ minTime + "ms，该阶段测试总耗时："
					+ ((float) (testEndTime - testStartTime) / 1000) + "s.\n";
			mBufferedWriter.write(noticeMessage);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			returnValue = -3;
		} catch (NumberFormatException e2) {
			// TODO: handle exception
			e2.printStackTrace();
			returnValue = -4;
		}

		beforeReleaseClassifierNativeHeapSize = Debug.getNativeHeapSize();
		beforeReleaseClassifierHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		beforeReleaseClassifierNativeHeapFreeSize = Debug
				.getNativeHeapFreeSize();

		// destroy recognition engine
		mInterface.destroyHanwangEngine();
		mInterface = null;

		Debug.stopAllocCounting();
		globalAllocSize = Debug.getGlobalAllocSize();
		globalFreedSize = Debug.getGlobalFreedSize();
		Debug.resetGlobalAllocSize();
		Debug.resetGlobalFreedSize();
		Debug.resetAllCounts();

		afterReleaseClassifierNativeHeapSize = Debug.getNativeHeapSize();
		afterReleaseClassifierHeapAllocatedSize = Debug
				.getNativeHeapAllocatedSize();
		afterReleaseClassifierNativeHeapFreeSize = Debug
				.getNativeHeapFreeSize();
		memoryUsageInfo = "beforeReleaseClassifierNativeHeapSize:"
				+ beforeReleaseClassifierNativeHeapSize
				+ "bytes, beforeReleaseClassifierHeapAllocatedSize:"
				+ beforeReleaseClassifierHeapAllocatedSize
				+ "bytes, beforeReleaseClassifierNativeHeapFreeSize:"
				+ beforeReleaseClassifierNativeHeapFreeSize
				+ "bytes, afterReleaseClassifierNativeHeapSize:"
				+ afterReleaseClassifierNativeHeapSize
				+ "bytes, afterReleaseClassifierHeapAllocatedSize:"
				+ afterReleaseClassifierHeapAllocatedSize
				+ "bytes, afterReleaseClassifierNativeHeapFreeSize:"
				+ afterReleaseClassifierNativeHeapFreeSize
				+ "bytes, globalAllocSize:" + globalAllocSize
				+ "bytes, globalFreedSize:" + globalFreedSize + "bytes.\n";

		try {
			mBufferedWriter.write(memoryUsageInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// close input and output file
		closeInputFile();
		closeOutputFile();

		hanwangMessage = mHandler.obtainMessage(MSG_END);
		noticeMessage = "汉王测试，已完成！结果如下：\n共计" + totalAmount + "条， 成功："
				+ successAmount + "条，失败：" + failureAmount + "条，\n首选正确率："
				+ firstRatio + "%，前五候选正确率：" + fifthRatio + "%，总体正确率："
				+ wholeRatio + "%，\n总识别耗时：" + totalTime + "ms，平均单条识别耗时："
				+ averageTime + "ms，\n最大识别耗时：" + maxTime + "ms，最小识别耗时："
				+ minTime + "ms，该阶段测试总耗时："
				+ ((float) (testEndTime - testStartTime) / 1000) + "s.";
		hanwangMessage.obj = noticeMessage;
		hanwangMessage.sendToTarget();

		return returnValue;
	}

	class updateUIThread implements Runnable {
		int companyId = 0;// 0 means SCUT, 1 means KPEN, 2 means HW

		public updateUIThread(int companyId) {
			this.companyId = companyId;
		}

		public void run() {
			switch (companyId) {
			case 0:
				handleScutAccuracyJudge();
				break;

			case 1:
				handleHanwangAccuracyJudge();
				break;

			default:
				break;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_change_mode:
			setMode();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setMode() {
		int choicedItemIndex = getModePosition();
		new AlertDialog.Builder(this)
				.setTitle("识别模式")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setSingleChoiceItems(
						new String[] { getString(R.string.mode_single_word),
								getString(R.string.mode_overlay),
								getString(R.string.mode_raw) },
						choicedItemIndex,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								changeMode(which);
								dialog.dismiss();
							}

						}).setNegativeButton("取消", null).show();
	}

	private int getModePosition() {
		switch (mReconginazeMode) {
		case gPenLibHandwrite.APPROACH_NORMAL:
			return 0;

		case gPenLibHandwrite.APPROACH_OVERLAP:
			return 1;

		case gPenLibHandwrite.APPROACH_ROW:
			return 2;
		default:
			break;
		}

		return 1;
	}

	private void changeMode(int which) {
		Log.i(TAG, "current mode is:" + which);
		switch (which) {
		case 0:
			mReconginazeMode = gPenLibHandwrite.APPROACH_NORMAL;
			mReconginazeModeHW = Recognizer.Mode_CHS_SingleChar;
			break;

		case 1:
			mReconginazeMode = gPenLibHandwrite.APPROACH_OVERLAP;
			mReconginazeModeHW = Recognizer.Mode_CHS_Sentence_Overlap;
			break;

		case 2:
			mReconginazeMode = gPenLibHandwrite.APPROACH_ROW;
			mReconginazeModeHW = Recognizer.Mode_CHS_Sentence;
			break;

		default:
			break;
		}
	}
	
	private String getModeName() {
		if (mReconginazeMode == gPenLibHandwrite.APPROACH_NORMAL) {
			return "单字";
		} else if (mReconginazeMode == gPenLibHandwrite.APPROACH_OVERLAP) {
			return "叠字";
		} else if (mReconginazeMode == gPenLibHandwrite.APPROACH_ROW) {
			return "短句";
		} else {
			return "";
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.overlay_handwrite, menu);
		return true;
	}

	public void onDestroy() {
		super.onDestroy();
		isEnded = true;
	}
}

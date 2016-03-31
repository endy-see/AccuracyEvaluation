/*********************************************************************************************
 * Project Name: SCUT gPen Overlap Demo
 * Copyright (C), 2015~, SCUT HCII-Lab (http://www.hcii-lab.net/gpen)
 * ANY INDIVIDUAL OR LEGAL ENTITY MAY NOT USE THIS FILE WITHOUT THE AUTHORIZATION BY HCII-LAB.
 * 
 *      Model name: FileHelper
 *          Author: Liquan Qiu
 *         Version: 1.0
 *         Created: 20150621
 *
 *        Function: Native API of the C++ classify engine.   
 *                  
 *     
 *********************************************************************************************/

package net.hciilab.recognization.lib;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.util.Log;

public class gPenLibHandwrite {

	private final static String TAG = "HANDWRITE";
	private static final boolean DEBUG = true;

	// ------------------------识别方法---------------------------------------------
	// Recognize in any rotation
	public final static int APPROACH_NORMAL = 1;
	public final static int APPROACH_ROTATION = 2;
	// 重叠手写模式
	public final static int APPROACH_OVERLAP = 3;
	
	public final static int APPROACH_ROW = 4;
	// ------------------------------------------------------------------------------------

	public static final int GPEN_VER_TRANDITIONAL = 1; // output Chinese
														// Traditional
	public static final int GPEN_VER_SIMPLIFIED = 2; // output Chinese
														// Simplified
	public static final int GPEN_VER_MIX = 3; // output Chinese Traditional and
												// Simplified
	
	//----------------------------错误码--------------------------------------
	public final static int AHR_INIT_ERROR = -1;
	public final static int AHR_CONF_ERROR = -2;
	public final static int AHR_RESET_ERROR = -3;
	public final static int AHR_SPLIT_ERROR = -4;
	public final static int AHR_RECOG_ERROR = -5;

	// ------------------对外接口,尽可能兼容单字gPenLib------------------------------------------------------------
	private static gPenLibHandwrite instance;

	private gPenLibHandwrite() {
	}

	/**
	 * 单例模式设计.
	 */
	public static gPenLibHandwrite getInstance() {
		if (instance == null)
			instance = new gPenLibHandwrite();
		return instance;
	}

	private static boolean bInit = false;

	/**
	 * 初始化分类器
	 * 
	 * @return 0：如果成功； 其它：如果失败。
	 */
	public int iSetClassifier(String soName, String languageFileName) {
		if (bInit) {
			return 0;
		}

		if (soName == null || languageFileName == null || soName.length() == 0
				|| languageFileName.length() == 0) {
			return -1;
		}

		bInit = true;

		return libInit(soName, languageFileName);
	}

	/**
	 * 销毁分类器，释放内存
	 * 
	 * @return 0：如果成功； 其它：如果失败。
	 */
	public int iReleaseClassifier() {
		bInit = false;
		return libDestroy();
	}

	/**
	 * 设置汉字识别的语言
	 * 
	 * @param ver
	 *            1表示繁体字，2表示简体字，3表示前两者混合
	 * @return 0:如果成功
	 */
	public int iSetVersion(int ver) {
		return libSetLangeVersion(ver);
	}
	
	/**
	 * 设置识别速度
	 * 
	 * @param speed 范围 1-99
	 * @return
	 */
	public int iSetRecogSpeed(int speed) {
		if(speed < 1 || speed > 99) {
			return -1;
		}
		return libSetRecogSpeed(speed);
	}

	/**
	 * 重置状态，重新进行输入
	 * 
	 * @return 0:如果成功
	 */
	public int iClear() {
		return libClear();
	}
	
	public int iReset() {
		return libReset();
	}

	// --------------识别结果类型,大于0表示结果------------------------------------------------
	/** 当前没有结果 */
	public final static int RESULT_NONE = 0;
	/** 结果为手势 */
	public final static int RESULT_GESTURE = -1;
	/** 结果为单字 */
	public final static int RESULT_SINGLE_WORD = -2;
	/** 错误 */
	public final static int RESULT_ERROR = -3;

	// ----------------------------------------------------------------------
//	/**
//	 * 处理所写字符，需要根据返回结果进行处理.
//	 * 
//	 * @param ptBuf
//	 * @param ptNum
//	 * @param targetType
//	 *            识别目标集
//	 * @param handwriteMode
//	 *            3表示重叠手写模式；1表示单字手写模式
//	 * @return
//	 */
//	public int processHandwrite(int ptBuf[], int ptNum, int targetType,
//			int handwriteMode) {
//		int ret = libWordRecognize(ptBuf, ptNum, targetType, handwriteMode);
//		return ret;
//	}

	/**
	 * 处理所写字符，需要根据返回结果进行处理.
	 * 
	 * @param ptBuf
	 * @return
	 */
	public int processHandwrite(int ptBuf[]) {
		if(ptBuf == null || ptBuf.length < 0)
			return -4;//参数错误
		
		int ret = libRealRecognize(ptBuf);
		return ret;
	}
	
	public int setTargetAndMode(int targetType, int handwriteMode) {
		return libConfigure(targetType, handwriteMode);
	}
	
	/**
	 * 获取所有的
	 * 
	 * @return
	 */
	public byte[] getAllOriginalResult() {
		return libGetAllRegResult();
	}

	/**
	 * 获取所有的识别候选项
	 * 
	 * @return
	 */
	public ArrayList<String> getAllShowResult() {
		byte[] allResult = libGetAllRegResult();
		if (allResult != null && allResult.length > 0) {
			ArrayList<String> resultList = new ArrayList<String>();

			for (int index = 0; index < allResult.length;) {

				int length = allResult[index++];

				if (length > 0) {
					byte[] result = new byte[length + 2];
					result[0] = -1;
					result[1] = -2;
					System.arraycopy(allResult, index, result, 2, length);
					try {
						String resultStr = new String(result, "unicode");
						resultList.add(resultStr);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
					}
					
					index += length;
				} else {
					break;
				}

			}
			return resultList;
		}
		return null;
	}

	/** 重新设计格式2.0，长度(限定在127以内),长度/类型+内容(每一字都是都是两个字符) */
	/** 类型说明 */
	/** 手写类型 */
	private static byte RESULT_GES = (byte) 255;
	/** 半角符号开头以1填充，因为我们的unicode码码集不会有1开头的，兼容 */
	private static byte RESULT_DC = (byte) 1;
	/** 结束符 ,长度为0 */
	private static byte RESULT_END = '\0';

//	private native int libWordRecognize(int ptBuf[], int ptNum, int targetType,
//			int handwriteMode);
	
	private native int libRealRecognize(int ptBuf[]);
	
	private native int libConfigure(int targetType, int handwriteMode);

	private native byte[] libGetAllRegResult();

	private native int libSetLangeVersion(int ver);

	private native int libInit(String soName, String languageFileName);

	private native int libDestroy();

	private native int libClear();
	
	private native int libReset();
	
	private native int libSetRecogSpeed(int speed);

	// load the lib
	static {
		System.loadLibrary("gpen_handwriter");
	}

	private void printf(String str) {
		if (DEBUG) {
			Log.d(TAG, str);
		}
	}
	
	// ------------------------------------------------------------------------------------

}

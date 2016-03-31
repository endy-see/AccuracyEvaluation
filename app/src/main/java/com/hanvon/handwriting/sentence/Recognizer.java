package com.hanvon.handwriting.sentence;

import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.util.Log;

public class Recognizer {
	// rec mode
	public static final int Mode_CHS_SingleChar = 1;
	public static final int Mode_CHS_Sentence = 2;
	public static final int Mode_ENG_WORD = 3;
	public static final int Mode_CHS_Sentence_Overlap = 4;
	public static final int Mode_CHS_ENG_AUTO = 5;

	// rec range
	public static final int Range_CHS = 1;// chinese only
	public static final int Range_Eng = 2;// english only
	public static final int Range_Num = 4;// number only
	public static final int Range_Punc = 8;// punctuation only
	public static final int Range_All = 15;// all

	// rec type
	public static final int RecType_Total = 1;// rec total at once
	public static final int RecType_Inc = 2;// rec the increased stokes only

	private long handle_rec;

	public long getHandle_rec() {
		return handle_rec;
	}

	private int mode;

	/**
	 * ����ʶ�����
	 * 
	 * @param mode
	 *            ʶ��ģʽ 1 ���ĵ��֣� 2 ���Ķ̾䣻 3 Ӣ�ĵ��ʣ� 4 ���Ķ̾��д�� 5��Ӣ�Զ��ж�
	 * @return ʶ�����
	 */
	public static Recognizer Create(int mode) {
		Recognizer rec = new Recognizer();
		rec.handle_rec = nativeCreateRecHandle(mode);
		rec.mode = mode;
		return rec;
	}

	/**
	 * ����ʶ�����
	 * 
	 * @param senRec
	 *            ��Ҫ���ٵĶ���
	 */
	public static void Destroy(Recognizer senRec) {
		nativeReleaseRecHandle(senRec.handle_rec);
		senRec.handle_rec = 0;
	}

	/**
	 * ��ָ����ʽ���ص�дʶ���������ؽ������
	 */
	public int getOverlayRecognition(short[] inputPoints,
			List<String> candidateResults) {
		if (inputPoints == null || candidateResults == null
				|| inputPoints.length == 0) {
			return -1;
		}

		if (nativeRecognize(handle_rec, inputPoints) != 0) {
			return -1;
		}

		String[] candidateString = nativeGetResult(handle_rec);
		int returnValue = 0;
		int realLength = 0;

		if (candidateString == null) {
			return -1;
		} else if ((returnValue = candidateString.length) == 0) {
			return 0;
		}

		for (int i = 0; i < returnValue; i++) {
			if (!candidateString[i].equals("")) {
				candidateResults.add(candidateString[i]);
				realLength++;
			}
		}

		return realLength;
	}

	/**
	 * ��ָ����ʽ��ȡ����ʶ���������ؽ������
	 */
	public int getSingleRecognition(short[] inputPoints, char[] candidateResults) {
		if (inputPoints == null || inputPoints.length == 0
				|| candidateResults == null || candidateResults.length == 0) {
			return -1;
		}

		if (nativeRecognize(handle_rec, inputPoints) != 0) {
			return -1;
		}

		String[] candidateString = nativeGetResult(handle_rec);
		int returnValue = 0;
		int realLength = 0;
		if (candidateString == null) {
			return -1;
		} else if ((returnValue = candidateString.length) == 0) {
			return 0;
		}

		char[] tempArray = null;
		for (int i = 0; i < returnValue; i++) {
			tempArray = candidateString[i].toCharArray();
			// just for debug start!!!
			// for (int j = 0; j < tempArray.length; j++) {
			// Log.e("RESULT",
			// i + ":" + j + "key[" + Integer.valueOf(tempArray[j])
			// + "], value[" + tempArray[j] + "]");
			// }
			// just for debug end!!!
			if (tempArray[0] != 0) {
				candidateResults[realLength] = tempArray[0];
				realLength++;
			}
		}

		return realLength;
	}

	/**
	 * ��ȡʶ����
	 * 
	 * @param trace
	 *            ��ʶ��ıʼ�
	 * @param nRecType
	 *            ʶ��ʽ 1 ʶ�������ַ��� 2 ʶ������һ���ַ���������ַ����ŵ�����Ϊ���һ���ֻ�����д��
	 *            �������ʶ����û������ģ����������ʶ���ܼ������㣬���ʶ��Ч�ʡ�ȱ�������һ���ַ�����ʶ���ʶ����ȷ��
	 * @return ʶ����
	 */
	public RecResult recognize(TraceData trace, int nRecType) {

		if (handle_rec == 0)
			return null;
		synchronized (this) {
			short[] pts = null;
			switch (mode) {
			case Mode_CHS_SingleChar:
			// case Mode_ENG_WORD:
			{
				pts = trace.GetTraceDataPts(true);
			}
				break;
			case Mode_CHS_Sentence:
			case Mode_CHS_Sentence_Overlap:
			// case Mode_CHS_ENG_AUTO:
			{
				pts = trace.GetTraceDataPts(nRecType == RecType_Total);

			}
				break;
			}

			if (nativeRecognize(handle_rec, pts) != 0)
				return null;
			RecResult recResult = new RecResult();
			recResult.setCandStr(nativeGetResult(handle_rec));
			// recResult.setCandLang(nativeGetResultLang(handle_rec));
			// if(mode!=Mode_CHS_SingleChar)
			// recResult.setCandSeg(nativeGetResultSeg(handle_rec));
			return recResult;
		}
	}

	/**
	 * ����ʶ��Χ
	 * 
	 * @param range
	 *            ʶ��Χ 1 ���ģ� 2 Ӣ�ģ� 3 ���֣� 4 ��㣻 5ȫ��
	 * @return ״ֵ̬
	 */
	public int setRecogRange(int range) {
		if (handle_rec == 0)
			return 0;
		synchronized (this) {
			int nRet = nativeSetRecogRange(handle_rec, range);
			return nRet;
		}
	}

	/**
	 * ���ʶ��ģʽ
	 * 
	 * @return ʶ��ģʽ
	 */
	public int getRecogMode() {
		if (handle_rec == 0)
			return 0;
		synchronized (this) {
			int nRet = nativeGetRecogMode(handle_rec);
			return nRet;
		}
	}

	/**
	 * ����ʶ��ģʽ
	 * 
	 * @param mode
	 *            ʶ��ģʽ
	 * @return ״ֵ̬
	 */
	public int setRecogMode(int mode) {
		if (handle_rec == 0)
			return 0;
		synchronized (this) {
			int oldMode = getRecogMode();
			nativeReleaseRecHandle(handle_rec);
			handle_rec = nativeCreateRecHandle(mode);
			this.mode = mode;
			return oldMode;
		}
	}

	/**
	 * ����ϵͳ�ֵ�
	 * 
	 * @param dicfn
	 *            �ֵ�·��
	 * @return
	 */
	public static boolean SetRecDic(String dicfn) {
		boolean nRet = nativeSetRecogDic(dicfn);
		return nRet;
	}

	// ���ط���
	private static native boolean nativeSetRecogDic(String fn);

	private static native long nativeCreateRecHandle(int mode);

	private static native void nativeReleaseRecHandle(long handle);

	private static native int nativeRecognize(long handle, short[] nPoint);

	private static native String[] nativeGetResult(long handle);

	 private static native String[] nativeGetResultLang(long handle);
	 private static native ArrayList<short[]> nativeGetResultSeg( long
	 handle);
	private static native int nativeSetRecogRange(long handle, int range_value);

	private static native int nativeGetRecogMode(long handle);

	static {
		System.loadLibrary("HwSentenceRec");
	}

}

/*********************************************************************************************
 * Project Name: SCUT gPen Overlap Demo
 * Copyright (C), 2015~, SCUT HCII-Lab (http://www.hcii-lab.net/gpen)
 * ANY INDIVIDUAL OR LEGAL ENTITY MAY NOT USE THIS FILE WITHOUT THE AUTHORIZATION BY HCII-LAB.
 * 
 *      Model name: gPenTargetType
 *          Author: Liquan Qiu
 *         Version: 1.0
 *         Created: 20150621
 *
 *        Function: 
 *                  
 *     
 *********************************************************************************************/

package net.hciilab.recognization.lib;

public class gPenTargetType {
	// ----Target
	// ranges--------------------------------------------------------------------
	public static final int GPEN_TR_UPPERCASE_SC = 0x00000001; // 全角大写英文字母
	public static final int GPEN_TR_UPPERCASE_DC = 0x00000002; // 半角大写英文字母
	public static final int GPEN_TR_LOWERCASE_SC = 0x00000004; // 全角小写英文字母
	public static final int GPEN_TR_LOWERCASE_DC = 0x00000008; // 半角小写英文字母
	public static final int GPEN_TR_NUMBER_SC = 0x00000010; // 全角数字
	public static final int GPEN_TR_NUMBER_DC = 0x00000020; // 半角数字
	public static final int GPEN_TR_PUNCTUATION_SC = 0x00000040; // 全角标点符号
	public static final int GPEN_TR_PUNCTUATION_DC = 0x00000080; // 半角标点符号
	public static final int GPEN_TR_GESTURE = 0x00000100; // 手势
	public static final int GPEN_TR_SPECIAL_CHAR = 0x00000200; // special
																// characters
	public static final int GPEN_TR_CHINESE_CHAR = 0x00000400; // Chinese
																// characters(Simplified
																// and
																// Traditional)

	public static final int GPEN_OVERLAP = 0x00100000; // 重叠手写模式
	public static final int GPEN_TEXTLINE = 0x00200000; // 文本行模式（不支持）
	public static final int GPEN_FREE = 0x00400000; // 自由书写模式（不支持）

	public static final int GPEN_TR_SPECIAL_DC = GPEN_TR_NUMBER_DC
			| GPEN_TR_CHINESE_CHAR | GPEN_TR_UPPERCASE_DC
			| GPEN_TR_LOWERCASE_DC | GPEN_TR_PUNCTUATION_DC;

	public static final int GPEN_TR_ALLDC = GPEN_TR_NUMBER_DC
			| GPEN_TR_CHINESE_CHAR | GPEN_TR_GESTURE | GPEN_TR_UPPERCASE_DC
			| GPEN_TR_LOWERCASE_DC | GPEN_TR_PUNCTUATION_DC;

	public static final int GPEN_TR_ALLSC = GPEN_TR_NUMBER_SC
			| GPEN_TR_CHINESE_CHAR | GPEN_TR_GESTURE | GPEN_TR_UPPERCASE_SC
			| GPEN_TR_LOWERCASE_SC | GPEN_TR_PUNCTUATION_SC;

	public static final int GPEN_TR_DEFAULT = GPEN_TR_NUMBER_SC
			| GPEN_TR_CHINESE_CHAR | GPEN_TR_GESTURE;

	public static final int GPEN_TR_OTHER_DC = GPEN_TR_NUMBER_DC
			| GPEN_TR_GESTURE | GPEN_TR_UPPERCASE_DC | GPEN_TR_LOWERCASE_DC
			| GPEN_TR_PUNCTUATION_DC;

	public static final int GPEN_TR_OTHER_SC = GPEN_TR_NUMBER_SC
			| GPEN_TR_GESTURE | GPEN_TR_UPPERCASE_SC | GPEN_TR_LOWERCASE_SC
			| GPEN_TR_PUNCTUATION_SC;
	// ------------------------------------------------------------------------------------
}

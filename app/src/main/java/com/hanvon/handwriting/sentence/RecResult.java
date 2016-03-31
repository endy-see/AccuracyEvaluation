package com.hanvon.handwriting.sentence;

import java.util.ArrayList;

//import android.util.Log;


public class RecResult {
	String[]  strCand = null;
	ArrayList<short[]> SegCand = null;
	String[]  strLang = null;
	
	static private String  nullStrCand="";
	static private short[] nullSegCand=new short[0];
	
	
	public String getCandStr(int idx)
	{
		if(strCand == null||strCand.length==0)
			return nullStrCand;
		return strCand[idx];
	}
	
	
	
	public String getCandLang(int idx)
	{
		if(strCand == null||strLang.length==0)
			return nullStrCand;
		return strLang[idx];
	}
	
	public short[] getCandSeg(int idx)
	{
		if(SegCand == null||SegCand.size()==0)
			return nullSegCand;
		return SegCand.get(idx);
	}
	
	public int    getCandNum()
	{		
		return strCand.length;
	}
	
	void   setCandStr(String[] strResult)
	{
		strCand = strResult;
	}

	void  setCandSeg(ArrayList<short[]> segResult)
	{
		SegCand = segResult;
	}
	void setCandLang(String[] Lang)
	{
		strLang = Lang;
	}
}

package com.hanvon.handwriting.sentence;

import java.util.ArrayList;

import android.util.Log;

public class TraceData {
	static ArrayList<TraceStroke> strokes = null;
	
	public TraceData()
	{
		strokes = new ArrayList<TraceStroke>();
	}
	public void addStrok(TraceStroke stroke)
	{
		strokes.add(stroke);
	}
	public int getStrokeCount()
	{
		return strokes.size();
	}
	public TraceStroke getStrokeAt(int idx)
	{
		return strokes.get(idx);
	}

	public static void clear()
	{
		strokes.clear();
	}
	public boolean isEmpty()
	{
		return strokes.isEmpty();
	}
	//1   add end flag
	//2   not add 
	public	short[] GetTraceDataPts(boolean bAddEndFlag)
	{
		int sz = strokes.size();
		int nTotal = 0;
		for(int i=0;i<sz;i++)
		{
			nTotal+=strokes.get(i).getStrokeDataCount();
		}
		if(bAddEndFlag)
			nTotal+=2;
		short[] ret = new short[nTotal];
		int offset = 0;
		for(int i=0;i<sz;i++)
		{
			short[] tmp = strokes.get(i).getStrokeData();
			if(tmp == null){
				//Log.i("wwl", "TraceData__ tmp==null");
				break;
			}
			System.arraycopy(tmp, 0, ret, offset, tmp.length);
			offset+=tmp.length;
		}
		if(bAddEndFlag)
		{
			ret[offset++] = -1;
			ret[offset++] = -1;
		}
		return ret;
	}

}

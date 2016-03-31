package com.hanvon.handwriting.sentence;

import java.util.ArrayList;

public class TraceStroke {
	ArrayList<Short> stroke = null;
	short[]                shortstroke = null;
	

	public void addPoint(int x,int y)
	{
		stroke.add((short)x);
		stroke.add((short)y);
	}

	public TraceStroke()
	{
		stroke = new ArrayList<Short>();
	}
	public int  getX(int idx)
	{
		return stroke.get(idx<<1);
	}
	public int  getY(int idx)
	{
		return stroke.get((idx<<1)+1);
	}

	
	public int  getPtCount()
	{
		return stroke.size()>>1;
	}
	

	public void clear()
	{
		stroke.clear();
	}
	
	public void endStroke()
	{
		shortstroke = new short[stroke.size()+2];
		for(int i=0;i<stroke.size();i++)
		{
			shortstroke[i] = stroke.get(i);
		}
		shortstroke[stroke.size()] = -1;
		shortstroke[stroke.size()+1] = 0;
		
	}
	int   getStrokeDataCount()
	{
		return stroke.size()+2;
	}
	public short[] getStrokeData()
	{
		if(shortstroke == null)
			endStroke();
		return shortstroke;
	}

}

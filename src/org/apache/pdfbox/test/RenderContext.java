/*
 * PDFBoxLite: a subset of the Apache PDFBox library that provides just the basic necessities for
 * creating a PDF file with some basic drawing primitives. This is for the benefit of projects
 * that do not want the full universe of PDF functionality, nor the many megabytes of code to work
 * with it.
 * 
 * License: original project Apache License, all modifications public domain
 * 
 * Author: Dr. Alex M. Clark (http://molmatinf.com)
 */

package org.apache.pdfbox.test;

import java.io.*;
import java.util.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.*;

/*
	Simple example of using basic primitives on a PDF stream.
*/

public class RenderContext
{
	public PDPageContentStream stream = null;
	public float width = 0, height = 0;

	public static final int NOCOLOUR = -1;
	public static final int TXTALIGN_CENTRE = 0;
	public static final int TXTALIGN_LEFT = 1;
	public static final int TXTALIGN_RIGHT = 2;
	public static final int TXTALIGN_BASELINE = 0;
	public static final int TXTALIGN_MIDDLE = 4;
	public static final int TXTALIGN_TOP = 8;
	public static final int TXTALIGN_BOTTOM = 16;

	//private PDFont font = PDType1Font.HELVETICA;

	// ------------ public methods ------------	

	public RenderContext()
	{
	}

	public void drawLine(float x1, float y1, float x2, float y2, int colour, float thickness)
	{
		try
		{
    		if (stream == null)
    		{
        		width = Math.max(width, Math.max(x1 + 0.5f * thickness, x2 + 0.5f * thickness));
        		height = Math.max(height, Math.max(y1 + 0.5f * thickness, y2 + 0.5f * thickness));
    		}
    		else
    		{
    			if (colour == NOCOLOUR) return;
                stream.moveTo(x1, height - y1);
                stream.lineTo(x2, height - y2);
                stream.setLineWidth(thickness);
                setStroke(colour);
                stream.stroke();
    		}
		}
		catch (IOException ex) {}
	}

	public void drawRect(float x, float y, float w, float h, int edgeCol, float thickness, int fillCol)
	{
		try
		{
    		if (stream == null)
    		{
    			width = Math.max(width, x + w + 0.5f * thickness);
    			height = Math.max(height, y + h + 0.5f * thickness);
    		}
    		else
    		{
    			if (fillCol != NOCOLOUR)
    			{
    				stream.addRect(x, height - y, w, -h);
    				setFill(fillCol);
    				stream.fill();
    			}
    			if (edgeCol != NOCOLOUR)
    			{
    				stream.addRect(x, height - y, w, -h);
    	            stream.setLineWidth(thickness);
    				setStroke(edgeCol);
    				stream.stroke();
    			}
    		}
		}
		catch (IOException ex) {}
	}
	
	public void drawOval(float cx, float cy, float rw, float rh, int edgeCol, float thickness, int fillCol)
	{
		try
		{
    		if (stream == null)
    		{
    			width = Math.max(width, cx + rw + 0.5f * thickness);
    			height = Math.max(height, cy + rh + 0.5f * thickness);
    		}
    		else
    		{
    			if (fillCol == NOCOLOUR && edgeCol == NOCOLOUR) return;

               	final float kappa = 0.5522848f;
                final float ox = rw * kappa, oy = rh * kappa;
                final float x0 = cx - rw, y0 = height - cy - rh;
                final float xe = cx + rw, ye = height - cy + rh;
                final float xm = cx, ym = height - cy;

                for (int layer = 0; layer < 2; layer++)
                {
                    if (layer == 0 && fillCol == NOCOLOUR) continue;
                    if (layer == 1 && edgeCol == NOCOLOUR) continue;

                    stream.moveTo(x0, ym);
                    stream.curveTo(x0, ym - oy, xm - ox, y0, xm, y0);
                    stream.curveTo(xm + ox, y0, xe, ym - oy, xe, ym);
                    stream.curveTo(xe, ym + oy, xm + ox, ye, xm, ye);
                    stream.curveTo(xm - ox, ye, x0, ym + oy, x0, ym);

                    if (layer == 0)
                    {
                        setFill(fillCol);
                        stream.fill();
                    }
                    else
                    {
                        stream.closePath();
                        stream.setLineWidth(thickness);
                        stream.setLineCapStyle(1);
                        stream.setLineJoinStyle(1);
                        setStroke(edgeCol);
                        stream.stroke();
                    }
                }
    		}
		}
		catch (IOException ex) {}
	}

	public void drawPoly(float[] px, float[] py, int edgeCol, float thickness, int fillCol, boolean closed)
	{
		try
		{
    		if (stream == null)
    		{
    			for (float x : px) width = Math.max(width, x + 0.5f * thickness);
    			for (float y : py) height = Math.max(height, y + 0.5f * thickness);
    		}
    		else
    		{
    			if (fillCol != NOCOLOUR)
    			{
    				stream.moveTo(px[0], height - py[0]);
    				for (int n = 1; n < px.length; n++) stream.lineTo(px[n], height - py[n]);
    				setFill(fillCol);
    				stream.fill();
    			}
    			if (edgeCol != NOCOLOUR)
    			{
    				stream.moveTo(px[0], height - py[0]);
    				for (int n = 1; n < px.length; n++) stream.lineTo(px[n], height - py[n]);
    				if (closed) stream.closePath();
    	            stream.setLineWidth(thickness);
    				setStroke(edgeCol);
    				stream.stroke();
    			}
    		}
		}
		catch (IOException ex) {}
	}

	public void drawCurve(float[] px, float[] py, boolean[] ctrl, int edgeCol, float thickness, int fillCol, boolean closed)
	{
		try
		{
    		if (stream == null)
    		{
    			for (float x : px) width = Math.max(width, x + 0.5f * thickness);
    			for (float y : py) height = Math.max(height, y + 0.5f * thickness);
    		}
    		else
    		{
    			if (fillCol != NOCOLOUR)
    			{
    				defineCurve(px, py, ctrl);
    				setFill(fillCol);
    				stream.fill();
    			}
    			if (edgeCol != NOCOLOUR)
    			{
    				defineCurve(px, py, ctrl);
    				if (closed) stream.closePath();
    	            stream.setLineWidth(thickness);
    				setStroke(edgeCol);
    				stream.stroke();
    			}
    		}
		}
		catch (IOException ex) {}
	}

	public void drawText(float x, float y, String txt, float sz, int colour, int align)
	{
		txt = downgradeToASCII(txt);
		try
		{
    		final float[] wad = measureLine(txt, sz);

    		if ((align & TXTALIGN_LEFT) != 0) {}
    		else if ((align & TXTALIGN_RIGHT) != 0) x -= wad[0];
    		else /*TXTALIGN_CENTRE*/ x -= 0.5f * wad[0];
    
    		if ((align & TXTALIGN_MIDDLE) != 0) y += 0.5f * wad[1];
    		else if ((align & TXTALIGN_TOP) != 0) y += wad[1];
    		else if ((align & TXTALIGN_BOTTOM) != 0) y -= wad[2];
    		// else: TXTALIGN_BASELINE
    	
    		if (stream == null)
    		{
    			width = Math.max(width, x + wad[0]);
    			height = Math.max(height, y + wad[2]);
    		}
    		else
    		{
                stream.beginText();
                stream.setFont(/*font*/ PDType1Font.HELVETICA, sz);
                stream.newLineAtOffset(x, height - y);
                setFill(colour);
                stream.showText(txt);
                stream.endText();
    		}
		}
		catch (IOException ex) {}
	}
	
	// measures a text string, at a given size; the return array is of the form {width,ascent,descent}
	public float[] measureLine(String txt, float sz)
	{
		txt = downgradeToASCII(txt);
		try
		{
    		float width = /*font*/ PDType1Font.HELVETICA.getStringWidth(txt) * sz * 0.001f;
    		float ascent = sz, descent = 0.3f * sz; // faked, but should be good enough
    		return new float[]{width, ascent, descent};
		}
		catch (IOException ex) {return null;}
	}
	
	// ------------ private methods ------------	

	private void setStroke(int rgb) throws IOException
	{
		final int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		stream.setStrokingColor(r, g, b);
	}
	private void setFill(int rgb) throws IOException
	{
		final int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		stream.setNonStrokingColor(r, g, b);
	}

	private void defineCurve(float[] px, float[] py, boolean[] ctrl) throws IOException
	{
		stream.moveTo(px[0], height - py[0]);
		
		int sz = px.length;
		for (int n = 1; n < sz;)
		{
			if (!ctrl[n])
			{
				stream.lineTo(px[n], height - py[n]);
				n++;
			}
			else if (ctrl[n] && n < sz - 1 && !ctrl[n + 1])
			{
				stream.curveTo2(px[n], height - py[n], px[n + 1], height - py[n + 1]);
				n += 2;
			}
			else if (ctrl[n] && n < sz - 2 && ctrl[n + 1] && !ctrl[n + 2])
			{
				stream.curveTo(px[n], height - py[n], px[n + 1], height - py[n + 1], px[n + 2], height - py[n + 2]);
				n += 3;
			}
			else n++; // (dunno, so skip)
		}
	}

	// removes Unicode characters, because they're not part of the default font spec
	private String downgradeToASCII(String str)
	{
		boolean any = false;
		final int len = str.length();
		for (int n = 0; n < len; n++) if (str.charAt(n) >= 127) {any = true; break;}
		if (!any) return str;

		StringBuffer buff = new StringBuffer();
		for (int n = 0; n < len; n++) 
		{
			char ch = str.charAt(n);
			if (ch > 127)
				buff.append("\\u{" + Integer.toString(ch, 16) + "}");
			else
				buff.append(ch);
		}
		return buff.toString();
	}
}



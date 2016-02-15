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

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.*;

public class TestMain
{
	public static void main(String[] argv)
	{
		System.out.println("PDFBoxLite: creating test output.");
		
		try
		{
			PDDocument document = new PDDocument();

    		RenderContext ctx = new RenderContext();
    		
    		renderTestPage(ctx);
    		
            PDPage page = new PDPage(new PDRectangle(ctx.width, ctx.height));
            document.addPage(page);
            
            ctx.stream = new PDPageContentStream(document, page);
            renderTestPage(ctx);
            ctx.stream.close();
            
			document.save(new File("testoutput.pdf"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		System.out.println("Done.");
	}
	
	private static void renderTestPage(RenderContext ctx) throws Exception
	{
		ctx.drawLine(0, 5, 100, 100, 0xFF0000, 1);
		ctx.drawLine(10, 5, 110, 100, 0x0000FF, 5);
		
		float[] cx = new float[]{20, 70, 120};
		float[] cy = new float[]{2, 100, 2};
		boolean[] ctrl = new boolean[]{false, true, false};
		ctx.drawCurve(cx, cy, ctrl, 0x008000, 2, 0xC0C0FF, true);

		float[] px = new float[]{110, 160, 210};
		float[] py = new float[]{10, 80, 10};
		ctx.drawPoly(px, py, 0x000000, 1, 0xC0FFFF, false);

		ctx.drawRect(0, 100, 100, 100, RenderContext.NOCOLOUR, 0, 0x808000);
		ctx.drawOval(150, 150, 45, 45, 0xFF00FF, 1, 0x800080);
		
		ctx.drawLine(10, 220, 30, 220, 0x000000, 1);
		ctx.drawLine(10, 220, 10, 240, 0x000000, 1);
		ctx.drawText(10, 220, "Fnord!", 20, 0xFF0000, RenderContext.TXTALIGN_LEFT | RenderContext.TXTALIGN_TOP);
	}
}

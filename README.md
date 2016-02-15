PDFBoxLite
==========

The PDFBoxLite project is a subset of the PDFBox project, which is intended to
present the bare minimum amount of library functionality needed to create a
static PDF document. Advanced PDF features such as forms, hyperlinks, metadata
and signatures have been stripped out.

The motivation for PDFBoxLite is size reduction. At the present time, PDF seems
to be the only vector graphics format that is universally implemented
(completely and correctly), and is the leading candidate for creating inline
figures for inclusion in documents that need to be print quality. Creating
inline figures is definitely not a major use case for all the advanced
functionality that PDF has accumulated over the years. For this reason adding ~8
megabytes to the size of a Java deliverable is suboptimal, especially when the
main project is a fraction of that size.

For testing purposes, the main method in org.apache.pdfbox.test.TestMain will
create a file called testoutput.pdf, which contains a selection of basic drawing
primitives, including a line of text. This is validation of sorts. Also, for
purposes of getting started with the PDFBox library, you can check out
org.apache.pdfbox.test.RenderContext, which provides a very basic wrapper around
the core functionality. Use it or lose it, but feel free to learn from it.

No doubt plenty more functionality could be cut out, but a reduction to ~1.5
megabytes represents a significant improvement.

Apologies in advance to the PDFBox team, having to see all of your hard work
mercilessly slashed. Anyone who wants to generate complex PDF documents should
go straight to the original project.

- Dr. Alex M. Clark
  February 2016



Apache PDFBox <http://pdfbox.apache.org/>
===================================================

The Apache PDFBox library is an open source Java tool for working with PDF 
documents. This project allows creation of new PDF documents, manipulation 
of existing documents and the ability to extract content from documents.
PDFBox also includes several command line utilities. PDFBox is published
under the Apache License, Version 2.0.

PDFBox is a project of the Apache Software Foundation <http://www.apache.org/>.

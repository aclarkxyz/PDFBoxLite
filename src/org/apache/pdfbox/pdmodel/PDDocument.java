/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * This is the in-memory representation of the PDF document.
 * The #close() method must be called once the document is no longer needed.
 * 
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    private final COSDocument document;

    // cached values
    private PDDocumentInformation documentInformation;
    private PDDocumentCatalog documentCatalog;

    // the encryption will be cached here. When the document is decrypted then
    // the COSDocument will not have an "Encrypt" dictionary anymore and this object must be used
    //private PDEncryption encryption;

    // holds a flag which tells us if we should remove all security from this documents.
    private boolean allSecurityToBeRemoved;

    // keep tracking customized documentId for the trailer. If null, a new id will be generated
    // this ID doesn't represent the actual documentId from the trailer
    private Long documentId;

    // the pdf to be read
    private final RandomAccessRead pdfSource;

    // the access permissions of the document
    //private AccessPermission accessPermission;
    
    // fonts to subset before saving
    private final Set<PDFont> fontsToSubset = new HashSet<PDFont>();
    
    // document-wide cached resources
    private ResourceCache resourceCache = new DefaultResourceCache();
    
    /**
     * Creates an empty PDF document.
     * You need to add at least one page for the document to be valid.
     */
    public PDDocument()
    {
        this(MemoryUsageSetting.setupMainMemoryOnly());
    }

    /**
     * Creates an empty PDF document.
     * You need to add at least one page for the document to be valid.
     *
     * @param memUsageSetting defines how memory is used for buffering PDF streams 
     */
    public PDDocument(MemoryUsageSetting memUsageSetting)
    {
        ScratchFile scratchFile = null;
        try
        {
            scratchFile = new ScratchFile(memUsageSetting);
        }
        catch (IOException ioe)
        {
            //LOG.warn("Error initializing scratch file: " + ioe.getMessage() +
            //         ". Fall back to main memory usage only.");
            try
            {
                scratchFile = new ScratchFile(MemoryUsageSetting.setupMainMemoryOnly());
            }
            catch (IOException ioe2) {}
        }
        
        document = new COSDocument(scratchFile);
        pdfSource = null;

        // First we need a trailer
        COSDictionary trailer = new COSDictionary();
        document.setTrailer(trailer);

        // Next we need the root dictionary.
        COSDictionary rootDictionary = new COSDictionary();
        trailer.setItem(COSName.ROOT, rootDictionary);
        rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
        rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.4"));

        // next we need the pages tree structure
        COSDictionary pages = new COSDictionary();
        rootDictionary.setItem(COSName.PAGES, pages);
        pages.setItem(COSName.TYPE, COSName.PAGES);
        COSArray kidsArray = new COSArray();
        pages.setItem(COSName.KIDS, kidsArray);
        pages.setItem(COSName.COUNT, COSInteger.ZERO);
    }

    /**
     * This will add a page to the document. This is a convenience method, that will add the page to the root of the
     * hierarchy and set the parent of the page to the root.
     * 
     * @param page The page to add to the document.
     */
    public void addPage(PDPage page)
    {
        getPages().add(page);
    }


    /**
     * Remove the page from the document.
     * 
     * @param page The page to remove from the document.
     */
    public void removePage(PDPage page)
    {
        getPages().remove(page);
    }

    /**
     * Remove the page from the document.
     * 
     * @param pageNumber 0 based index to page number.
     */
    public void removePage(int pageNumber)
    {
        getPages().remove(pageNumber);
    }

    /**
     * This will import and copy the contents from another location. Currently the content stream is stored in a scratch
     * file. The scratch file is associated with the document. If you are adding a page to this document from another
     * document and want to copy the contents to this document's scratch file then use this method otherwise just use
     * the addPage method.
     * 
     * @param page The page to import.
     * @return The page that was imported.
     * 
     * @throws IOException If there is an error copying the page.
     */
    public PDPage importPage(PDPage page) throws IOException
    {
        PDPage importedPage = new PDPage(new COSDictionary(page.getCOSObject()), resourceCache);
        InputStream in = null;
        try
        {
            in = page.getContents();
            if (in != null)
            {
                PDStream dest = new PDStream(this, page.getContents(), COSName.FLATE_DECODE);
                importedPage.setContents(dest);
            }
            addPage(importedPage);
        }
        catch (IOException e)
        {
            IOUtils.closeQuietly(in);
        }

        return importedPage;
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     * 
     * @param doc The COSDocument that this document wraps.
     */
    public PDDocument(COSDocument doc)
    {
        this(doc, null);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     * 
     * @param doc The COSDocument that this document wraps.
     * @param source the parser which is used to read the pdf
     */
    public PDDocument(COSDocument doc, RandomAccessRead source)
    {
        document = doc;
        pdfSource = source;
    }

    /**
     * This will get the low level document.
     * 
     * @return The document that this layer sits on top of.
     */
    public COSDocument getDocument()
    {
        return document;
    }

    /**
     * This will get the document info dictionary. This is guaranteed to not return null.
     * 
     * @return The documents /Info dictionary
     */
    public PDDocumentInformation getDocumentInformation()
    {
        if (documentInformation == null)
        {
            COSDictionary trailer = document.getTrailer();
            COSDictionary infoDic = (COSDictionary) trailer.getDictionaryObject(COSName.INFO);
            if (infoDic == null)
            {
                infoDic = new COSDictionary();
                trailer.setItem(COSName.INFO, infoDic);
            }
            documentInformation = new PDDocumentInformation(infoDic);
        }
        return documentInformation;
    }

    /**
     * This will set the document information for this document.
     * 
     * @param info The updated document information.
     */
    public void setDocumentInformation(PDDocumentInformation info)
    {
        documentInformation = info;
        document.getTrailer().setItem(COSName.INFO, info.getCOSObject());
    }

    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     * 
     * @return The documents /Root dictionary
     */
    public PDDocumentCatalog getDocumentCatalog()
    {
        if (documentCatalog == null)
        {
            COSDictionary trailer = document.getTrailer();
            COSBase dictionary = trailer.getDictionaryObject(COSName.ROOT);
            if (dictionary instanceof COSDictionary)
            {
                documentCatalog = new PDDocumentCatalog(this, (COSDictionary) dictionary);
            }
            else
            {
                documentCatalog = new PDDocumentCatalog(this);
            }
        }
        return documentCatalog;
    }

    /**
     * Save the document to a file.
     * 
     * @param fileName The file to save as.
     *
     * @throws IOException if the output could not be written
     */
    public void save(String fileName) throws IOException
    {
        save(new File(fileName));
    }

    /**
     * Save the document to a file.
     * 
     * @param file The file to save as.
     *
     * @throws IOException if the output could not be written
     */
    public void save(File file) throws IOException
    {
        save(new BufferedOutputStream(new FileOutputStream(file)));
    }

    /**
     * This will save the document to an output stream.
     * 
     * @param output The stream to write to.
     *
     * @throws IOException if the output could not be written
     */
    public void save(OutputStream output) throws IOException
    {
        if (document.isClosed())
        {
            throw new IOException("Cannot save a document which has been closed");
        }

        // subset designated fonts
        for (PDFont font : fontsToSubset)
        {
            font.subset();
        }
        fontsToSubset.clear();
        
        // save PDF
        COSWriter writer = new COSWriter(output);
        try
        {
            writer.write(this);
            writer.close();
        }
        finally
        {
            writer.close();
        }
    }

   /**
     * Save the PDF as an incremental update. This is only possible if the PDF was loaded from a file.
     *
     * @param output stream to write
     * @throws IOException if the output could not be written
     * @throws IllegalStateException if the document was not loaded from a file.
     */
    public void saveIncremental(OutputStream output) throws IOException
    {
        COSWriter writer = null;
        try
        {
            writer = new COSWriter(output, pdfSource);
            writer.write(this);
            writer.close();
        }
        finally
        {
            if (writer != null)
            {
                writer.close();
            }
        }
    }

    /**
     * Returns the page at the given index.
     *
     * @param pageIndex the page index
     * @return the page at the given index.
     */
    public PDPage getPage(int pageIndex) // todo: REPLACE most calls to this method with BELOW method
    {
        return getDocumentCatalog().getPages().get(pageIndex);
    }

    /**
     * Returns the page tree.
     * 
     * @return the page tree
     */
    public PDPageTree getPages()
    {
        return getDocumentCatalog().getPages();
    }

    /**
     * This will return the total page count of the PDF document.
     * 
     * @return The total number of pages in the PDF document.
     */
    public int getNumberOfPages()
    {
        return getDocumentCatalog().getPages().getCount();
    }

    /**
     * This will close the underlying COSDocument object.
     * 
     * @throws IOException If there is an error releasing resources.
     */
    @Override
    public void close() throws IOException
    {
        if (!document.isClosed())
        {
            // close all intermediate I/O streams
            document.close();
            
            // close the source PDF stream, if we read from one
            if (pdfSource != null)
            {
                pdfSource.close();
            }
        }
    }

    /**
     * Indicates if all security is removed or not when writing the pdf.
     * 
     * @return returns true if all security shall be removed otherwise false
     */
    public boolean isAllSecurityToBeRemoved()
    {
        return allSecurityToBeRemoved;
    }

    /**
     * Activates/Deactivates the removal of all security when writing the pdf.
     * 
     * @param removeAllSecurity remove all security if set to true
     */
    public void setAllSecurityToBeRemoved(boolean removeAllSecurity)
    {
        allSecurityToBeRemoved = removeAllSecurity;
    }

    /**
     * Provides the document ID.
     *
     * @return the dcoument ID
     */
    public Long getDocumentId()
    {
        return documentId;
    }

    /**
     * Sets the document ID to the given value.
     * 
     * @param docId the new document ID
     */
    public void setDocumentId(Long docId)
    {
        documentId = docId;
    }
    
    /**
     * Returns the PDF specification version this document conforms to.
     *
     * @return the PDF version (e.g. 1.4f)
     */
    public float getVersion()
    {
        float headerVersionFloat = getDocument().getVersion();
        // there may be a second version information in the document catalog starting with 1.4
        if (headerVersionFloat >= 1.4f)
        {
            String catalogVersion = getDocumentCatalog().getVersion();
            float catalogVersionFloat = -1;
            if (catalogVersion != null)
            {
                try
                {
                    catalogVersionFloat = Float.parseFloat(catalogVersion);
                }
                catch(NumberFormatException exception)
                {
                    //LOG.error("Can't extract the version number of the document catalog.", exception);
                }
            }
            // the most recent version is the correct one
            return Math.max(catalogVersionFloat, headerVersionFloat);
        }
        else
        {
            return headerVersionFloat;
        }
    }

    /**
     * Sets the PDF specification version for this document.
     *
     * @param newVersion the new PDF version (e.g. 1.4f)
     * 
     */
    public void setVersion(float newVersion)
    {
        float currentVersion = getVersion();
        // nothing to do?
        if (newVersion == currentVersion)
        {
            return;
        }
        // the version can't be downgraded
        if (newVersion < currentVersion)
        {
            //LOG.error("It's not allowed to downgrade the version of a pdf.");
            return;
        }
        // update the catalog version if the document version is >= 1.4
        if (getDocument().getVersion() >= 1.4f)
        {
            getDocumentCatalog().setVersion(Float.toString(newVersion));
        }
        else
        {
            // versions < 1.4f have a version header only
            getDocument().setVersion(newVersion);
        }
    }

    /**
     * Returns the resource cache associated with this document, or null if there is none.
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    /**
     * Sets the resource cache associated with this document.
     * 
     * @param resourceCache A resource cache, or null.
     */
    public void setResourceCache(ResourceCache resourceCache)
    {
        this.resourceCache = resourceCache;
    }
}

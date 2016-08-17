/**
 * Copyright 2010-2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.interactive_instruments.etf.dal.dao.basex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.model.EidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.IFile;
import de.interactive_instruments.MediaType;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.etf.model.Parameterizable;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.PropertyHolder;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class XsltOutputTransformer implements OutputFormat {

	/**
	 *  Identifier for the transformer
	 */
	private final String label;

	private final EID id;

	/**
	 *  Thread safe!
	 */
	private Templates cachedXSLT;

	private ConfigProperties config;
	private IFile stylesheetFile;
	private long stylesheetLastModified = 0;
	private final TransformerFactory transFact = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
	private final Logger logger = LoggerFactory.getLogger(XsltOutputTransformer.class);

	/**
	 * Create a new XSL Output Transformer
	 *
	 * @param label
	 * @param stylesheetJarPath
	 * @param jarImportPath
	 * @throws IOException if stylesheet is not readable
	 * @throws TransformerConfigurationException if stylesheet
	 * contains errors
	 */
	public XsltOutputTransformer(final Dao dao, final String label, final String stylesheetJarPath, final String jarImportPath)
			throws IOException, TransformerConfigurationException {
		this.id = EidFactory.getDefault().createUUID(dao.getDtoType().getSimpleName());
		this.label = label;
		this.stylesheetFile = null;

		final ClassLoader cL = getClass().getClassLoader();

		final Source xsltSource = new StreamSource(cL.getResourceAsStream(stylesheetJarPath));
		xsltSource.setSystemId(stylesheetJarPath);

		if (jarImportPath != null) {
			transFact.setURIResolver((ref, base) -> {
				final InputStream s = cL.getResourceAsStream(jarImportPath + "/" + ref);
				return new StreamSource(s);
			});
		}

		this.cachedXSLT = transFact.newTemplates(xsltSource);
	}

	/**
	 * Create a new XSL Output Transformer
	 *
	 * @param label
	 * @param stylesheetFile XSL stylesheet file
	 * @throws IOException if stylesheet is not readable
	 * @throws TransformerConfigurationException if stylesheet
	 * contains errors
	 */
	public XsltOutputTransformer(final Dao dao, final String label, final IFile stylesheetFile)
			throws IOException, TransformerConfigurationException {
		this.id = EidFactory.getDefault().createUUID(dao.getDtoType().getSimpleName());
		this.label = label;
		this.stylesheetFile = stylesheetFile;
		stylesheetFile.expectFileIsReadable();

		recacheChangedStylesheet();
	}

	private void recacheChangedStylesheet() throws TransformerConfigurationException {
		if (stylesheetFile != null && stylesheetFile.lastModified() != stylesheetLastModified) {
			synchronized (this) {
				logger.info(this.label + " : caching stylesheet " + stylesheetFile.getAbsolutePath());
				final Source xsltSource = new StreamSource(stylesheetFile);
				this.cachedXSLT = transFact.newTemplates(xsltSource);
				this.stylesheetLastModified = stylesheetFile.lastModified();
			}
		}
	}

	@Override
	public EID getId() {
		return this.id;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public MediaType getMediaTypeType() {
		return null;
	}

	@Override
	public Parameterizable getParameters() {
		return null;
	}

	@Override
	public void streamTo(final PropertyHolder arguments, final InputStream inputStream, final OutputStream outputStreamStream) throws IOException {

		try {
			recacheChangedStylesheet();

			final Transformer transformer = cachedXSLT.newTransformer();
			if (config != null) {
				config.namePropertyPairs().forEach(p -> {
					transformer.setParameter(p.getKey(), p.getValue());
				});
			}
			transformer.transform(
					new StreamSource(inputStream), new StreamResult(outputStreamStream));
		} catch (TransformerException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	// Todo use configureable
	public void init(PropertyHolder config) {
		if (config.getProperty("etf.webapp.service.url") != null) {
			this.config = new ConfigProperties();
			this.config.setProperty("baseUrl",
					config.getProperty("etf.webapp.service.url"));
		}
	}

	@Override
	public int compareTo(final OutputFormat o) {
		return 0;
	}
}

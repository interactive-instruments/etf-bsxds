/**
 * Copyright 2017-2019 European Union, interactive instruments GmbH
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.dal.dao.basex;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.basex.core.BaseXException;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dao.Filter;
import de.interactive_instruments.etf.dal.dao.OutputFormatStreamable;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.properties.Properties;
import de.interactive_instruments.properties.PropertyHolder;

/**
 * Abstract class for a prepared XQuery statement whose result can be directly
 * streamed.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
abstract class AbstractBsxPreparedDto implements OutputFormatStreamable {

	protected final BsXQuery bsXquery;

	public AbstractBsxPreparedDto(final BsXQuery xquery) {
		this.bsXquery = xquery;
	}

	private static String[] optionalParameterNames = {"offset", "limit"};

	/**
	 * Streams the result from the BaseX database to the Output Format directly
	 * through a piped stream
	 *
	 * @param outputFormat the Output Format
	 * @param arguments transformation arguments
	 * @param outputStream transformed output stream
	 *
	 */
	public void streamTo(final OutputFormat outputFormat, final PropertyHolder arguments, final OutputStream outputStream) {
		try {

			// create a copy
			final Properties properties = new Properties(arguments);

			// DETAILED_WITHOUT_HISTORY level is required
			bsXquery.parameter("levelOfDetail", String.valueOf(Filter.LevelOfDetail.DETAILED_WITHOUT_HISTORY), "xs:string");

			// Pass parameters from query to XSLT
			for (final String optionalParameterName : optionalParameterNames) {
				final String val = bsXquery.getParameter(optionalParameterName);
				if (!SUtils.isNullOrEmpty(val)) {
					properties.setProperty(optionalParameterName, val);
				}
			}

			final String fields = bsXquery.getParameter("fields");
			if (!SUtils.isNullOrEmpty(fields) && !fields.equals("*")) {
				properties.setProperty("fields", fields);
			}

			// Required property
			properties.setProperty(
					"selection",
					Objects.requireNonNull(
							bsXquery.getParameter("selection"),
							"Invalid selection"));

			final PipedInputStream in = new PipedInputStream();
			final PipedOutputStream out = new PipedOutputStream(in);
			new Thread(() -> {
				try {
					bsXquery.execute(out);
				} catch (final IOException e) {
					throw new BsxPreparedDtoException(e);
				} finally {
					try {
						out.close();
					} catch (IOException e) {
						ExcUtils.suppress(e);
					}
				}
			}).start();
			Objects.requireNonNull(outputFormat, "Output Format is null").streamTo(properties, in, outputStream);
			// statement for streaming the request without transformation - for debug purposes:
			// bsXquery.execute(outputStream);
		} catch (IOException e) {
			logError(e);
			throw new BsxPreparedDtoException(e);
		}
	}

	protected final void logError(final Throwable e) {
		bsXquery.getCtx().getLogger().error("Query Exception: {}", ExceptionUtils.getRootCauseMessage(e));
		if (bsXquery.getCtx().getLogger().isDebugEnabled()) {
			try {
				if (bsXquery.getCtx().getLogger().isTraceEnabled()) {
					bsXquery.getCtx().getLogger().trace("Query: {}", bsXquery.toString());
					if (ExceptionUtils.getRootCause(e) != null &&
							ExceptionUtils.getRootCause(e) instanceof NullPointerException) {
						bsXquery.getCtx().getLogger().trace("NullPointerException may indicate an invalid mapping!");
					}
				}
				Thread.sleep((long) (Math.random() * 2450));
				bsXquery.getCtx().getLogger().debug("Query result: {}", bsXquery.execute());
			} catch (InterruptedException | BaseXException e2) {
				ExcUtils.suppress(e2);
			}
		}
	}
}

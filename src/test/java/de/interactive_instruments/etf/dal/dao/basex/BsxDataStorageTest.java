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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.DATA_STORAGE;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class BsxDataStorageTest {

	/**
	 * Check if the BsxSchemaResourceResolver resolves all schema files
	 * and ensure the schema is valid
	 *
	 * @throws SAXException
	 */
	@Test(timeout = 35000)
	public void loadSchema() throws SAXException, ConfigurationException, InvalidStateTransitionException,
			InitializationException, StorageException, IOException {
		BsxTestUtils.ensureInitialization();
		assertTrue(DATA_STORAGE.isInitialized());
		assertNotNull(DATA_STORAGE.getSchema());
		assertNotNull(DATA_STORAGE.getSchema().newValidator());
	}

	@Test(timeout = 35000)
	public void releaseAndInit() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		assertTrue(DATA_STORAGE.isInitialized());
		DATA_STORAGE.release();
		assertFalse(DATA_STORAGE.isInitialized());
		DATA_STORAGE.init();
		assertTrue(DATA_STORAGE.isInitialized());
		assertNotNull(DATA_STORAGE.getBsxCtx());
	}

}

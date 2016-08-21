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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtil.DATA_STORAGE;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class BsxDataStorageTest {

	/**
	 * Check if the BsxSchemaResourceResolver resolves all schema files
	 * and ensure the schema is valid
	 *
	 * @throws SAXException
	 */
	@Test
	public void loadSchema() throws SAXException, ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException, IOException {
		BsxTestUtil.ensureInitialization();
		assertTrue(DATA_STORAGE.isInitialized());
		assertNotNull(DATA_STORAGE.getSchema());
		assertNotNull(DATA_STORAGE.getSchema().newValidator());
	}

	@Test(timeout = 4000)
	public void releaseAndInit() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		assertTrue(DATA_STORAGE.isInitialized());
		DATA_STORAGE.release();
		assertFalse(DATA_STORAGE.isInitialized());
		DATA_STORAGE.init();
		assertTrue(DATA_STORAGE.isInitialized());
		assertNotNull(DATA_STORAGE.getBsxCtx());
	}

}

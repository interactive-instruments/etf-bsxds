/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.dal.dao.basex;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.EtfConstants;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
class BsxTestConstants {

	public final static BsxDataStorage DATA_STORAGE;

	public final static TestObjectDto TO_DTO_1;

	static {
		DATA_STORAGE = new BsxDataStorage();

		TO_DTO_1 = new TestObjectDto();
		TO_DTO_1.setId(EidFactory.getDefault().createUUID("TestObjectDto.1"));
		TO_DTO_1.setLabel("TestObjectDto.1.label");
		TO_DTO_1.setDescription("TestObjectDto.1.description");
		TO_DTO_1.setAuthor("TestObjectDto.1.author");
	}

	static void ensureInitialization() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		if(!DATA_STORAGE.isInitialized()) {

			final IFile dataStorageDir;
			if(System.getenv("ETF_DS_DIR")!=null) {
				dataStorageDir = new IFile(System.getenv("ETF_DS_DIR"));
				dataStorageDir.mkdirs();
			}else if(new IFile("./build").exists()) {
				dataStorageDir = new IFile("./build/tmp/etf-ds");
				dataStorageDir.mkdirs();
			}else{
				dataStorageDir=null;
			}

			assertTrue(dataStorageDir!=null && dataStorageDir.exists());
			DATA_STORAGE.getConfigurationProperties().setProperty(EtfConstants.ETF_DATASOURCE_DIR, dataStorageDir.getAbsolutePath());
			DATA_STORAGE.init();
		}
	}
}

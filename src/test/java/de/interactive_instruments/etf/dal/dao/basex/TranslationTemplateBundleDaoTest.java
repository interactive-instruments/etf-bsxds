/**
 * Copyright 2017 European Union, interactive instruments GmbH
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
import static de.interactive_instruments.etf.test.TestDtos.TTB_DTO_1;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TranslationTemplateBundleDaoTest {

	private static WriteDao<TranslationTemplateBundleDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
			StorageException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TranslationTemplateBundleDto.class));
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(TTB_DTO_1.getId());
		} catch (ObjectWithIdNotFoundException | StorageException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
		assertNotNull(writeDao);
		assertTrue(writeDao.isInitialized());
		assertFalse(writeDao.exists(TTB_DTO_1.getId()));
		writeDao.add(TTB_DTO_1);
		assertTrue(writeDao.exists(TTB_DTO_1.getId()));
		writeDao.delete(TTB_DTO_1.getId());
		assertFalse(writeDao.exists(TTB_DTO_1.getId()));
	}

	@Test
	public void test_2_getById() throws StorageException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(TTB_DTO_1.getId()));
		writeDao.add(TTB_DTO_1);
		assertTrue(writeDao.exists(TTB_DTO_1.getId()));
		final PreparedDto<TranslationTemplateBundleDto> preparedDto = writeDao.getById(TTB_DTO_1.getId());

		// Check internal ID
		assertEquals(TTB_DTO_1.getId(), preparedDto.getDtoId());
		final TranslationTemplateBundleDto dto = preparedDto.getDto();
		assertNotNull(dto);
		assertEquals(TTB_DTO_1.getId(), dto.getId());

		assertNotNull(dto.getTranslationTemplateCollection("TR.Template.1"));
		assertArrayEquals(new String[]{"de", "en"},
				dto.getTranslationTemplateCollection("TR.Template.1").getLanguages().toArray(new String[2]));

		assertNull(dto.getTranslationTemplate("en", "TR.Template.1"));
		assertNotNull(dto.getTranslationTemplate("TR.Template.1", "en"));
		assertEquals("TR.Template.1", dto.getTranslationTemplate("TR.Template.1", "en").getName());
		assertEquals(Locale.ENGLISH.toLanguageTag(), dto.getTranslationTemplate("TR.Template.1", "en").getLanguage());
		assertEquals("TR.Template.1 with three tokens: {TOKEN.3} {TOKEN.1} {TOKEN.2}",
				dto.getTranslationTemplate("TR.Template.1", "en").getStrWithTokens());

		assertNull(dto.getTranslationTemplate("de", "TR.Template.2"));
		assertNotNull(dto.getTranslationTemplate("TR.Template.2", "de"));
		assertEquals("TR.Template.2", dto.getTranslationTemplate("TR.Template.2", "de").getName());
		assertEquals(Locale.GERMAN.toLanguageTag(), dto.getTranslationTemplate("TR.Template.2", "de").getLanguage());
		assertEquals("TR.Template.2 mit drei tokens: {TOKEN.5} {TOKEN.4} {TOKEN.6}",
				dto.getTranslationTemplate("TR.Template.2", "de").getStrWithTokens());

		writeDao.delete(TTB_DTO_1.getId());
		assertFalse(writeDao.exists(TTB_DTO_1.getId()));
	}

	@Test
	public void test_7_0_stream_file_to_store()
			throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, IncompleteDtoException,
			URISyntaxException {
		final IFile testObjectXmlFile = new IFile(getClass().getClassLoader().getResource(
				"database/translationtemplatebundle.xml").toURI());
		final EID id = EidFactory.getDefault().createAndPreserveStr("70a263c0-0ad7-42f2-9d4d-0d8a4ca71b52");

		final TranslationTemplateBundleDto translationTemplateBundle = ((StreamWriteDao<TranslationTemplateBundleDto>) writeDao)
				.add(new FileInputStream(testObjectXmlFile));
		translationTemplateBundle.ensureBasicValidity();

		assertEquals(id.getId(), translationTemplateBundle.getId().getId());

	}

	@Test(expected = StoreException.class)
	public void test_7_1_stream_file_to_store()
			throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, IncompleteDtoException,
			URISyntaxException {
		final IFile testObjectXmlFile = new IFile(getClass().getClassLoader().getResource(
				"database/invalidtranslationtemplatebundle.xml").toURI());
		((StreamWriteDao<TranslationTemplateBundleDto>) writeDao).add(new FileInputStream(testObjectXmlFile));
	}
}

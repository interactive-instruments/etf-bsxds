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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.*;
import static de.interactive_instruments.etf.test.TestDtos.*;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Filter;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.PreparedDtoCollection;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.ResourceDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.test.TestDtos;
import de.interactive_instruments.exceptions.*;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestObjectDaoTest {

	private static final String TO_DTO_1_REPLACED_ID = "aa1b77e2-59d5-3ce6-bbe2-eb2b4c4ae61d";
	final int maxDtos = 250;
	private static WriteDao<TestObjectDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
			StorageException, ObjectWithIdNotFoundException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestObjectDto.class));

		final WriteDao<TagDto> tagDao = (WriteDao<TagDto>) DATA_STORAGE.getDao(TagDto.class);
		BsxTestUtils.forceDeleteAndAdd(TAG_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(TAG_DTO_2);
		BsxTestUtils.forceDeleteAndAdd(TAG_DTO_3);

		final WriteDao<TestObjectTypeDto> testObjectTypeDao = (WriteDao<TestObjectTypeDto>) DATA_STORAGE
				.getDao(TestObjectTypeDto.class);
		BsxTestUtils.forceDeleteAndAdd(TOT_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(TOT_DTO_2);
		BsxTestUtils.forceDeleteAndAdd(TOT_DTO_3);
	}

	@AfterClass
	public static void tearDown()
			throws ConfigurationException, InvalidStateTransitionException, InitializationException, StorageException {
		BsxTestUtils.forceDelete(writeDao, EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID));

		BsxTestUtils.forceDelete(writeDao, TAG_DTO_1.getId());
		BsxTestUtils.forceDelete(writeDao, TAG_DTO_2.getId());
		BsxTestUtils.forceDelete(writeDao, TAG_DTO_3.getId());

		BsxTestUtils.forceDelete(writeDao, TOT_DTO_1.getId());
		BsxTestUtils.forceDelete(writeDao, TOT_DTO_2.getId());
		BsxTestUtils.forceDelete(writeDao, TOT_DTO_3.getId());
	}

	@Before
	public void clean() throws StorageException {
		BsxTestUtils.forceDelete(writeDao, TO_DTO_1.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_1_0_type_check() throws StorageException {
		final WriteDao dao = writeDao;
		dao.add(TOT_DTO_1);
	}

	@Test
	public void test_1_1_failOnInvalidDto() throws StorageException, ObjectWithIdNotFoundException {
		final TestObjectDto invalidDto = new TestObjectDto();
		TestDtos.setBasicProperties(invalidDto, 666);
		BsxTestUtils.forceDelete(writeDao, invalidDto.getId());

		// Must fail because no resources and test object types are set
		try {
			writeDao.add(invalidDto);
		} catch (StorageException e) {
			if (!DATA_STORAGE.getLogger().isDebugEnabled()) {
				assertFalse(writeDao.exists(invalidDto.getId()));
			}
			return;
		}
		BsxTestUtils.forceDelete(writeDao, invalidDto.getId());
		fail("No Exception thrown");
	}

	@Test
	public void test_1_2_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
		assertTrue(writeDao.isInitialized());

		notExistsOrDisabled(TO_DTO_1);
		writeDao.add(TO_DTO_1);
		existsAndNotDisabled(TO_DTO_1);
		writeDao.delete(TO_DTO_1.getId());
		notExistsOrDisabled(TO_DTO_1);
	}

	@Test
	public void test_1_2_unmarshalling() throws JAXBException, IOException, URISyntaxException {
		final IFile testObjectXmlFile = getTestResourceFile("database/testobjects.xml");
		testObjectXmlFile.expectFileIsReadable();
		final Unmarshaller um = BsxTestUtils.DATA_STORAGE.createUnmarshaller();
		final DsResultSet dtos = (DsResultSet) um.unmarshal(new StringReader(
				testObjectXmlFile.readContent().toString()));
		assertNotNull(dtos);
		assertNotNull(dtos.getTestObjects());
	}

	@Test
	public void test_2_0_getById() throws StorageException, ObjectWithIdNotFoundException {
		final PreparedDto<TestObjectDto> preparedDto = BsxTestUtils.addAndGetByIdTest(TO_DTO_1);

		// Check references
		assertNotNull(preparedDto.getDto().getResources());
		assertEquals(TO_DTO_1.getResources().size(),
				preparedDto.getDto().getResources().size());
		assertNotNull(preparedDto.getDto().getTestObjectTypes());
		assertEquals(TO_DTO_1.getTestObjectTypes().size(),
				preparedDto.getDto().getTestObjectTypes().size());
		assertNotNull(preparedDto.getDto().getTags());
		assertEquals(TO_DTO_1.getTags().size(),
				preparedDto.getDto().getTags().size());

		writeDao.delete(TO_DTO_1.getId());
		notExistsOrDisabled(TO_DTO_1);
	}

	@Test(expected = ObjectWithIdNotFoundException.class)
	public void test_2_1_get_non_existing_id() throws StorageException, ObjectWithIdNotFoundException {
		final EID nonExistingId = EidFactory.getDefault().createAndPreserveStr("FOO");
		assertFalse(writeDao.exists(nonExistingId));
		writeDao.getById(nonExistingId);
	}

	private void cleanGeneratedItems() throws StorageException {
		// Clean
		for (int i = 0; i <= maxDtos; i++) {
			final String iStr = BsxTestUtils.toStrWithTrailingZeros(i);
			BsxTestUtils.forceDelete(writeDao, EidFactory.getDefault().createUUID("TestObjectDto." + iStr));
		}
	}

	@Test(timeout = 40000L)
	public void test_3_0_pagination() throws StorageException {

		// Clean
		try {
			writeDao.delete(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID));
		} catch (ObjectWithIdNotFoundException | StorageException e) {}

		cleanGeneratedItems();

		{
			// This test only works if there are now Test Objects in database
			final PreparedDtoCollection<TestObjectDto> clResult = writeDao.getAll(new Filter() {
				@Override
				public int offset() {
					return 0;
				}

				@Override
				public int limit() {
					return 10000;
				}
			});
			assertTrue("Test only works with data storage that does not contain any Test Objects!", clResult.isEmpty());
		}

		final Date creationDate = new Date();

		final List<TestObjectTypeDto> testObjectTypeDtos = new ArrayList<TestObjectTypeDto>() {
			{
				add(TOT_DTO_1);
				add(TOT_DTO_2);
				add(TOT_DTO_3);
			}
		};

		final List<TagDto> tagDtos = new ArrayList<TagDto>() {
			{
				add(TAG_DTO_1);
				add(TAG_DTO_2);
				add(TAG_DTO_2);
			}
		};

		// Add
		final List<TestObjectDto> dtos = new ArrayList<>();
		for (int i = 0; i <= maxDtos; i++) {
			final TestObjectDto dto = new TestObjectDto();
			TestDtos.setBasicProperties(dto, i);
			dto.setLastEditor("TestObjectDto." + BsxTestUtils.toStrWithTrailingZeros(i) + ".author");
			dto.setLastUpdateDate(creationDate);
			dto.setReference("https://reference.reference/spec/3");
			dto.setTestObjectTypes(testObjectTypeDtos);
			dto.setTags(tagDtos);

			final ResourceDto resourceDto = new ResourceDto();
			resourceDto.setName("Resource." + BsxTestUtils.toStrWithTrailingZeros(i));
			resourceDto.setUri(URI.create("http://nowhere.com/" + BsxTestUtils.toStrWithTrailingZeros(i)));
			dto.addResource(resourceDto);
			dtos.add(dto);
		}
		writeDao.addAll(dtos);

		// Test paginagtion
		final int min1 = 30;
		final int max1 = 150;
		final PreparedDtoCollection<TestObjectDto> result1 = writeDao.getAll(new Filter() {
			@Override
			public int offset() {
				return min1;
			}

			@Override
			public int limit() {
				return max1;
			}
		});
		{
			assertNotNull(result1);
			assertEquals(max1, result1.size());
			int i1 = min1;
			for (final TestObjectDto testObjectDto : result1) {
				final String iStr = BsxTestUtils.toStrWithTrailingZeros(i1);
				// Result shall be sorted based on its labeld
				assertEquals("TestObjectDto." + iStr + ".label", testObjectDto.getLabel());
				++i1;
			}
		}
		final int min2 = 52;
		final int max2 = 198;
		final PreparedDtoCollection<TestObjectDto> result2 = writeDao.getAll(new Filter() {
			@Override
			public int offset() {
				return min2;
			}

			@Override
			public int limit() {
				return max2;
			}
		});

		{
			assertNotNull(result2);

			// Prequery ids
			final Set<EID> idsInCollection = result2.keySet();
			assertEquals(max2, result2.size());
			assertEquals(idsInCollection.size(), result2.size());

			int i2 = min2;
			for (final TestObjectDto testObjectDto : result2) {
				assertTrue(idsInCollection.contains(testObjectDto.getId()));

				final String iStr = BsxTestUtils.toStrWithTrailingZeros(i2);
				// Result shall be sorted based on its label
				assertEquals("TestObjectDto." + iStr + ".label", testObjectDto.getLabel());
				++i2;
			}
		}
		assertNotEquals(0, result1.compareTo(result2));
	}

	@Test
	public void test_4_1_streaming_xml()
			throws StorageException, ObjectWithIdNotFoundException, IOException {
		compareStreamingContentXml(TO_DTO_1, "cmp/TestObjectInItemCollectionResponse.xml", "DsResult2Xml");
	}

	@Test
	public void test_4_2_streaming_json()
			throws StorageException, ObjectWithIdNotFoundException, IOException {
		compareStreamingContentRaw(TO_DTO_1, "cmp/TestObjectInItemCollectionResponse.json", "DsResult2Json");
	}

	@Test
	public void test_5_0_update() throws StorageException, ObjectWithIdNotFoundException {

		// This test case only works after a clean because the generated ID

		// Clean
		try {
			writeDao.delete(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID));
		} catch (ObjectWithIdNotFoundException | StorageException e) {
			ExcUtils.suppress(e);
		}

		notExistsOrDisabled(TO_DTO_1);
		writeDao.add(TO_DTO_1);
		existsAndNotDisabled(TO_DTO_1);

		final String originalLabel = TO_DTO_1.getLabel();

		// Prepare query
		final PreparedDto<TestObjectDto> preparedDto = writeDao.getById(TO_DTO_1.getId());
		assertNotNull(preparedDto);
		// Will execute the query
		assertNull(preparedDto.getDto().getReplacedBy());

		// Check references
		assertNotNull(preparedDto.getDto().getResources());
		assertEquals(TO_DTO_1.getResources().size(),
				preparedDto.getDto().getResources().size());
		assertNotNull(preparedDto.getDto().getTestObjectTypes());
		assertEquals(TO_DTO_1.getTestObjectTypes().size(),
				preparedDto.getDto().getTestObjectTypes().size());

		// Change its label
		preparedDto.getDto().setLabel("NEW LABEL");
		assertEquals("NEW LABEL", preparedDto.getDto().getLabel());
		// Write back
		final TestObjectDto newDto = writeDao.update(preparedDto.getDto());
		assertEquals("NEW LABEL", newDto.getLabel());
		assertEquals(TO_DTO_1_REPLACED_ID, newDto.getId().getId());

		// Check that the old one still exists
		final PreparedDto<TestObjectDto> preparedOldDto = writeDao.getById(TO_DTO_1.getId());

		// Check for identical ID, same label and referencedBy property
		assertEquals(TO_DTO_1.getId(), preparedOldDto.getDto().getId());
		assertEquals(originalLabel, preparedOldDto.getDto().getLabel());
		assertNotEquals("NEW LABEL", preparedOldDto.getDto().getLabel());
		assertNotNull(preparedOldDto.getDto().getReplacedBy());
		assertEquals(newDto.getId(), preparedOldDto.getDto().getReplacedBy().getId());

		// And check that the new one exists
		assertEquals("NEW LABEL", ((TestObjectDto) preparedOldDto.getDto().getReplacedBy()).getLabel());

		// query and compare new one
		final PreparedDto<TestObjectDto> preparedNewDto = writeDao.getById(newDto.getId());
		assertEquals(newDto.toString(), preparedNewDto.getDto().toString());
	}

	@Test(expected = StorageException.class)
	public void test_5_1_fail_on_update_replaced_item() throws StorageException, ObjectWithIdNotFoundException {
		test_5_0_update();
		existsAndNotDisabled(TO_DTO_1);
		assertTrue(writeDao.exists(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID)));
		writeDao.update(TO_DTO_1);
	}

	@Test
	public void test_5_2_reset_after_update() throws StorageException, ObjectWithIdNotFoundException {
		test_5_0_update();
		DATA_STORAGE.reset();
		// Updated item still exists after reset
		existsAndNotDisabled(TO_DTO_1);
		assertTrue(writeDao.exists(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID)));
		writeDao.delete(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID));
	}

	@Test
	public void test_6_0_pagination_history_items_filter() throws StorageException, ObjectWithIdNotFoundException {
		cleanGeneratedItems();

		test_5_0_update();
		existsAndNotDisabled(TO_DTO_1);
		assertTrue(writeDao.exists(EidFactory.getDefault().createAndPreserveStr(TO_DTO_1_REPLACED_ID)));

		{
			final PreparedDtoCollection<TestObjectDto> result1 = writeDao.getAll(new Filter() {
				@Override
				public int offset() {
					return 0;
				}

				@Override
				public int limit() {
					return 3;
				}

			});
			assertNotNull(result1);
			assertEquals(1, result1.size());
			final TestObjectDto dto1 = result1.get(TO_DTO_1_REPLACED_ID);
			assertNotNull(dto1);
			assertEquals(TO_DTO_1_REPLACED_ID, dto1.getId().toString());
			assertNull(result1.get(TO_DTO_1));
		}

		{
			final PreparedDtoCollection<TestObjectDto> result2 = writeDao.getAll(new Filter() {
				@Override
				public int offset() {
					return 0;
				}

				@Override
				public int limit() {
					return 3;
				}

				@Override
				public LevelOfDetail levelOfDetail() {
					return LevelOfDetail.HISTORY;
				}
			});
			assertEquals(2, result2.size());
			assertNotNull(result2);
			assertTrue(result2.toString().contains("size=2"));

			final TestObjectDto dto1 = result2.get(TO_DTO_1_REPLACED_ID);
			assertNotNull(dto1);
			assertEquals(TO_DTO_1_REPLACED_ID, dto1.getId().toString());

			final TestObjectDto dto2 = result2.get(TO_DTO_1.getId());
			assertNotNull(dto2);
			assertEquals(TO_DTO_1.getId(), dto2.getId());
		}
	}
}

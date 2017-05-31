/**
 * Copyright 2010-2017 interactive instruments GmbH
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

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;

import org.basex.BaseX;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.*;
import org.basex.core.cmd.Set;
import org.basex.query.QueryException;
import org.basex.query.util.pkg.Pkg;
import org.basex.query.util.pkg.RepoManager;
import org.eclipse.persistence.internal.oxm.mappings.Descriptor;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.interactive_instruments.IFile;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.etf.EtfConstants;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.capabilities.ComponentDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.dal.dto.test.*;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.*;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.exceptions.config.MissingPropertyException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * Basex Data Storage
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class BsxDataStorage implements BsxDsCtx, DataStorage {

	public final static String ID_PREFIX = "EID";

	public final static String ETF_NAMESPACE_DECL = "declare namespace etf = "
			+ "\"http://www.interactive-instruments.de/etf/2.0\"; ";

	private final Logger logger = LoggerFactory.getLogger(BsxDataStorage.class);

	private static final String ETF_DB_NAME = "etf-ds";

	private final String etfxdbFileName = "etfxdb.xquery";

	private final String etfxqmPath = "de/interactive_instruments/etf/etfxdb.xqm";

	private JAXBContext jaxbContext;

	private final Map<String, Object> properties;

	private Map<Class<? extends Dto>, Dao<? extends Dto>> daoMapping;

	private Map<Class<? extends Dto>, Class<? extends Dto>> dtoLazyLoadProxies = new HashMap<>();
	private Map<Class<? extends Dto>, Class<? extends Dto>> dtoCacheAccessProxies = new HashMap<>();

	private IFile storeDir;

	private IFile backupDir;

	private IFile attachmentDir;

	private Context ctx = new Context();

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private ConfigPropertyHolder configProperties = new ConfigProperties(EtfConstants.ETF_DATASOURCE_DIR);

	private final DtoCache dtoCache = new DtoCache(512, this);

	private Schema schema;

	private final MarshalValidator marshalValidator = new MarshalValidator(logger);

	private static class UnmarshallerLogger extends Unmarshaller.Listener {

		private final Logger logger;

		private UnmarshallerLogger(final Logger logger) {
			this.logger = logger;
		}

		private String asString(final Object object) {
			if (object != null) {
				if (object instanceof Dto) {
					return ((Dto) object).getDescriptiveLabel();
				}
				return object.toString();
			}
			return null;
		}

		@Override
		public void beforeUnmarshal(final Object target, final Object parent) {
			logger.trace("Before: \n\ttarget: {} \n\tparent: {} ", asString(target), asString(parent));
		}
	}

	private static class MarshalValidator implements ValidationEventHandler {

		private final Logger logger;

		private MarshalValidator(final Logger logger) {
			this.logger = logger;
		}

		/**
		 * Checks unresolved idrefs
		 *
		 * @param event the encapsulated validation event information.  It is a
		 * provider error if this parameter is null.
		 * @return true if the JAXB Provider should attempt to continue the current
		 *         unmarshal, validate, or marshal operation after handling this
		 *         warning/error, false if the provider should terminate the current
		 *         operation with the appropriate <tt>UnmarshalException</tt>,
		 *         <tt>ValidationException</tt>, or <tt>MarshalException</tt>.
		 * @throws IllegalArgumentException if the event object is null.
		 */
		@Override
		public boolean handleEvent(final ValidationEvent event) {
			if (event.getMessage().startsWith("cvc-id")) {

				// TODO check if ID exists

				return true;
			}
			logger.error("Validation failed"
					+ "\n\tSeverity: {}"
					+ "\n\tMessage: {}"
					+ "\n\tLinked exception: {}"
					+ "\n\tLocator: {}:{} {}"
					+ "\n\tObject: {}"
					+ "\n\tNode: {}",
					event.getSeverity(), event.getMessage(), event.getLinkedException(),
					event.getLocator().getLineNumber(), event.getLocator().getColumnNumber(),
					event.getLocator().getOffset(), event.getLocator().getObject(),
					event.getLocator().getNode());

			return event.getSeverity() == ValidationEvent.WARNING;
		}
	}

	/**
	 * TODO package visibility if SPI is available
	 */
	public BsxDataStorage() {
		logger.debug("Preparing BsxDataStorage");

		properties = Collections.unmodifiableMap(new HashMap<String, Object>() {
			{
				put(JAXBContextProperties.DEFAULT_TARGET_NAMESPACE, EtfConstants.ETF_XMLNS);
			}
		});

		logger.info("BsxDataStorage {} prepared", this.getClass().getPackage().getImplementationVersion());
		logger.info("using BaseX {} ", BaseX.class.getPackage().getImplementationVersion());
		logger.info("using EclipseLink {} ", org.eclipse.persistence.Version.getVersion());
	}

	/**
	 * will be removed in version 2.1.0
	 */
	@Deprecated
	public void addFile(final IFile file) throws StoreException {
		try {
			new Add(file.getName(), file.getAbsolutePath()).execute(ctx);
		} catch (BaseXException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public synchronized void init() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
		if (!this.initialized.get()) {
			logger.debug("Initializing BsxDataStorage");
			this.configProperties.expectAllRequiredPropertiesSet();
			initDbSchema();
			try {
				initDaosAndMoxyDtoMapping();
				initLazyDtoProxies();
				initDtoCacheAccessProxies();
				initBsxDatabase();
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException
					| TransformerConfigurationException | JAXBException | IOException e) {
				throw new InitializationException(e);
			}
			logger.info("BsxDataStorage initialized");
			return;
		}
		throw new InvalidStateTransitionException("Data storage already initialized");
	}

	private String getXercesVersion() {
		String versionNumber = "unknown";
		try {
			final Class versionClass = Class.forName("org.apache.xerces.impl.Version");
			try {
				logger.trace("Xerces path: {}", versionClass.getProtectionDomain().getCodeSource().getLocation());
			} catch (Exception e) {
				ExcUtils.suppress(e);
			}
			final Method method = versionClass.getMethod("getVersion", (Class[]) null);
			final String version = (String) method.invoke(null, (Object[]) null);
			versionNumber = version.substring("Xerces-J ".length(), version.lastIndexOf("."));
		} catch (Exception e) {
			ExcUtils.suppress(e);
		}
		return versionNumber;
	}

	private void initDbSchema() throws InitializationException {
		try {
			final String validatorVersion = getXercesVersion();
			if (!("2.11.0-xml-schema-1.1-beta".equals(validatorVersion) ||
					"2.11.0-xml-schema-1".equals(validatorVersion))) {
				throw new RuntimeException("Validator version \"" + validatorVersion + "\" not supported");
			}

			final InputStream storageSchema = getClass().getClassLoader().getResourceAsStream("schema/model/resultSet.xsd");
			Objects.requireNonNull(storageSchema, "Internal error reading the data storage schema");
			final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			} catch (SAXException fE) {
				ExcUtils.suppress(fE);
			}
			try {
				sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			} catch (SAXException fE) {
				ExcUtils.suppress(fE);
			}
			try {
				sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
			} catch (SAXException fE) {
				ExcUtils.suppress(fE);
			}
			sf.setResourceResolver(new BsxSchemaResourceResolver());
			schema = sf.newSchema(new StreamSource(storageSchema));
			IFile.closeQuietly(storageSchema);
		} catch (SAXException e) {
			throw new InitializationException("Could not load schema: ", e);
		}
	}

	private void initDaosAndMoxyDtoMapping()
			throws StorageException, JAXBException, IOException, TransformerConfigurationException, ConfigurationException,
			InvalidStateTransitionException, InitializationException {
		final Map<Class<? extends Dto>, WriteDao<? extends Dto>> tmpDaoMapping = new HashMap<>();
		tmpDaoMapping.put(TestObjectDto.class, new TestObjectDao(this));
		tmpDaoMapping.put(TestObjectTypeDto.class, new TestObjectTypeDao(this));
		tmpDaoMapping.put(TestRunDto.class, new TestRunDao(this));
		tmpDaoMapping.put(TranslationTemplateBundleDto.class, new TranslationTemplateBundleDao(this));
		tmpDaoMapping.put(TagDto.class, new TagDao(this));
		tmpDaoMapping.put(ExecutableTestSuiteDto.class, new ExecutableTestSuiteDao(this));
		tmpDaoMapping.put(ComponentDto.class, new ComponentDao(this));
		tmpDaoMapping.put(TestTaskResultDto.class, new TestTaskResultDao(this));
		tmpDaoMapping.put(TestItemTypeDto.class, new TestItemTypeDao(this));
		tmpDaoMapping.put(TestTaskDto.class, new TestTaskDao(this));

		// Configure and init
		for (final WriteDao<? extends Dto> dao : tmpDaoMapping.values()) {
			dao.getConfigurationProperties().setPropertiesFrom(configProperties, true);
			dao.registerListener(dtoCache);
			dao.init();
		}

		daoMapping = Collections.unmodifiableMap(tmpDaoMapping);

		// Initialize moxy
		jaxbContext = JAXBContext.newInstance("de.interactive_instruments.etf.dal.bsx.moxy",
				Thread.currentThread().getContextClassLoader(), properties);

		// Eid objects are used as identifier for each Dto. However moxy does not determine the underlying
		// transformed type (String) correctly
		// (e.g, in XmlCollectionReferenceMapping.descriptor.getTypedField(tgtFld).getType() )
		// so the type is explicitly set here
		final List<Descriptor> descriptors = ((org.eclipse.persistence.jaxb.JAXBContext) jaxbContext).getXMLContext()
				.getDescriptors();
		for (final Descriptor descriptor : descriptors) {
			((XMLDescriptor) descriptor).getAllFields().stream()
					.filter(databaseField -> "@id".equals(databaseField.getQualifiedName()))
					.forEach(databaseField -> databaseField.setType(String.class));
		}
	}

	private void initBsxDatabase() throws MissingPropertyException, IOException, InitializationException {
		this.storeDir = this.configProperties.getPropertyAsFile(EtfConstants.ETF_DATASOURCE_DIR).expandPath("obj");
		// TODO get and set backup dir
		// this.backupDir = this.configProperties.getPropertyAsFile(EtfConstants.ETF_BACKUP_DIR);
		this.storeDir.ensureDir();
		this.attachmentDir = this.configProperties.getPropertyAsFile(EtfConstants.ETF_DATASOURCE_DIR).expandPath("attachments");

		new Set("AUTOFLUSH", "false").execute(ctx);
		new Set("TEXTINDEX", "true").execute(ctx);
		new Set("ATTRINDEX", "true").execute(ctx);
		new Set("FTINDEX", "true").execute(ctx);
		new Set("MAXLEN", "80").execute(ctx);
		new Set("UPDINDEX", "true").execute(ctx);

		final RepoManager repoManger = new RepoManager(ctx);
		IFile installFile = null;
		try {
			// Install basic XQuery script
			final InputStream basicXQueryStream = getClass().getClassLoader().getResourceAsStream("xquery/" + etfxdbFileName);
			Objects.requireNonNull(basicXQueryStream,
					"Internal error reading the basic data storage XQuery function library");
			// Copy file to repo
			installFile = new IFile(ctx.repo.path().file()).expandPath(etfxqmPath);
			installFile.getParentFile().mkdirs();
			installFile.write(basicXQueryStream);
			repoManger.install(installFile.getAbsolutePath());
		} catch (QueryException e) {
			throw new InitializationException("XQuery script installation failed: ", e);
		} catch (IOException e) {
			if (installFile != null && installFile.exists()) {
				logger.warn("Failed to update database file, will try to use the installed one");
			} else {
				throw new InitializationException("XQuery script installation failed: ", e);
			}
		}

		try {
			// Check for functx
			boolean functxFound = false;
			for (final Pkg pkg : repoManger.all()) {
				if ("http://www.functx.com".equals(pkg.name())) {
					functxFound = true;
				}
			}
			if (!functxFound) {
				// Install it
				final String functxInstallationUrl = "http://files.basex.org/modules/expath/functx-1.0.xar";
				logger.info("Installing FunctX XQuery Function Library from " + functxInstallationUrl);
				repoManger.install(functxInstallationUrl);
			}
		} catch (QueryException e) {
			throw new InitializationException("FunctX XQuery Function Library installation failed. "
					+ "If a proxy server is used, set the Java Virtual Machine parameter 'http.proxyHost'. "
					+ "Otherwise download functx-1.0.xar manually, extract the file (it is a ZIP file) and copy "
					+ "it to the BaseXRepo folder 'repo' below folder " + storeDir.getAbsolutePath(), e);
		}

		// Do not use auto optimize here: "However, updates can take much longer, so this option
		// should only be activated for medium-sized databases." (BaseX documentation)
		try {
			new Open(ETF_DB_NAME).execute(ctx);
		} catch (Exception e) {
			try {
				logger.warn("Opening of the data data storage failed, recreating it");
				reset();
			} catch (Exception e2) {
				throw new InitializationException("Recreation of etf-ds store failed: ", e2);
			}
		}
		logger.info(new InfoDB().execute(ctx));
		logger.info("Installed packages\n" + new RepoList().execute(ctx));
		this.initialized.set(true);
		notifyAll();
	}

	IFile getAttachmentDir() {
		return attachmentDir;
	}

	@Override
	public boolean isInitialized() {
		return this.initialized.get();
	}

	/**
	 * Clean unused items and optimize data storage
	 */
	@Override
	public void cleanAndOptimize() {
		this.initialized.set(false);
		long start = System.currentTimeMillis();
		try {
			// Sleep 3 seconds
			wait(3000);
		} catch (final InterruptedException e) {
			ExcUtils.suppress(e);
		}

		// TODO delete unused items
		logger.info("Optimizing " + ETF_DB_NAME);
		try {
			new OptimizeAll().execute(ctx);
		} catch (BaseXException e) {
			logger.error("Optimization failed: {} - Try to reset data storage manually!");
			return;
		}
		logger.info("Cleaned and optimized store in: " + TimeUtils.currentDurationAsMinsSeconds(start));
		this.initialized.set(true);
	}

	/**
	 * Returns the Data Access Object mappings for each Dto
	 *
	 * @return Data Access Object mappings for each Dto
	 */
	@Override
	public Map<Class<? extends Dto>, Dao<? extends Dto>> getDaoMappings() {
		return daoMapping;
	}

	/**
	 * Reset the data storage
	 *
	 * @throws StorageException if internal error occurs
	 */
	@Override
	public synchronized void reset() throws StorageException {
		this.initialized.set(false);
		long start = System.currentTimeMillis();
		logger.warn("Preparing data storage reset");
		try {
			// Sleep 0,5 seconds
			wait(500);
		} catch (final InterruptedException e) {
			ExcUtils.suppress(e);
		}
		try {
			try {
				new DropDB(ETF_DB_NAME).execute(ctx);
			} catch (Exception e) {
				ExcUtils.suppress(e);
			}
			new CreateDB(ETF_DB_NAME).execute(ctx);
			System.gc();
			long added = 0;
			long skipped = 0;
			// Add every single file and not the whole directory as
			// invalid files can be logged with detailed information
			for (File file : storeDir.listFiles(file1 -> !file1.isDirectory() && file1.getName().endsWith("xml"))) {
				try {
					new Add(file.getName(), file.getAbsolutePath()).execute(ctx);
					added++;
				} catch (Exception e) {
					skipped++;
					logger.error("Failed to add file {} (no. {}) to data storage: ", file.getAbsolutePath(), added, e);
				}
			}
			logger.info("{} data files added", added);
			if (skipped > 0) {
				logger.error("{} data files were skipped - check the reported files! "
						+ "This issue may occur due to improper shutdown. "
						+ "The data storage will try to continue the (re-)initialization without the skipped data.", skipped);
			}
			new Flush().execute(ctx);
			logger.info("Optimizing " + ETF_DB_NAME);
			new OptimizeAll().execute(ctx);
			new Open(ETF_DB_NAME).execute(ctx);
		} catch (BaseXException e) {
			logger.error("Failed to reset data storage which is in uninitialized state now.");
			try {
				ctx.close();
			} catch (Exception c) {
				ExcUtils.suppress(c);
			}
			throw new StorageException(e.getMessage());
		}
		logger.info("Recreated data storage in: " + TimeUtils.currentDurationAsMinsSeconds(start));
		this.initialized.set(true);
	}

	/**
	 * Create a data storage backup and returns the backup name
	 *
	 * @return the backup name
	 * @throws StorageException
	 */
	@Override
	public String createBackup() throws StorageException {
		final String bakName = "ETFDS-" + TimeUtils.dateToIsoString(new Date());
		try {
			new CreateBackup(bakName).execute(getBsxCtx());
		} catch (BaseXException e) {
			new StorageException(e);
		}
		return bakName;
	}

	/**
	 * List all available backup names
	 *
	 * @return
	 */
	@Override
	public List<String> getBackupList() {
		// TODO not implemented in this version
		throw new UnsupportedOperationException("not implemented in this version");
	}

	/**
	 * Restore a data storage backup by its backup name
	 *
	 * @param backupName name of the backup
	 * @throws StorageException
	 */
	@Override
	public void restoreBackup(final String backupName) throws StorageException {
		try {
			new Restore(backupName).execute(getBsxCtx());
		} catch (BaseXException e) {
			new StorageException(e);
		}
	}

	/**
	 * Returns the Basex context or waits until it has been initialized
	 *
	 * @return
	 */
	@Override
	public Context getBsxCtx() {
		if (this.initialized.get()) {
			return this.ctx;
		}
		synchronized (getClass()) {
			if (!this.initialized.get()) {
				logger.warn("Data storage is busy, waiting for initialization");
				long timeoutInMinutes = 12;
				try {
					getClass().wait(timeoutInMinutes * 60000);
				} catch (final InterruptedException e) {
					ExcUtils.suppress(e);
				}
				if (!this.initialized.get()) {
					final String errMsg = "Data storage not up after " + timeoutInMinutes + " minutes";
					logger.error(errMsg);
					throw new IllegalStateException(errMsg);
				}
			}
		}
		return this.ctx;
	}

	@Override
	public IFile getStoreDir() {
		return this.storeDir;
	}

	/**
	 * Creates a Unmarshaller which resolves and caches ID
	 * references
	 *
	 * @return
	 * @throws JAXBException
	 */
	@Override
	public Unmarshaller createUnmarshaller() throws JAXBException {
		final Unmarshaller um = jaxbContext.createUnmarshaller();
		um.setSchema(schema);
		if (logger.isDebugEnabled()) {
			um.setListener(new UnmarshallerLogger(logger));
		}
		final ValidationEventHandler idResolver = dtoCache.newIdResolverInstance();
		um.setEventHandler(idResolver);
		um.setProperty(UnmarshallerProperties.ID_RESOLVER, idResolver);
		return um;
	}

	/**
	 * Creates a Marshaller which ensures that persisted objects
	 * are schema valid
	 *
	 * @return
	 * @throws JAXBException
	 */
	@Override
	public Marshaller createMarshaller() throws JAXBException {
		final Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setSchema(schema);
		marshaller.setEventHandler(marshalValidator);
		return marshaller;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public Dto getFromCache(final EID eid) {
		return this.dtoCache.getFromCache(eid);
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return configProperties;
	}

	Schema getSchema() {
		return schema;
	}

	/**
	 * As the Dtos are not implementing interfaces, the InvocationHandler
	 * can not be used for proxying method calls. Therefore, new classes
	 * are generated at runtime and method calls are proxied to the {@link LazyLoadProxyDto}
	 * class.
	 *
	 * Note: the member variables of these proxy classes are never accessed, so avoid access
	 * via reflection!
	 */
	private final void initLazyDtoProxies()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		for (final Map.Entry<Class<? extends Dto>, Dao<? extends Dto>> classDaoEntry : daoMapping.entrySet()) {
			final Class<? extends Dto> classType = classDaoEntry.getKey();
			if (!dtoLazyLoadProxies.containsKey(classType)) {
				logger.debug("Creating proxy class for {}", classType.getSimpleName());
				final Class<? extends Dto> proxy = new ByteBuddy()
						.subclass(classType)
						.name(classType.getName() + "Proxy")
						.defineField("cached", Dto.class, Visibility.PRIVATE)
						.defineField("proxiedId", EID.class, Visibility.PRIVATE)
						.implement(ProxyAccessor.class).intercept(FieldAccessor.ofBeanProperty())
						.method(any().and(not(isDeclaredBy(ProxyAccessor.class))))
						.intercept(MethodDelegation.to(new LazyLoadProxyDto(classDaoEntry.getValue(), logger)))
						.make()
						.load(classType.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
						.getLoaded();
				dtoLazyLoadProxies.put(classType, proxy);
			}
		}
	}

	private final void initDtoCacheAccessProxies() {
		final Class[] treeModelDtos = {
				TestModuleDto.class, TestCaseDto.class, TestStepDto.class, TestAssertionDto.class
		};
		for (final Class classType : treeModelDtos) {
			if (!dtoCacheAccessProxies.containsKey(classType)) {
				logger.debug("Creating direct cache access proxy class for {}", classType.getSimpleName());
				final Class<? extends Dto> proxy = new ByteBuddy()
						.subclass(classType)
						.name(classType.getName() + "CacheAccess")
						.defineField("cached", Dto.class, Visibility.PRIVATE)
						.defineField("proxiedId", EID.class, Visibility.PRIVATE)
						.implement(ProxyAccessor.class).intercept(FieldAccessor.ofBeanProperty())
						.method(any().and(not(isDeclaredBy(ProxyAccessor.class))))
						.intercept(MethodDelegation.to(new CacheAccessProxyDto(this, logger)))
						.make()
						.load(classType.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
						.getLoaded();
				dtoCacheAccessProxies.put(classType, proxy);
			}
		}
	}

	/**
	 * Creates a new lazy load proxy class which loads the data from the
	 * data storage on demand.
	 *
	 * @param eid EID which exists in the data storags
	 * @param type Dto type
	 * @return LazyLoadProxyDto
	 * @throws ObjectWithIdNotFoundException invalid EID
	 */
	public Dto createProxy(final EID eid, final Class<? extends Dto> type)
			throws ObjectWithIdNotFoundException, StorageException {
		final Dao dao = daoMapping.get(type);
		if (dao == null) {
			final Class<? extends Dto> cacheAccessProxy = dtoCacheAccessProxies.get(type);
			if (cacheAccessProxy != null) {
				try {
					final Dto dto = cacheAccessProxy.newInstance();
					((ProxyAccessor) dto).setProxiedId(eid);
					return dto;
				} catch (InstantiationException | IllegalAccessException e) {
					throw new IllegalArgumentException("Could not initiate cache access proxy: " + e);
				}
			}
			throw new StorageException("No Data Access Object available for type " + type);

			// FIXME handle ETS dependencies
		} else if (!dao.exists(
				Objects.requireNonNull(eid, "ID is null")) && type != ExecutableTestSuiteDto.class) {
			throw new ObjectWithIdNotFoundException(eid.getId(), type.getSimpleName());
		}

		try {
			final Dto dto = dtoLazyLoadProxies.get(type).newInstance();
			((ProxyAccessor) dto).setProxiedId(eid);
			return dto;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Could not initiate Dto proxy: " + e);
		}
	}

	@Override
	public void release() {
		this.initialized.set(false);
		try {
			new Close().execute(ctx);
			ctx.close();
			this.dtoCache.clear();
		} catch (BaseXException e) {
			logger.error("Error during data storage shutdown ", e);
		} finally {
			logger.info("BsxDataStorage released");
		}
	}
}

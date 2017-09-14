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
package de.interactive_instruments.etf.dal.dao.basex.transformers;

import org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping;
import org.eclipse.persistence.mappings.transformers.AttributeTransformer;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.Session;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.model.EidFactory;

/**
 * Reads the string form XML and transform it to an EID
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class EidAttributeTransformer implements AttributeTransformer {

	private final EidFactory factory = EidFactory.getDefault();

	@Override
	public void initialize(final AbstractTransformationMapping mapping) {}

	@Override
	public Object buildAttributeValue(final Record record, final Object object, final Session session) {
		return factory.createAndPreserveStr(
				SUtils.requireNonNullOrEmpty(((String) record.get("@id")).substring(3),
						"ID attribute is null or empty"));
	}
}

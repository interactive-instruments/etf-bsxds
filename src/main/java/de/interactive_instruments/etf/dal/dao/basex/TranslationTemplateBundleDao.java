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

import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.StoreException;
import org.basex.core.BaseXException;

import java.util.List;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class TranslationTemplateBundleDao extends BsxWriteDao<TranslationTemplateBundleDto> {

	private static class TranslationTemplateBundleGetCmd implements MainDtoResultGetCmd<TranslationTemplateBundleDto> {
		@Override public List<TranslationTemplateBundleDto> getMainDtos(final DsResult dsResult) {
			return dsResult.getTranslationTemplateBundles();
		}
	}

	protected TranslationTemplateBundleDao(final BsxDsCtx ctx) throws StoreException {
		super("/etf:TranslationTemplateBundle", "TranslationTemplateBundle", ctx, new TranslationTemplateBundleGetCmd());
	}

	@Override protected void doCleanAfterDelete(final EID eid) throws BaseXException {

	}

	@Override public Class<TranslationTemplateBundleDto> getDtoType() {
		return TranslationTemplateBundleDto.class;
	}

	@Override public EidMap<OutputFormat> getOutputFormats() {
		return null;
	}
}

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

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.item.OutputFormat;
import org.basex.core.BaseXException;
import org.basex.core.cmd.XQuery;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.Objects;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class BsxPreparedDto<T extends Dto> implements PreparedDto<T> {

	final XQuery xquery;
	private final BsxDbCtx ctx;
	private final MainDtoResultGetCmd<T> getter;
	T cachedDto;

	BsxPreparedDto(final XQuery xquery, final BsxDbCtx ctx, final MainDtoResultGetCmd<T> getter) {
		this.xquery = xquery;
		this.ctx = ctx;
		this.getter = getter;
	}

	@Override public T getDto() {
		if(cachedDto==null) {
			try {
				final ByteArrayOutputStream output = new ByteArrayOutputStream();
				xquery.execute(ctx.getBsxCtx(), output);
				final DataStorageResult result = (DataStorageResult)
						ctx.createUnmarshaller().unmarshal(new ByteArrayInputStream(output.toByteArray()));
				cachedDto = Objects.requireNonNull(getter.getMainDto(result), "Database returned no data");
			}catch (IOException| JAXBException e) {
				try {
					ctx.getLogger().error("PreparedResult: {}  Exception: {}" ,xquery.execute(ctx.getBsxCtx()), e.getMessage());
				} catch (BaseXException bsxExc) {
					throw new IllegalStateException(bsxExc);
				}
				throw new IllegalStateException(e);
			}
		}
		return cachedDto;
	}

	@Override public void streamTo(final OutputFormat outputFormat, final OutputStream outputStream) {
		try {
			xquery.execute(ctx.getBsxCtx(), outputStream);
		} catch (BaseXException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override public int compareTo(final PreparedDto o) {
		if(o instanceof BsxPreparedDto) {
			return -1;
		}
		final BsxPreparedDto bsxO = (BsxPreparedDto) o;
		if(cachedDto!=null && bsxO.cachedDto!=null) {
			final int cmp = cachedDto.getId().compareTo(bsxO.cachedDto.getId());
			if(cmp!=0) {
				return cmp;
			}
		}
		return this.xquery.toString().compareTo(bsxO.xquery.toString());
	}

	@Override public String toString() {
		final StringBuffer sb = new StringBuffer("BsxPreparedDto{");
		sb.append("xquery=").append(xquery.toString());
		sb.append(", cachedDto=").append(cachedDto!=null ? cachedDto.getId() : "unresolved");
		sb.append('}');
		return sb.toString();
	}
}

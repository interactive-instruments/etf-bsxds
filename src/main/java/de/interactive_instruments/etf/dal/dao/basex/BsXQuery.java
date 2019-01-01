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

import static de.interactive_instruments.etf.dal.dao.basex.DsUtils.valueOfOrDefault;

import java.io.OutputStream;
import java.util.HashMap;

import org.basex.core.BaseXException;
import org.basex.core.cmd.XQuery;

import de.interactive_instruments.etf.dal.dao.Filter;

/**
 * Wrapped Xquery
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class BsXQuery {

	private XQuery xQuery;
	private final BsxDsCtx ctx;
	private final String queryStr;
	private final HashMap<String, String[]> parameter;

	BsXQuery(final BsxDsCtx ctx, final String queryStr) {
		this.ctx = ctx;
		this.queryStr = queryStr;
		this.parameter = new HashMap<>();
	}

	private BsXQuery(final BsxDsCtx ctx, final String queryStr, final HashMap parameter) {
		this.ctx = ctx;
		this.queryStr = queryStr;
		this.parameter = new HashMap<>(parameter);
	}

	BsXQuery parameter(final String name, final String value, final String type) {
		if (xQuery != null) {
			// reset query
			xQuery = null;
		}
		parameter.put(name, new String[]{value, type});
		return this;
	}

	BsXQuery parameter(final String name, final String value) {
		return parameter(name, value, null);
	}

	BsXQuery parameter(final Filter filter) {
		if (filter == null) {
			return this;
		}
		return parameter("offset", valueOfOrDefault(filter.offset(), "0"), "xs:integer")
				.parameter("limit", valueOfOrDefault(filter.limit(), "100"), "xs:integer")
				.parameter("levelOfDetail",
						valueOfOrDefault(filter.levelOfDetail(), String.valueOf(Filter.LevelOfDetail.SIMPLE)))
				.parameter("fields", valueOfOrDefault(filter.fields(), "*"));
	}

	String getParameter(final String name) {
		final String[] res = parameter.get(name);
		if (res != null) {
			return res[0];
		}
		return null;
	}

	BsxDsCtx getCtx() {
		return ctx;
	}

	private void ensureInitializedQuery() {
		if (xQuery == null) {
			xQuery = new XQuery(queryStr);
			parameter.entrySet().forEach(e -> xQuery.bind("$" + e.getKey(), e.getValue()[0], e.getValue()[1]));
		}
	}

	void execute(final OutputStream os) throws BaseXException {
		ensureInitializedQuery();
		xQuery.execute(ctx.getBsxCtx(), os);
	}

	String execute() throws BaseXException {
		ensureInitializedQuery();
		return xQuery.execute(ctx.getBsxCtx());
	}

	BsXQuery createCopy() {
		return new BsXQuery(ctx, queryStr, this.parameter);
	}

	@Override
	public String toString() {
		return xQuery != null ? xQuery.toString() : "Note compiled: " + queryStr;
	}
}

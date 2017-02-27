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

import java.util.LinkedList;
import java.util.List;

import de.interactive_instruments.etf.dal.dao.FilterBuilder;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class BsxFilterBuilder implements FilterBuilder {

	final List<String> expressions = new LinkedList<>();
	String orderBy;

	@Override
	public Prop property(final String s) {
		return null;
	}

	@Override
	public void where(final Expression expression) {

	}

	@Override
	public void orderBy(final Prop prop, final Order order) {

	}
}

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.bytebuddy.implementation.bind.annotation.*;

import org.slf4j.Logger;

import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;

/**
 * Lazy Load proxy for Dtos
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public final class LazyLoadProxyDto {

	private final Dao dao;
	private final Logger logger;
	private Dto cached;
	private EID eid;

	LazyLoadProxyDto(final Dao dao, final Logger logger) {
		this.dao = dao;
		this.logger = logger;
	}

	@RuntimeType
	public Object intercept(@Origin Method method, @AllArguments Object[] args) {
		logger.trace("Intercepted {} method call", method.getName());
		if (cached == null) {
			if (eid == null) {
				throw new IllegalStateException("Eid not set");
			}
			try {
				cached = dao.getById(eid).getDto();
			} catch (StoreException | ObjectWithIdNotFoundException e) {
				throw new IllegalStateException("Unable to load proxied Dto " + eid, e);
			}
		}
		try {
			return method.invoke(cached, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to proxy Dto " + eid + " method call", e);
		}
	}

	@RuntimeType
	public void setId(@Argument(0) EID eid) {
		this.eid = eid;
		if (cached != null) {
			cached.setId(eid);
		}
	}

	@RuntimeType
	public EID getId() {
		return this.eid;
	}

	@RuntimeType
	public String getDescriptiveLabel() {
		return "\'LAZY." + eid + "\'";
	}

	@RuntimeType
	public String toString() {
		final StringBuffer sb = new StringBuffer("LazyDtoProxy{");
		sb.append("id=").append(eid).append(", proxies=");
		if (cached != null) {
			sb.append(cached.toString());
		} else {
			sb.append("UNRESOLVED");
		}
		sb.append('}');
		return sb.toString();
	}
}

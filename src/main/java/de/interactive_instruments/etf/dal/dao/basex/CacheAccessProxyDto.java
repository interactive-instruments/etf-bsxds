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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import org.slf4j.Logger;

import de.interactive_instruments.etf.model.EID;

/**
 * Cache access proxy for Dtos
 *
 * Must be public and non-final!
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class CacheAccessProxyDto {

	private final BsxDsCtx ctx;
	private final Logger logger;

	CacheAccessProxyDto(final BsxDsCtx ctx, final Logger logger) {
		this.ctx = ctx;
		this.logger = logger;
	}

	@RuntimeType
	public Object intercept(@Origin Method method, @This ProxyAccessor proxy, @AllArguments Object[] args) {
		logger.trace("Intercepted {} method call", method.getName());
		if (proxy.getCached() == null) {
			if (proxy.getProxiedId() == null) {
				throw new IllegalStateException("Eid not set");
			}
			proxy.setCached(ctx.getFromCache(proxy.getProxiedId()));
			if (proxy.getCached() == null) {
				throw new IllegalStateException("Unable to load cached Dto " + proxy.getProxiedId() + " ! "
						+ "This may happen if the root object of this reference is not loaded!");
			}
		}
		try {
			return method.invoke(proxy.getCached(), args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to proxy Dto " + proxy.getProxiedId() + " method call", e);
		}
	}

	@RuntimeType
	public EID getId(@This ProxyAccessor proxy) {
		if (proxy.getCached() != null) {
			return proxy.getCached().getId();
		}
		return proxy.getProxiedId();
	}

	@RuntimeType
	public String getDescriptiveLabel(@This ProxyAccessor proxy) {
		return "\'CACHEACCESS." + proxy.getProxiedId() + "\'";
	}

	@RuntimeType
	public String toString(@This ProxyAccessor proxy) {
		final StringBuilder sb = new StringBuilder("CacheAccessProxy{");
		sb.append("id=").append(proxy.getProxiedId()).append(", proxies=");
		if (proxy.getCached() != null) {
			sb.append(proxy.getCached().toString());
		} else {
			sb.append("UNRESOLVED");
		}
		sb.append('}');
		return sb.toString();
	}
}

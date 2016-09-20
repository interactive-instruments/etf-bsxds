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

import de.interactive_instruments.etf.testdriver.TestRunLogger;
import org.slf4j.Marker;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class TestTestRunLogger implements TestRunLogger {


	@Override public File getLogFile() {
		return null;
	}

	@Override public List<String> getLogMessages(final long l) {
		return null;
	}

	@Override public void streamLogMessagesTo(final long l, final OutputStream outputStream) {

	}

	@Override public String getName() {
		return null;
	}

	@Override public boolean isTraceEnabled() {
		return false;
	}

	@Override public void trace(final String msg) {

	}

	@Override public void trace(final String format, final Object arg) {

	}

	@Override public void trace(final String format, final Object arg1, final Object arg2) {

	}

	@Override public void trace(final String format, final Object... arguments) {

	}

	@Override public void trace(final String msg, final Throwable t) {

	}

	@Override public boolean isTraceEnabled(final Marker marker) {
		return false;
	}

	@Override public void trace(final Marker marker, final String msg) {

	}

	@Override public void trace(final Marker marker, final String format, final Object arg) {

	}

	@Override public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {

	}

	@Override public void trace(final Marker marker, final String format, final Object... argArray) {

	}

	@Override public void trace(final Marker marker, final String msg, final Throwable t) {

	}

	@Override public boolean isDebugEnabled() {
		return false;
	}

	@Override public void debug(final String msg) {

	}

	@Override public void debug(final String format, final Object arg) {

	}

	@Override public void debug(final String format, final Object arg1, final Object arg2) {

	}

	@Override public void debug(final String format, final Object... arguments) {

	}

	@Override public void debug(final String msg, final Throwable t) {

	}

	@Override public boolean isDebugEnabled(final Marker marker) {
		return false;
	}

	@Override public void debug(final Marker marker, final String msg) {

	}

	@Override public void debug(final Marker marker, final String format, final Object arg) {

	}

	@Override public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {

	}

	@Override public void debug(final Marker marker, final String format, final Object... arguments) {

	}

	@Override public void debug(final Marker marker, final String msg, final Throwable t) {

	}

	@Override public boolean isInfoEnabled() {
		return false;
	}

	@Override public void info(final String msg) {

	}

	@Override public void info(final String format, final Object arg) {

	}

	@Override public void info(final String format, final Object arg1, final Object arg2) {

	}

	@Override public void info(final String format, final Object... arguments) {

	}

	@Override public void info(final String msg, final Throwable t) {

	}

	@Override public boolean isInfoEnabled(final Marker marker) {
		return false;
	}

	@Override public void info(final Marker marker, final String msg) {

	}

	@Override public void info(final Marker marker, final String format, final Object arg) {

	}

	@Override public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {

	}

	@Override public void info(final Marker marker, final String format, final Object... arguments) {

	}

	@Override public void info(final Marker marker, final String msg, final Throwable t) {

	}

	@Override public boolean isWarnEnabled() {
		return false;
	}

	@Override public void warn(final String msg) {

	}

	@Override public void warn(final String format, final Object arg) {

	}

	@Override public void warn(final String format, final Object... arguments) {

	}

	@Override public void warn(final String format, final Object arg1, final Object arg2) {

	}

	@Override public void warn(final String msg, final Throwable t) {

	}

	@Override public boolean isWarnEnabled(final Marker marker) {
		return false;
	}

	@Override public void warn(final Marker marker, final String msg) {

	}

	@Override public void warn(final Marker marker, final String format, final Object arg) {

	}

	@Override public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {

	}

	@Override public void warn(final Marker marker, final String format, final Object... arguments) {

	}

	@Override public void warn(final Marker marker, final String msg, final Throwable t) {

	}

	@Override public boolean isErrorEnabled() {
		return false;
	}

	@Override public void error(final String msg) {

	}

	@Override public void error(final String format, final Object arg) {

	}

	@Override public void error(final String format, final Object arg1, final Object arg2) {

	}

	@Override public void error(final String format, final Object... arguments) {

	}

	@Override public void error(final String msg, final Throwable t) {

	}

	@Override public boolean isErrorEnabled(final Marker marker) {
		return false;
	}

	@Override public void error(final Marker marker, final String msg) {

	}

	@Override public void error(final Marker marker, final String format, final Object arg) {

	}

	@Override public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {

	}

	@Override public void error(final Marker marker, final String format, final Object... arguments) {

	}

	@Override public void error(final Marker marker, final String msg, final Throwable t) {

	}
}

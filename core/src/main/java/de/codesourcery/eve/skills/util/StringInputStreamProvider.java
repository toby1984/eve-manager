/**
 * Copyright 2004-2009 Tobias Gierke <tobias.gierke@code-sourcery.de>
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
package de.codesourcery.eve.skills.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringInputStreamProvider implements IInputStreamProvider  {

	private final String data;
	
	public StringInputStreamProvider(String data) {
		this.data = data;
		if (data == null) {
			throw new IllegalArgumentException("data cannot be NULL");
		}
	}

	@Override
	public InputStream createInputStream() throws IOException {
		return new ByteArrayInputStream( data.getBytes() );
	}
	
	@Override
	public String toString() {
		return "StringInputStreamProvider[ data="+data+"]";
	}
}
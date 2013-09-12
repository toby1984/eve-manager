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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileInputStreamProvider implements IInputStreamProvider {

	private final File file;
	
	public FileInputStreamProvider(File f) {
		if (f == null) {
			throw new IllegalArgumentException("file cannot be NULL");
		}
		this.file = f;
	}

	@Override
	public InputStream createInputStream() throws FileNotFoundException {
		return new FileInputStream( file );
	}
	
	@Override
	public String toString() {
		return "file://"+file.getAbsolutePath();
	}
	
}

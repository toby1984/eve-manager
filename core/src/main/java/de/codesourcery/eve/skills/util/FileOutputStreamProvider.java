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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStreamProvider implements IOutputStreamProvider {

	private final File file;
	private final boolean overwrite;
	
	public FileOutputStreamProvider(File file) {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be NULL");
		}
		this.file = file;
		this.overwrite = false;
	}
	
	public FileOutputStreamProvider(File file,boolean overwrite) {
		if ( file == null ) {
			throw new IllegalArgumentException("file cannot be NULL");
		}
		this.overwrite = true;
		this.file = file;
	}
	
	@Override
	public OutputStream createOutputStream() throws IOException {
		return new FileOutputStream( file , ! overwrite );
	}

}

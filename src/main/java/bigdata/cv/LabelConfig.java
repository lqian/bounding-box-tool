/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bigdata.cv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LabelConfig {

	String[] clazzNames;
	Map<String, String> aliasMap = new HashMap<>();
	Map<String, String> idsMap = new HashMap<>();

	public LabelConfig(Path path) throws IOException {
		ArrayList<String> names = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				String tokens[] = line.split("=", 2);
				if (tokens.length == 2) {
					names.add(tokens[0]);
					aliasMap.put(tokens[0], tokens[1]);
				} else {
					System.out.println("invalid class name: " + line);
				}
			}
		} catch (IOException e) {
			System.err.println(e);
		}

		clazzNames = names.toArray(new String[names.size()]);
		for (int i=0; i < clazzNames.length; i++) {
			idsMap.put(clazzNames[i], "" + i);
		}
	}

	public String getAliases(String name) {
		return aliasMap.get(name);
	}
	
	public String getId(String name) {
		return idsMap.get(name);
	}
}

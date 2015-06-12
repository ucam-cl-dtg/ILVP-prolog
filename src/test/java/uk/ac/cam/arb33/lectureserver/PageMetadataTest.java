/*
 * Database.java abstracts the underlying database technology and data layout.
 *
 * Copyright 2012 Alastair R. Beresford
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero Public License for more details.
 */
package uk.ac.cam.arb33.lectureserver;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class PageMetadataTest {

	@Test
	public void testBreadcrumbs() {

		//Spec says getContextPath() and getServletPath() never return with a '/' in last position
		String[][][] testCase = {
				{{"/war/path/servlet/one/two"}, {"/war/path"}, {"/servlet"}, {"one", "two"}},
				{{"/war/path/servlet/one/two/"}, {"/war/path"}, {"/servlet"}, {"one", "two"}},
				{{"/war/path/servlet/one"}, {"/war/path"}, {"/servlet"}, {"one"}},
				{{"/war/path/servlet/one/"}, {"/war/path"}, {"/servlet"}, {"one"}},
				{{"/war/path/servlet"}, {"/war/path"}, {"/servlet"}, {}},
				{{"/war/path/servlet/"}, {"/war/path"}, {"/servlet"}, {}},
				{{"/war/path/one/two"}, {"/war/path"}, {""}, {"one", "two"}},
				{{"/war/path/one/two/"}, {"/war/path"}, {""}, {"one", "two"}},
				{{"/war/path/one"}, {"/war/path"}, {""}, {"one"}},
				{{"/war/path/one/"}, {"/war/path"}, {""}, {"one"}},
				{{"/war/path"}, {"/war/path"}, {""}, {}},
				{{"/war/path/"}, {"/war/path"}, {""}, {}},
				{{"/servlet"}, {""}, {"/servlet"}, {}},
				{{"/servlet/"}, {""}, {"/servlet"}, {}},
				{{""}, {""}, {""}, {}},
				{{"/"}, {""}, {""}, {}},
		};

		for(String[][] t: testCase) {			
			PageMetadata metadata = new PageMetadata(null, t[0][0], t[1][0], t[2][0]);
			assertArrayEquals(t[3], metadata.breadcrumb);
		}
	}
}

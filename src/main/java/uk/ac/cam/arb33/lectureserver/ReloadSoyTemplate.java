/*
 * Reload the SoyTemplate and build a new HtmlRenderer using DB as source of filename.
 *
 * Copyright 2013 Alastair R. Beresford
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

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Reload the SoyTemplate and build a new HtmlRenderer using DB as source of filename
 *
 */
public class ReloadSoyTemplate extends ServletBase {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String resource = reloadSoyTemplateRenderer();
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("utf-8");
			resp.getWriter().write("Reloaded template using resource = " + resource);
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
}

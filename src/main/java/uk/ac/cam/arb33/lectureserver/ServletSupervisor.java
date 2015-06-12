/*
 * HttpServlet used to server data to course supervisors and directors of studies.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletSupervisor extends ServletBase {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		PrintWriter out = resp.getWriter();		

		try {
			final Database.Person person = authenticateAndIssueErrorMessage(req, resp);
			if (person == null) {
				return; //error message already written by authentication method.
			}
			
			database.putEvent(person.id, Database.EventType.PAGE_SUCCESS.getId(), req.getRequestURI());

			PageMetadata metadata = new PageMetadata(person,
					req.getRequestURI(), req.getContextPath(),req.getServletPath());
			String[] path = metadata.breadcrumb;
			switch(path.length) {
			case 0:
				renderer.homePage(metadata, out);
				break;
			case 1:
				List<Database.Course> courses = database.getCourses(path[0]);
				renderer.allCourseInYear(metadata, out, courses);
				break;
			case 2:
				Database.StudentProgress progress = database.getStudentProgress(person.id, path[1], 
						path[0]);
				renderer.studentProgress(metadata, out, progress);
				break;
			default:
				throw new FileNotFoundException();
			}
		} catch (FileNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (Exception e) {
			//TODO(arb33): log these errors in db and issue redirect or something else sensible instead
			out.write("" + e);
			for(StackTraceElement element: e.getStackTrace()) {
				out.write("<br/>" + element);
			}
		}
	}
}
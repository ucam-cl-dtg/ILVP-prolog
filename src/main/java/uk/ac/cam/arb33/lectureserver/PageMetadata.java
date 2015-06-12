/*
 * Constructs a metadata object representing the closure template global variables.
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

public class PageMetadata {

	final public int idPerson;
	final public String personFullName;
	final public String personCrsid;
	final public String uri;
	final public String contextPath;
	final public String servletPath;
	final public String contextAndServletPath;
	final public String uriInsideServlet;
	final public String[] breadcrumb;
	final public String[] breadcrumbUrl;

	public PageMetadata(Database.Person person, String uri, String contextPath, String servletPath) {

		this.idPerson = (person != null) ? person.id : -1;
		this.personCrsid = (person != null) ? person.email : null;
		if (person == null) {
			this.personFullName = null;
		} else {
			String firstname = (person.firstname == null) ? "" : person.firstname;
			String familyname = (person.familyname == null) ? "": person.familyname;
			this.personFullName = firstname + " " + familyname;
		}
			
		this.uri = uri = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
		this.contextPath = contextPath;
		this.servletPath = servletPath;
		this.contextAndServletPath = contextPath + servletPath;
		final int uriLen = uri.length();
		final int len = contextAndServletPath.length();
		
		this.uriInsideServlet = (uriLen <= len) ? "" : uri.substring(len + 1, uriLen);
		this.breadcrumb = (uriLen <= len) ? new String[]{} : uriInsideServlet.split("/");
		this.breadcrumbUrl = new String[breadcrumb.length];
		String previousUrl = contextAndServletPath;
		for(int i = 0; i < breadcrumb.length; i++) {
			breadcrumbUrl[i] =  previousUrl = previousUrl + "/" + breadcrumb[i];
		}
	}
}